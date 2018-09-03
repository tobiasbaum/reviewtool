package de.setsoftware.reviewtool.model.changestructure;

import de.setsoftware.reviewtool.model.api.IChange;
import de.setsoftware.reviewtool.model.api.IClassification;

/**
 * Interface for strategies that can determine irrelevant changes. Irrelevant changes can for example be changes
 * that have a low risk of introducing defects (when the main review goal is to detect defects).
 */
public abstract class IIrrelevanceDetermination implements IChangeClassifier {

    /**
     * Returns a description for this irrelevance determination strategy.
     * This description is shown to the user, together with the strategy's result, to allow
     * him to select which of the results to apply.
     */
    public abstract String getDescription();

    /**
     * Returns true if, according to this strategy, the given change should be considered irrelevant for review.
     * Does not have to take the existing irrelevance flag into account.
     */
    public abstract boolean isIrrelevant(IChange change);

    @Override
    public final IClassification classify(IChange change) {
        if (this.isIrrelevant(change)) {
            return new Classification(this.getDescription(), true);
        } else {
            return null;
        }
    }

}
