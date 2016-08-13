package de.setsoftware.reviewtool.model.changestructure;

/**
 * Interface for strategies that can determine irrelevant changes. Irrelevant changes can for example be changes
 * that have a low risk of introducing defects (when the main review goal is to detect defects).
 */
public interface IIrrelevanceDetermination {

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
    public abstract boolean isIrrelevant(Change change);

}
