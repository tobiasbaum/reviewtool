package de.setsoftware.reviewtool.model;

import de.setsoftware.reviewtool.model.api.Mode;
import de.setsoftware.reviewtool.model.changestructure.ToursInReview;

/**
 * Observer interface for classes listening for changes in the review mode
 * or the reviewed ticket.
 */
public interface ReviewModeListener {
    
    /**
     * Ruft die passende notify-Operation für den übergebenen Modus auf.
     */
    public default void notifyForMode(Mode mode, ReviewStateManager mgr, ToursInReview toursInReview) {
        switch (mode) {
        case FIXING:
            this.notifyFixing(mgr);
            break;
        case REVIEWING:
            this.notifyReview(mgr, toursInReview);
            break;
        case IDLE:
            this.notifyIdle();
            break;
        default:
            throw new AssertionError();
        }
    }

    /**
     * Is called when a review is started.
     */
    public abstract void notifyReview(ReviewStateManager mgr, ToursInReview toursInReview);

    /**
     * Is called when fixing is started.
     */
    public abstract void notifyFixing(ReviewStateManager mgr);

    /**
     * Is called when fixing or reviewing is ended.
     */
    public abstract void notifyIdle();

}
