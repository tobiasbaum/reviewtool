package de.setsoftware.reviewtool.config;

/**
 * Interface to decouple the dynamically configurable review plugin from the
 * concrete implementations of the configurators.
 */
public interface IReviewConfigurable {

    /**
     * Sets the strategy used to load and store review data.
     */
    public abstract void configureWith(Object strategy);
    
}
