package de.setsoftware.reviewtool.model.changestructure;

import de.setsoftware.reviewtool.model.api.IChange;
import de.setsoftware.reviewtool.model.api.IClassification;
import de.setsoftware.reviewtool.model.api.ICommit;

/**
 * Interface for strategies that can determine irrelevant changes. Irrelevant changes can for example be changes
 * that have a low risk of introducing defects (when the main review goal is to detect defects).
 */
public abstract class IIrrelevanceDetermination implements IChangeClassifier {

    private final int number;

    public IIrrelevanceDetermination(int number) {
        this.number = number;
    }

    /**
     * Returns a description for this irrelevance determination strategy.
     * This description is shown to the user, together with the strategy's result, to allow
     * him to select which of the results to apply.
     */
    public abstract String getDescription();

    /**
     * Returns the number that will be used for the classifications of this class.
     */
    public int getNumber() {
        return this.number;
    }

    /**
     * Returns true if, according to this strategy, the given change should be considered irrelevant for review.
     * Does not have to take the existing irrelevance flag into account.
     */
    public abstract boolean isIrrelevant(ICommit commit, IChange change);

    @Override
    public final IClassification classify(ICommit commit, IChange change) {
        if (this.isIrrelevant(commit, change)) {
            return new Classification(this.number, this.getDescription(), true);
        } else {
            return null;
        }
    }

    @Override
    public void clearCaches() {
    }

}
