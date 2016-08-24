package de.setsoftware.reviewtool.config;

import de.setsoftware.reviewtool.model.IReviewPersistence;
import de.setsoftware.reviewtool.model.changestructure.IChangeSource;
import de.setsoftware.reviewtool.ui.dialogs.EndReviewExtension;
import de.setsoftware.reviewtool.ui.views.IStopViewer;

/**
 * Interface to decouple the dynamically configurable review plugin from the
 * concrete implementations of the configurators.
 */
public interface IReviewConfigurable {

    /**
     * Sets the strategy used to load and store review data.
     */
    public abstract void setPersistence(IReviewPersistence persistence);

    /**
     * Adds a source for changes.
     */
    public abstract void setChangeSource(IChangeSource changeSource);

    /**
     * Adds an extension for the end review dialog.
     */
    public abstract void addEndReviewExtension(EndReviewExtension extension);

    /**
     * Sets a stop viewer.
     */
    public abstract void setStopViewer(IStopViewer stopViewer);
}
