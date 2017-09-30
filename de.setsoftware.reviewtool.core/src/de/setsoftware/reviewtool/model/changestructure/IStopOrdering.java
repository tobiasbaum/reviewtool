package de.setsoftware.reviewtool.model.changestructure;

import java.util.List;

/**
 * Interface for decoupling core and the ordering algorithm.
 */
public interface IStopOrdering {

    public abstract List<? extends TourElement> groupAndSort(List<Stop> stops);

}
