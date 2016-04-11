package de.setsoftware.reviewtool.ui.views;

import de.setsoftware.reviewtool.model.ReviewStateManager;
import de.setsoftware.reviewtool.model.changestructure.SlicesInReview;

/**
 * Observer interface for classes listening for changes in the review mode
 * or the reviewed ticket.
 */
public interface ReviewModeListener {

    /**
     * Is called when a review is started.
     */
    public abstract void notifyReview(ReviewStateManager mgr, SlicesInReview slicesInReview);

    /**
     * Is called when fixing is started.
     */
    public abstract void notifyFixing(ReviewStateManager mgr);

    /**
     * Is called when fixing or reviewing is ended.
     */
    public abstract void notifyIdle();

}
