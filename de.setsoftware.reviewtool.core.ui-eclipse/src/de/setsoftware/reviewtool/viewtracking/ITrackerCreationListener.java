package de.setsoftware.reviewtool.viewtracking;

/**
 * Interface for observers that want to be notified when the active code view tracker changes.
 */
public interface ITrackerCreationListener {

    /**
     * Notifies the implementing class that a new {@link CodeReviewTracker} is about to be
     * started.
     */
    public abstract void trackerStarts(CodeViewTracker tracker);

}
