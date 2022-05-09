package de.setsoftware.reviewtool.model.changestructure;

/**
 * Reacts to events of a {@link ChangeManager}.
 */
public interface IChangeManagerListener {

    /**
     * Called whenever information about local changes is updated.
     * @param changeManager The event source.
     */
    public abstract void localChangeInfoUpdated(ChangeManager changeManager);

}
