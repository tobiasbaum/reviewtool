package de.setsoftware.reviewtool.model.changestructure;

/**
 * A singular change in a commit.
 */
public abstract class Change {

    private final boolean irrelevantForReview;
    private final boolean isVisible;

    /**
     * Constructs a change.
     * @param isVisible True if the change is visible, else false. See {@link isVisible()} for a description of
     * visible/invisible changes.
     */
    public Change(boolean irrelevantForReview, final boolean isVisible) {
        this.irrelevantForReview = irrelevantForReview;
		this.isVisible = isVisible;
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
     * Returns true if this change is visible. Invisible changes would have been filtered out if they did not take part
     * in the relevant change history. For example, if a file A was changed in revisions 1-3, and the revisions 1 and
     * 3 belong to ticket A and revision 2 belongs to ticket B, and someone wants to analyse the changes of ticket A,
     * the changes in revision 2 need to be included because the changes in revision 3 depend upon them, but these
     * changes in revision 2 are marked as invisible as they are not part of ticket A.
     */
    public boolean isVisible() {
        return this.isVisible;
    }

    /**
     * Returns a copy (if needed) of this change that is marked as "irrelevant for review".
     */
    protected abstract Change makeIrrelevant();

}
