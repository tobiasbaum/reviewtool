package de.setsoftware.reviewtool.model;

/**
 * Interface for observers that want to be notified when a ticket's review data is saved.
 */
public interface IReviewDataSaveListener {

    /**
     * Is called when new review data is saved.
     */
    public abstract void onSave(String newData);

}
