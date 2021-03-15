package de.setsoftware.reviewtool.model;

import de.setsoftware.reviewtool.model.remarks.IMarkerFactory;
import de.setsoftware.reviewtool.model.remarks.ReviewData;

/**
 * Abstraction for user interaction that is needed to correct review remark data that is syntactically invalid.
 */
public interface ISyntaxFixer {

    public abstract ReviewData getCurrentReviewDataParsed(ReviewStateManager persistence, IMarkerFactory factory);

}
