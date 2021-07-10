package de.setsoftware.reviewtool.model.changestructure;

public interface IChangeManager {

    /**
     * Returns true iff change tracking is enabled.
     */
    public abstract boolean isTrackingEnabled();

    /**
     * Disables change tracking. Cannot be enabled again for this tracker.
     */
    public abstract void disableChangeTracking();

    /**
     * Registers a listeners that will be notified on changes.
     */
    public abstract void addListener(IChangeManagerListener listener);

}
