package de.setsoftware.reviewtool.model.changestructure;

import de.setsoftware.reviewtool.model.api.IChange;
import de.setsoftware.reviewtool.model.api.IClassification;
import de.setsoftware.reviewtool.model.api.IWorkingCopy;

/**
 * A singular change in a commit.
 */
public abstract class Change implements IChange {

    private final IWorkingCopy wc;
    private final boolean irrelevantForReview;

    /**
     * Constructs a change.
     */
    public Change(final IWorkingCopy wc, final boolean irrelevantForReview) {
        this.wc = wc;
        this.irrelevantForReview = irrelevantForReview;
    }

    @Override
    public final boolean isIrrelevantForReview() {
        return this.irrelevantForReview;
    }

    @Override
    public final IClassification[] getClassification() {
        if (this.isIrrelevantForReview()) {
            return new IClassification[] {new Classification("irrelevant")};
        } else {
            return Classification.NONE;
        }
    }

    @Override
    public final IWorkingCopy getWorkingCopy() {
        return this.wc;
    }
}
