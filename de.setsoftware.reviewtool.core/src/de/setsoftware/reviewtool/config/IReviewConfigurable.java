package de.setsoftware.reviewtool.config;

import de.setsoftware.reviewtool.model.IReviewPersistence;
import de.setsoftware.reviewtool.model.changestructure.IChangeSource;

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

}
