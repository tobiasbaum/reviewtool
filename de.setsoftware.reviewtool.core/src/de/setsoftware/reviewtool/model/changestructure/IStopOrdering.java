package de.setsoftware.reviewtool.model.changestructure;

import java.util.List;

import de.setsoftware.reviewtool.ordering.efficientalgorithm.CancelCallback;

/**
 * Interface for decoupling core and the ordering algorithm.
 */
public interface IStopOrdering {

    public abstract List<? extends TourElement> groupAndSort(
            List<Stop> stops, CancelCallback isCanceled) throws InterruptedException;

}
