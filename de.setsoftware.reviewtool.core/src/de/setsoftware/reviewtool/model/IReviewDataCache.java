package de.setsoftware.reviewtool.model;

/**
 * Interface to access the local cache for review remarks.
 */
public interface IReviewDataCache {

    /**
     * Sets the data in the cache to the given value.
     */
    public abstract void saveLocalReviewData(String key, String data);

    /**
     * Returns the data currently in the cache, or null if there is none.
     */
    public abstract String getLocalReviewData(String key);

    /**
     * Deletes the cached data.
     */
    public abstract void clearLocalReviewData(String key);

}
