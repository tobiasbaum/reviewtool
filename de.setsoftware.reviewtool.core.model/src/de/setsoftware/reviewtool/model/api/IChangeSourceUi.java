package de.setsoftware.reviewtool.model.api;

/**
 * UI-Callbacks for change sources.
 */
public interface IChangeSourceUi {

    /**
     * Is called when the current local working copy does not contain all needed
     * changes (if the change source implements a corresponding check).
     * If true is returned the user chose to update the working copy.
     * If false is returned, the change source shall go on without performing an update.
     * If null is returned the user chose to cancel.
     */
    public abstract Boolean handleLocalWorkingIncomplete(String detailInfo);

    /**
     * Increases the task nesting level.
     */
    public abstract void increaseTaskNestingLevel();

    /**
     * Decreases the task nesting level.
     */
    public abstract void decreaseTaskNestingLevel();

    public abstract void subTask(String string);
    
    public abstract void throwIfCancelled();

    public abstract boolean isCanceled();
}
