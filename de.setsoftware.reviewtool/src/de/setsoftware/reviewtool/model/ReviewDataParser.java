package de.setsoftware.reviewtool.model;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.CoreException;

class ReviewDataParser {

	private static final String TYPE_PREFIX = "* ";
	private static final String REMARK_PREFIX = "*# ";
	private static final String COMMENT_PREFIX = "*#* ";

	private static final Pattern REVIEW_HEADER_PATTERN = Pattern.compile("Review (\\d+):");
	private static final Pattern COMMENT_PATTERN = Pattern.compile("([^ ]+): (.+)", Pattern.DOTALL);

	private enum ParseState {BEFORE_ROUND, IN_ROUND, IN_REMARK, IN_COMMENT};

	private final ReviewStateManager persistence;
	private final IMarkerFactory markerFactory;

	private ParseState state = ParseState.BEFORE_ROUND;
	private ReviewRound currentRound;
	private final List<ReviewRound> rounds = new ArrayList<>();
	private RemarkType currentType;
	private final StringBuilder currentText = new StringBuilder();
	private ReviewRemark currentRemark;

	public ReviewDataParser(ReviewStateManager persistence, IMarkerFactory markerFactory) {
		this.persistence = persistence;
		this.markerFactory = markerFactory;
	}

	public void handleNextLine(String trimmedLine) throws CoreException {
		if (this.state == ParseState.BEFORE_ROUND) {
			if (trimmedLine.isEmpty()) {
				//ist OK, nichts tun
			}
			final Matcher m = REVIEW_HEADER_PATTERN.matcher(trimmedLine);
			if (m.matches()) {
				this.currentRound = new ReviewRound(Integer.parseInt(m.group(1)));
				this.rounds.add(this.currentRound);
				this.state = ParseState.IN_ROUND;
			} else {
				throw new RuntimeException("parse exception: " + trimmedLine);
			}
		} else {
			if (trimmedLine.isEmpty()) {
				this.endLastItem();
				this.state = ParseState.BEFORE_ROUND;
				this.currentRound = null;
			} else if (trimmedLine.startsWith(TYPE_PREFIX)) {
				this.endLastItem();
				this.currentType = this.parseType(trimmedLine);
				this.state = ParseState.IN_ROUND;
			} else if (trimmedLine.startsWith(REMARK_PREFIX)) {
				this.endLastItem();
				this.parseRemarkStart(trimmedLine);
				this.state = ParseState.IN_REMARK;
			} else if (trimmedLine.startsWith(COMMENT_PREFIX)) {
				this.endLastItem();
				this.parseCommentStart(trimmedLine);
				this.state = ParseState.IN_COMMENT;
			} else {
				if (this.state == ParseState.IN_REMARK
						|| this.state == ParseState.IN_COMMENT) {
					this.currentText .append("\n").append(trimmedLine);
				} else {
					throw new RuntimeException("parse exception: " + trimmedLine);
				}
			}
		}
	}

	private RemarkType parseType(String trimmedLine) {
		return ReviewRound.parseType(trimmedLine.substring(2));
	}

	private void parseRemarkStart(String trimmedLine) {
		this.currentText.append(trimmedLine.substring(REMARK_PREFIX.length()));
	}

	private void parseCommentStart(String trimmedLine) {
		this.currentText.append(trimmedLine.substring(COMMENT_PREFIX.length()));
	}

	void endLastItem() throws CoreException {
		switch (this.state) {
		case IN_REMARK:
			final ResolutionType resoRemark = this.handleResolutionMarkers();
			final Position pos = this.parsePosition();
			this.currentRemark = ReviewRemark.create(
					this.persistence,
					this.markerFactory.createMarker(pos),
					this.persistence.getReviewerForRound(this.currentRound.getNumber()),
					pos,
					this.currentText.toString(),
					this.currentType);
			if (resoRemark != null) {
				this.currentRemark.setResolution(resoRemark);
			}
			this.currentRound.add(this.currentRemark);
			break;
		case IN_COMMENT:
			final ResolutionType resoComment = this.handleResolutionMarkers();
			final Matcher m = COMMENT_PATTERN.matcher(this.currentText);
			if (m.matches()) {
				this.currentRemark.addComment(m.group(1), m.group(2));
				if (resoComment != null) {
					this.currentRemark.setResolution(resoComment);
				}
			} else {
				throw new RuntimeException("parse exception: " + this.currentText);
			}
			break;
		}
		this.currentText.setLength(0);
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

	public ReviewData getResult() {
		if (this.rounds.isEmpty()) {
			return new ReviewData();
		}

		final TreeMap<Integer, ReviewRound> roundMap = new TreeMap<>();
		for (final ReviewRound round : this.rounds) {
			if (roundMap.containsKey(round.getNumber())) {
				throw new RuntimeException("duplicate round: " + round.getNumber());
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

}
