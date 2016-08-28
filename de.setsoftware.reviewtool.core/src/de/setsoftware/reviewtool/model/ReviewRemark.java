package de.setsoftware.reviewtool.model;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

/**
 * A single review remark.
 */
public class ReviewRemark {

    public static final String RESOLUTION_MARKER_FIXED = "(/)";
    public static final String RESOLUTION_MARKER_WONTFIX = "(x)";
    public static final String RESOLUTION_MARKER_QUESTION = "(?)";

    private final ReviewStateManager persistence;
    private final IMarker marker;

    private ReviewRemark(ReviewStateManager p, IMarker marker) {
        this.persistence = p;
        this.marker = marker;
    }

    /**
     * Creates a review remark for the given line in the given file, but does not bind it to a marker (e.g.
     * because the file is not imported in the workspace).
     */
    public static ReviewRemark createWithoutMarker(
            ReviewStateManager p, IPath path, String user, String text, int line, RemarkType type, IWorkspace workspace)
                    throws CoreException {
        final IMarker marker = new DummyMarker(Constants.REVIEWMARKER_ID);
        final Position pos = PositionTransformer.toPosition(path, line, workspace);
        return create(p, marker, user, pos, text, type);
    }

    /**
     * Creates a review remark for the given line in the given resource and binds it to a marker.
     */
    public static ReviewRemark create(
            ReviewStateManager p, IResource resource, String user, String text, int line, RemarkType type)
                    throws CoreException {
        final IMarker marker = resource.createMarker(Constants.REVIEWMARKER_ID);
        final Position pos = PositionTransformer.toPosition(resource.getFullPath(), line, resource.getWorkspace());
        return create(p, marker, user, pos, text, type);
    }

    /**
     * Low level create operation.
     */
    public static ReviewRemark create(
            ReviewStateManager p,
            IMarker marker,
            String user,
            Position position,
            String text,
            RemarkType type) throws CoreException {
        marker.setAttribute(IMarker.MESSAGE, formatComment(user, text));
        marker.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_HIGH);
        marker.setAttribute(IMarker.SEVERITY, determineSeverity(ResolutionType.OPEN, type));
        marker.setAttribute(Constants.REMARK_TYPE, type.name());
        marker.setAttribute(Constants.REMARK_RESOLUTION,
                type == RemarkType.ALREADY_FIXED ? ResolutionType.FIXED.name() : ResolutionType.OPEN.name());
        marker.setAttribute(Constants.REMARK_POSITION, position.serialize());
        if (position instanceof FileLinePosition) {
            marker.setAttribute(IMarker.LINE_NUMBER, ((FileLinePosition) position).getLine());
        }
        return getFor(p, marker);
    }

    private String getPositionString() {
        return this.marker.getAttribute(Constants.REMARK_POSITION, new GlobalPosition().serialize());
    }

    public static ReviewRemark getFor(ReviewStateManager p, IMarker marker) {
        return new ReviewRemark(p, marker);
    }

    private ResolutionType getResolution() {
        return ResolutionType.valueOf(
                this.marker.getAttribute(Constants.REMARK_RESOLUTION, ResolutionType.OPEN.toString()));
    }

    RemarkType getRemarkType() {
        return RemarkType.valueOf(
                this.marker.getAttribute(Constants.REMARK_TYPE, RemarkType.MUST_FIX.toString()));
    }

    public boolean needsFixing() {
        return this.getResolution() == ResolutionType.OPEN
                && (this.getRemarkType() == RemarkType.CAN_FIX || this.getRemarkType() == RemarkType.MUST_FIX);
    }

    public void setResolution(ResolutionType value) throws CoreException {
        this.marker.setAttribute(IMarker.SEVERITY, determineSeverity(value, this.getRemarkType()));
        this.marker.setAttribute(Constants.REMARK_RESOLUTION, value.name());
    }

    private static int determineSeverity(ResolutionType resolution, RemarkType type) {
        if (resolution == ResolutionType.FIXED
                || resolution == ResolutionType.WONT_FIX) {
            return IMarker.SEVERITY_INFO;
        }
        if (type == RemarkType.ALREADY_FIXED
                || type == RemarkType.POSITIVE
                || type == RemarkType.TEMPORARY
                || type == RemarkType.OTHER) {
            return IMarker.SEVERITY_INFO;
        }
        return IMarker.SEVERITY_WARNING;
    }

    /**
     * Adds a comment/reply to this review remark.
     */
    public void addComment(String user, String reply) throws CoreException {
        final String oldText = this.marker.getAttribute(IMarker.MESSAGE, "");
        final String newText = oldText + "\n\n" + formatComment(user, reply);
        this.marker.setAttribute(IMarker.MESSAGE, newText);
    }

    private static String formatComment(String user, String comment) {
        return (user + ": " + comment).replaceAll("\n+", "\n").trim();
    }

    /**
     * Returns the comments on this review remark, including the initial remark.
     */
    public List<ReviewRemarkComment> getComments() {
        final String text = this.marker.getAttribute(IMarker.MESSAGE, "");
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
     * Merges this remark into the existing remarks and saves it to the persistence.
     */
    public void save() {
        final ReviewData r = this.persistence.getUi().getSyntaxFixer().getCurrentReviewDataParsed(
                this.persistence, DummyMarker.FACTORY);
        r.merge(this, this.persistence.getCurrentRound());
        this.persistence.saveCurrentReviewData(r.serialize());
    }

    /**
     * Returns a String representation of this review remark.
     */
    public String serialize() {
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

    private String resolutionMarker(int currentPos, List<ReviewRemarkComment> comments) {
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

    /**
     * Deletes this review remark. Persists the changed review data and deletes the marker as well.
     */
    public void delete() throws CoreException {
        final ReviewData data = this.persistence.getUi().getSyntaxFixer().getCurrentReviewDataParsed(
                this.persistence, DummyMarker.FACTORY);
        data.deleteRemark(this);
        this.persistence.saveCurrentReviewData(data.serialize());
        this.marker.delete();
    }

    public boolean hasSameTextAndPositionAs(ReviewRemark reviewRemark) {
        return this.getPositionString().equals(reviewRemark.getPositionString())
                && this.getComments().get(0).getText().equals(reviewRemark.getComments().get(0).getText());
    }

}
