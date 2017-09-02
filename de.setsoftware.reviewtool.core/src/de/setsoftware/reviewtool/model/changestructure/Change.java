package de.setsoftware.reviewtool.model.changestructure;

import de.setsoftware.reviewtool.model.api.IChange;

/**
 * A singular change in a commit.
 */
public abstract class Change implements IChange {

    private final boolean irrelevantForReview;
    private final boolean isVisible;

    /**
     * Constructs a change.
     * @param isVisible True if the change is visible, else false. See {@link #isVisible()} for a description of
     *                      visible/invisible changes.
     */
    public Change(boolean irrelevantForReview, final boolean isVisible) {
        this.irrelevantForReview = irrelevantForReview;
        this.isVisible = isVisible;
    }

    @Override
    public final boolean isIrrelevantForReview() {
        return this.irrelevantForReview;
    }

    @Override
    public final boolean isVisible() {
        return this.isVisible;
    }

}
