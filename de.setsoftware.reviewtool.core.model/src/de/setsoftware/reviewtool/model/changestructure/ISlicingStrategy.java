package de.setsoftware.reviewtool.model.changestructure;

import java.util.List;

import de.setsoftware.reviewtool.model.api.ICommit;

/**
 * Interface for strategies that automatically slice a changeset into a list of tours.
 */
public interface ISlicingStrategy {

    /**
     * Determine a separation of the given commits into review tours.
     */
    public abstract List<Tour> toTours(List<? extends ICommit> commits);

}
