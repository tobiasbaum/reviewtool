package de.setsoftware.reviewtool.model.changestructure;

/**
 * A singular change in a commit.
 */
public abstract class Change {

    private final boolean irrelevantForReview;

    public Change(boolean irrelevantForReview) {
        this.irrelevantForReview = irrelevantForReview;
    }

    /**
     * Visitor-Pattern: Calls the method in the visitor that corresponds to this
     * change's type.
     */
    public abstract void accept(ChangeVisitor visitor);

    /**
     * Returns the {@link FileInRevision} the change is based upon.
     */
    public abstract FileInRevision getFrom();

    /**
     * Returns the {@link FileInRevision} the change was integrated into.
     */
    public abstract FileInRevision getTo();

    /**
     * Returns true if this change is regarded as irrelevant for code review.
     * When false is returned, this can be because the change is definitely relevant
     * or because nothing is known about the relevance.
     */
    public final boolean isIrrelevantForReview() {
        return this.irrelevantForReview;
    }

    /**
     * Returns a copy (if needed) of this change that is marked as "irrelevant for review".
     */
    protected abstract Change makeIrrelevant();

}
