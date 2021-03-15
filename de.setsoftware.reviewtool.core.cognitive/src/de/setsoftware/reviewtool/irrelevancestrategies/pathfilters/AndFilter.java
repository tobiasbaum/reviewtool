package de.setsoftware.reviewtool.irrelevancestrategies.pathfilters;

import de.setsoftware.reviewtool.model.api.IChange;
import de.setsoftware.reviewtool.model.api.ICommit;
import de.setsoftware.reviewtool.model.changestructure.IIrrelevanceDetermination;

/**
 * Combines multiple filters by logical AND.
 */
public class AndFilter extends IIrrelevanceDetermination {

    private final IIrrelevanceDetermination[] children;

    public AndFilter(IIrrelevanceDetermination... children) {
        super(children[0].getNumber());
        this.children = children;
    }

    @Override
    public String getDescription() {
        return this.children[0].getDescription();
    }

    @Override
    public void clearCaches() {
        for (final IIrrelevanceDetermination child : this.children) {
            child.clearCaches();
        }
    }

    @Override
    public boolean isIrrelevant(ICommit commit, IChange change) {
        for (final IIrrelevanceDetermination child : this.children) {
            if (!child.isIrrelevant(commit, change)) {
                return false;
            }
        }
        return true;
    }

}
