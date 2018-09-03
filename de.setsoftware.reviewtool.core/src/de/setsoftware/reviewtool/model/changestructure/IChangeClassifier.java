package de.setsoftware.reviewtool.model.changestructure;

import de.setsoftware.reviewtool.model.api.IChange;
import de.setsoftware.reviewtool.model.api.IClassification;

/**
 * Interface for strategies that can determine classification labels for changes.
 */
public interface IChangeClassifier {

    /**
     * Determines and returns the classification for the given change.
     * If no classification label shall be added by this strategy, null is returned.
     */
    public abstract IClassification classify(IChange change);

}
