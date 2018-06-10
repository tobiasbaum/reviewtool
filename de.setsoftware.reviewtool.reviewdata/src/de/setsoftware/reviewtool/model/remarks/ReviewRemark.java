package de.setsoftware.reviewtool.model.remarks;

import java.util.ArrayList;
import java.util.List;

/**
 * A single review remark.
 */
public class ReviewRemark {

    public static final String RESOLUTION_MARKER_FIXED = "(/)";
    public static final String RESOLUTION_MARKER_WONTFIX = "(x)";
    public static final String RESOLUTION_MARKER_QUESTION = "(?)";

    public static final String REMARK_TYPE = "de.setsoftware.reviewtool.markerattributes.remarktype";
    public static final String REMARK_RESOLUTION = "de.setsoftware.reviewtool.markerattributes.resolution";
    public static final String REMARK_POSITION = "de.setsoftware.reviewtool.markerattributes.position";

    private final IReviewMarker marker;

    private ReviewRemark(IReviewMarker marker) {
        this.marker = marker;
    }

    /**
     * Creates a review remark for the given line in the given resource and binds it to a marker.
     */
    public static ReviewRemark create(
            IReviewResource resource, String user, String text, int line, RemarkType type)
                    throws ReviewRemarkException {
        final IReviewMarker marker = resource.createReviewMarker();
        final Position pos = resource.createPosition(line);
        return create(marker, user, pos, text, type);
    }

    /**
     * Low level create operation.
     */
    public static ReviewRemark create(
            IReviewMarker marker,
            String user,
            Position position,
            String text,
            RemarkType type) throws ReviewRemarkException {
        marker.setMessage(formatComment(user, text));
        setSeverity(marker, ResolutionType.OPEN, type);
        marker.setAttribute(REMARK_TYPE, type.name());
        marker.setAttribute(REMARK_RESOLUTION,
                type == RemarkType.ALREADY_FIXED ? ResolutionType.FIXED.name() : ResolutionType.OPEN.name());
        marker.setAttribute(REMARK_POSITION, position.serialize());
        if (position instanceof FileLinePosition) {
            marker.setLineNumber(((FileLinePosition) position).getLine());
        }
        return getFor(marker);
    }

    private String getPositionString() throws ReviewRemarkException {
        return this.marker.getAttribute(REMARK_POSITION, new GlobalPosition().serialize());
    }

    public static ReviewRemark getFor(IReviewMarker marker) {
        return new ReviewRemark(marker);
    }

    private ResolutionType getResolution() throws ReviewRemarkException {
        return ResolutionType.valueOf(
                this.marker.getAttribute(REMARK_RESOLUTION, ResolutionType.OPEN.toString()));
    }

    RemarkType getRemarkType() throws ReviewRemarkException {
        return RemarkType.valueOf(
                this.marker.getAttribute(REMARK_TYPE, RemarkType.MUST_FIX.toString()));
    }

    public boolean needsFixing() throws ReviewRemarkException {
        return this.getResolution() == ResolutionType.OPEN
                && (this.getRemarkType() == RemarkType.CAN_FIX || this.getRemarkType() == RemarkType.MUST_FIX);
    }

    public void setResolution(ResolutionType value) throws ReviewRemarkException {
        setSeverity(this.marker, value, this.getRemarkType());
        this.marker.setAttribute(REMARK_RESOLUTION, value.name());
    }

    private static void setSeverity(IReviewMarker marker, ResolutionType resolution, RemarkType type) {
        if (resolution == ResolutionType.FIXED
                || resolution == ResolutionType.WONT_FIX) {
            marker.setSeverityInfo();
        } else if (type == RemarkType.ALREADY_FIXED
                || type == RemarkType.POSITIVE
                || type == RemarkType.TEMPORARY
                || type == RemarkType.OTHER) {
            marker.setSeverityInfo();
        } else {
            marker.setSeverityWarning();
        }
    }

    /**
     * Adds a comment/reply to this review remark.
     */
    public void addComment(String user, String reply) throws ReviewRemarkException {
        final String oldText = this.marker.getMessage();
        final String newText = oldText + "\n\n" + formatComment(user, reply);
        this.marker.setMessage(newText);
    }

    private static String formatComment(String user, String comment) {
        return (user + ": " + comment).replaceAll("\n+", "\n").trim();
    }

    /**
     * Returns the comments on this review remark, including the initial remark.
     */
    public List<ReviewRemarkComment> getComments() throws ReviewRemarkException {
        final String text = this.marker.getMessage();
        final String[] parts = text.split("\n\n");
        final List<ReviewRemarkComment> ret = new ArrayList<>();
        for (final String part : parts) {
            final String[] userAndText = part.split(": ", 2);
            if (userAndText.length != 2) {
                ret.add(new ReviewRemarkComment("", part));
            } else {
                ret.add(new ReviewRemarkComment(userAndText[0], userAndText[1]));
            }
        }
        return ret;
    }

    /**
     * Returns a String representation of this review remark.
     */
    public String serialize() throws ReviewRemarkException {
        final StringBuilder ret = new StringBuilder();
        final List<ReviewRemarkComment> comments = this.getComments();
        ret.append("*#").append(spacePrefixIfNonempty(this.getPositionString())).append(" ");
        ret.append(comments.get(0).getText()).append(
                spacePrefixIfNonempty(this.resolutionMarker(0, comments))).append("\n");

        int i = 1;
        for (final ReviewRemarkComment comment : comments.subList(1, comments.size())) {
            ret.append("*#*").append(spacePrefixIfNonempty(this.resolutionMarker(i, comments))).append(" ");
            ret.append(comment.getUser()).append(": ").append(comment.getText()).append("\n");
            i++;
        }
        return ret.toString();
    }

    private static String spacePrefixIfNonempty(String s) {
        return s.isEmpty() ? s : " " + s;
    }

    private String resolutionMarker(int currentPos, List<ReviewRemarkComment> comments) throws ReviewRemarkException {
        if (currentPos + 1 != comments.size()) {
            return "";
        }
        switch (this.getResolution()) {
        case FIXED:
            return RESOLUTION_MARKER_FIXED;
        case QUESTION:
            return RESOLUTION_MARKER_QUESTION;
        case WONT_FIX:
            return RESOLUTION_MARKER_WONTFIX;
        case OPEN:
        default:
            return "";
        }
    }

    public boolean hasSameTextAndPositionAs(ReviewRemark reviewRemark) throws ReviewRemarkException {
        return this.getPositionString().equals(reviewRemark.getPositionString())
                && this.getComments().get(0).getText().equals(reviewRemark.getComments().get(0).getText());
    }

    public void deleteMarker() {
        this.marker.delete();
    }

    private Position getPosition() {
        return Position.parse(this.getPositionString());
    }

    /**
     * Returns true iff the current remark's position is larger than the given remark's.
     * Global remarks are seen as the smallest ones, and whole file remarks are smaller than
     * remarks for a specific line in that file.
     */
    public boolean hasLargerPositionThan(ReviewRemark r2) {
        final Position p1 = this.getPosition();
        final Position p2 = r2.getPosition();
        final String f1 = p1.getShortFileName();
        if (f1 == null) {
            return false;
        }
        final String f2 = p2.getShortFileName();
        if (f2 == null) {
            return true;
        }
        if (f1.compareTo(f2) < 0) {
            return false;
        } else if (f1.compareTo(f2) > 0) {
            return true;
        }

        return p1.getLine() > p2.getLine();
    }

}
