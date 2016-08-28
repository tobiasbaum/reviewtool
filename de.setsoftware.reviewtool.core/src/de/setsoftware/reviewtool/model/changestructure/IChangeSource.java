package de.setsoftware.reviewtool.model.changestructure;

/**
 * Interface for strategies to determine the changes for a ticket, separated into commits.
 */
public interface IChangeSource {

    /**
     * Returns all changes (that are relevant for the review tool) for the ticket with the given key.
     */
    public abstract IChangeData getChanges(String key, IChangeSourceUi ui);

}
