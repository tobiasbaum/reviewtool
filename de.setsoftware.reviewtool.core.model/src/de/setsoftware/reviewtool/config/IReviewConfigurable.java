package de.setsoftware.reviewtool.config;

import java.io.File;

/**
 * Interface to decouple the dynamically configurable review plugin from the
 * concrete implementations of the configurators.
 */
public interface IReviewConfigurable {

    /**
     * Sets the strategy used to load and store review data.
     */
    public abstract void configureWith(Object strategy);
    
    /**
     * Returns a directory in which state can be saved/cached.
     */
    public abstract File getStateDirectory();
    
}
