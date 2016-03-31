package de.setsoftware.reviewtool.model.changestructure;

import java.util.List;

/**
 * Interface for strategies to determine the changes for a ticket, separated into slices.
 */
public interface ISliceSource {

    /**
     * Returns all changes (that are relevant for the review tool) for the ticket with the given key.
     */
    public abstract List<Slice> getSlices(String key);

}
