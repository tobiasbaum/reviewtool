package de.setsoftware.reviewtool.model.changestructure;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Interface for strategies to determine the changes for a ticket, separated into commits.
 */
public interface IChangeSource {

    /**
     * Returns all remote changes (that are relevant for the review tool) for the ticket with the given key.
     */
    public abstract IChangeData getChanges(String key, IChangeSourceUi ui);

    /**
     * Returns all local changes (that are relevant for the review tool) in a new {@link IChangeData} object,
     * based on a {@link IChangeData} object returned earlier by {@link #getChanges(String, IChangeSourceUi)}.
     */
    public abstract IChangeData getLocalChanges(IChangeData remoteChanges, IProgressMonitor ui);

}
