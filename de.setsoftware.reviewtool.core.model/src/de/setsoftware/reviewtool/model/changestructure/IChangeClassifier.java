package de.setsoftware.reviewtool.model.changestructure;

import de.setsoftware.reviewtool.model.api.IChange;
import de.setsoftware.reviewtool.model.api.IClassification;
import de.setsoftware.reviewtool.model.api.ICommit;

/**
 * Interface for implementations to determine classification labels for changes.
 */
public interface IChangeClassifier {

    /**
     * Determines and returns the classification for the given change.
     * If no classification label shall be added by this strategy, null is returned.
     */
    public abstract IClassification classify(ICommit commit, IChange change);

    /**
     * Informs the implementations that a possible internal caches shall be cleared
     * because a filtering run starts/ends.
     */
    public abstract void clearCaches();

}
