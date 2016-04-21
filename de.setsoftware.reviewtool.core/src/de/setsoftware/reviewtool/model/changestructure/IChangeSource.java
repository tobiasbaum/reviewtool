package de.setsoftware.reviewtool.model.changestructure;

import java.util.List;

/**
 * Interface for strategies to determine the changes for a ticket, separated into commits.
 */
public interface IChangeSource {

    /**
     * Returns all changes (that are relevant for the review tool) for the ticket with the given key.
     */
    public abstract List<Commit> getChanges(String key);

    /**
     * Creates a fragment tracer that is compatible with this change source.
     */
    public abstract IFragmentTracer createTracer();

}
