package de.setsoftware.reviewtool.model.changestructure;

import java.util.List;

/**
 * Interface for strategies that automatically create a list of slices
 * from a changeset.
 */
public interface ISlicingStrategy {

    /**
     * Determine a separation of the given changes into review slices.
     */
    public abstract List<Slice> toSlices(List<Commit> changes);

}
