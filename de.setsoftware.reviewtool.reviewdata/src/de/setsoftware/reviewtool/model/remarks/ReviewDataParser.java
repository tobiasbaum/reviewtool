package de.setsoftware.reviewtool.model.remarks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for serialized review data.
 * The review data is stored in a human readable format based an Markdown syntax.
 */
class ReviewDataParser {

    private static final String TYPE_PREFIX = "* ";
    private static final String REMARK_PREFIX = "*# ";
    private static final String COMMENT_PREFIX = "*#* ";

    private static final Pattern REVIEW_HEADER_PATTERN =
            Pattern.compile("[^a-zA-Z0-9]*review[^a-zA-Z0-9]+(\\d+)[^a-zA-Z0-9]*", Pattern.CASE_INSENSITIVE);
    private static final Pattern COMMENT_PATTERN = Pattern.compile("([^ ]+): (.+)", Pattern.DOTALL);

    /**
     * States for the parser's state machine.
     */
    private enum ParseState {
        BEFORE_REMARK,
        IN_REMARK,
        IN_COMMENT
    }

    private final IMarkerFactory markerFactory;

    private ParseState state = ParseState.BEFORE_REMARK;
    private final Map<Integer, String> reviewersForRounds;
    private ReviewRound currentRound;
    private final List<ReviewRound> rounds = new ArrayList<>();
    private RemarkType currentType = RemarkType.CAN_FIX;
    private final StringBuilder currentText = new StringBuilder();
    private ReviewRemark currentRemark;

    public ReviewDataParser(Map<Integer, String> reviewersForRounds, IMarkerFactory markerFactory) {
        this.reviewersForRounds = reviewersForRounds;
        this.markerFactory = markerFactory;
    }

    public void handleNextLine(String trimmedLine) throws ReviewRemarkException {
        final Matcher roundStartMatcher = REVIEW_HEADER_PATTERN.matcher(trimmedLine);
        if (roundStartMatcher.matches()) {
            this.currentRound = this.getOrCreateRound(Integer.parseInt(roundStartMatcher.group(1)));
            this.state = ParseState.BEFORE_REMARK;
        } else if (trimmedLine.isEmpty()) {
            this.endLastItem();
            this.state = ParseState.BEFORE_REMARK;
            this.currentRemark = null;
            this.currentType = RemarkType.CAN_FIX;
        } else if (trimmedLine.startsWith(TYPE_PREFIX)) {
            this.endLastItem();
            final RemarkType type = this.parseType(trimmedLine);
            if (type != null) {
                this.currentType = type;
                this.state = ParseState.BEFORE_REMARK;
            } else {
                //invalid type name, treat it as a remark
                this.parseRemarkOrCommentStart(trimmedLine);
                this.state = ParseState.IN_REMARK;
            }
        } else if (trimmedLine.startsWith(REMARK_PREFIX)) {
            this.endLastItem();
            this.parseRemarkOrCommentStart(trimmedLine);
            this.state = ParseState.IN_REMARK;
        } else if (trimmedLine.startsWith(COMMENT_PREFIX)) {
            this.endLastItem();
            this.parseRemarkOrCommentStart(trimmedLine);
            this.state = ParseState.IN_COMMENT;
        } else if (trimmedLine.startsWith("-") || trimmedLine.startsWith("#") || trimmedLine.startsWith("*")) {
            this.endLastItem();
            this.parseRemarkOrCommentStart(trimmedLine);
            this.state = ParseState.IN_REMARK;
        } else {
            if (this.state == ParseState.IN_REMARK || this.state == ParseState.IN_COMMENT) {
                this.currentText.append("\n");
            } else {
                this.state = ParseState.IN_REMARK;
            }
            this.currentText.append(trimmedLine);
        }
    }

    private ReviewRound getOrCreateRound(int number) {
        for (final ReviewRound round : this.rounds) {
            if (round.getNumber() == number) {
                return round;
            }
        }
        final ReviewRound newRound = new ReviewRound(number);
        this.rounds.add(newRound);
        return newRound;
    }

    private RemarkType parseType(String trimmedLine) {
        return ReviewRound.parseType(trimmedLine.substring(2));
    }

