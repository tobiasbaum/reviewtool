package de.setsoftware.reviewtool.ui.views;

import de.setsoftware.reviewtool.model.ReviewStateManager;

/**
 * Observer interface for classes listening for changes in the review mode
 * or the reviewed ticket.
 */
public interface ReviewModeListener {

    public abstract void notifyReview(ReviewStateManager mgr);

    public abstract void notifyFixing(ReviewStateManager mgr);

    public abstract void notifyIdle();

}
