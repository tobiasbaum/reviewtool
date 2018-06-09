package de.setsoftware.reviewtool.model.changestructure;

import de.setsoftware.reviewtool.model.api.IChange;

/**
 * A singular change in a commit.
 */
public abstract class Change implements IChange {

    private final boolean irrelevantForReview;

    /**
     * Constructs a change.
     */
    public Change(final boolean irrelevantForReview) {
        this.irrelevantForReview = irrelevantForReview;
    }

    @Override
    public final boolean isIrrelevantForReview() {
        return this.irrelevantForReview;
    }

}
