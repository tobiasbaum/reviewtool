package de.setsoftware.reviewtool.model.changestructure;

import java.util.List;

/**
 * Interface for strategies that automatically slice a changeset into a list of tours.
 */
public interface ISlicingStrategy {

    /**
     * Determine a separation of the given changes into review tours.
     */
    public abstract List<Tour> toTours(List<Commit> changes);

}