    private void parseRemarkOrCommentStart(String trimmedLine) {
        String remaining = trimmedLine;
        while (remaining.startsWith("*")
                || remaining.startsWith("#")
                || remaining.startsWith("-")
                || remaining.startsWith(" ")) {
            remaining = remaining.substring(1);
        }
        this.currentText.append(remaining);
    }

    void endLastItem() throws ReviewRemarkException {
        switch (this.state) {
        case IN_REMARK:
            final ResolutionType resoRemark = this.handleResolutionMarkers();
            final Position pos = this.parsePosition();
            this.currentRemark = ReviewRemark.create(
                    this.markerFactory.createMarker(pos),
                    this.getReviewerForCurrentRound(),
                    pos,
                    this.currentText.toString(),
                    this.currentType);
            if (resoRemark != null) {
                this.currentRemark.setResolution(resoRemark);
            }
            this.getCurrentRound().add(this.currentRemark);
            break;
        case IN_COMMENT:
            final ResolutionType resoComment = this.handleResolutionMarkers();
            final Matcher m = COMMENT_PATTERN.matcher(this.currentText);
            if (m.matches()) {
                if (this.currentRemark == null) {
                    throw new ReviewRemarkException("dangling comment: " + this.currentText);
                }
                this.currentRemark.addComment(m.group(1), m.group(2));
                if (resoComment != null) {
                    this.currentRemark.setResolution(resoComment);
                }
            } else {
                throw new ReviewRemarkException("parse exception: " + this.currentText);
            }
            break;
        case BEFORE_REMARK:
            break;
        default:
            throw new AssertionError("unknown state " + this.state);
        }
        this.currentText.setLength(0);
    }

    private ReviewRound getCurrentRound() {
        return this.currentRound != null ? this.currentRound : this.getOrCreateRound(1);
    }

    private Position parsePosition() {
        final String text = this.currentText.toString();
        if (text.startsWith("(")) {
            final String position = text.substring(0, text.indexOf(')') + 1);
            this.currentText.setLength(0);
            this.currentText.append(text.substring(position.length()).trim());
            return Position.parse(position);
        } else {
            return new GlobalPosition();
        }
    }

    private ResolutionType handleResolutionMarkers() {
        final String text = this.currentText.toString();
        if (text.contains(ReviewRemark.RESOLUTION_MARKER_FIXED)) {
            this.currentText.setLength(0);
            this.currentText.append(text.replace(ReviewRemark.RESOLUTION_MARKER_FIXED, "").trim());
            return ResolutionType.FIXED;
        }
        if (text.contains(ReviewRemark.RESOLUTION_MARKER_WONTFIX)) {
            this.currentText.setLength(0);
            this.currentText.append(text.replace(ReviewRemark.RESOLUTION_MARKER_WONTFIX, "").trim());
            return ResolutionType.WONT_FIX;
        }
        if (text.contains(ReviewRemark.RESOLUTION_MARKER_QUESTION)) {
            this.currentText.setLength(0);
            this.currentText.append(text.replace(ReviewRemark.RESOLUTION_MARKER_QUESTION, "").trim());
            return ResolutionType.QUESTION;
        }
        return null;
    }

    public ReviewData getResult() throws ReviewRemarkException {
        if (this.rounds.isEmpty()) {
            return new ReviewData();
        }

        final TreeMap<Integer, ReviewRound> roundMap = new TreeMap<>();
        for (final ReviewRound round : this.rounds) {
            if (roundMap.containsKey(round.getNumber())) {
                throw new ReviewRemarkException("duplicate round: " + round.getNumber());
            }
            roundMap.put(round.getNumber(), round);
        }
        final List<ReviewRound> sortedRounds = new ArrayList<>();
        final int maxNumber = roundMap.lastKey();
        for (int i = 1; i <= maxNumber; i++) {
            if (roundMap.containsKey(i)) {
                sortedRounds.add(roundMap.get(i));
            } else {
                sortedRounds.add(new ReviewRound(i));
            }
        }
        return new ReviewData(sortedRounds);
    }

    private String getReviewerForCurrentRound() {
        final String r = this.reviewersForRounds.get(this.getCurrentRound().getNumber());
        return r != null ? r : "??";
    }

}
