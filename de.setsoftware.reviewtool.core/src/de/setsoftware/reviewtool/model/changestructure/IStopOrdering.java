package de.setsoftware.reviewtool.model.changestructure;

import java.util.List;
import java.util.Set;

import de.setsoftware.reviewtool.model.api.IClassification;
import de.setsoftware.reviewtool.ordering.efficientalgorithm.TourCalculatorControl;

/**
 * Interface for decoupling core and the ordering algorithm.
 */
public interface IStopOrdering {

    public abstract List<? extends TourElement> groupAndSort(
            List<Stop> stops, TourCalculatorControl isCanceled, Set<? extends IClassification> irrelevantCategories) throws InterruptedException;

}
