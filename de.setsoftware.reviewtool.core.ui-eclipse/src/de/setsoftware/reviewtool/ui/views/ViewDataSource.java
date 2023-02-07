package de.setsoftware.reviewtool.ui.views;

import de.setsoftware.reviewtool.model.ReviewModeListener;
import de.setsoftware.reviewtool.model.changestructure.ToursInReview;
import de.setsoftware.reviewtool.ui.api.IStopViewer;

/**
 * Interface to allow decoupling the views from the plug-in.
 */
public abstract class ViewDataSource {

    private static ViewDataSource instance;

    public static void setInstance(ViewDataSource newInstance) {
        instance = newInstance;
    }

    public static ViewDataSource get() {
        return instance;
    }

    /**
     * Registers a new listener for state changes.
     * Additionally, new listener will be called once at registration for the current state.
     */
    public abstract void registerListener(ReviewModeListener l);

    /**
     * Returns the object that manages the tours for the current review.
     * Returns null iff there is no current review.
     */
    public abstract ToursInReview getToursInReview();

    /**
     * Returns the stop viewer to use for the current review.
     */
    public abstract IStopViewer getStopViewer();
}
