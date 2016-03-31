package de.setsoftware.reviewtool.model;

/**
 * Abstraction for user interaction that is needed to correct review remark data that is syntactically invalid.
 */
public interface ISyntaxFixer {

    public abstract ReviewData getCurrentReviewDataParsed(ReviewStateManager persistence, IMarkerFactory factory);

}
