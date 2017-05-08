package de.setsoftware.reviewtool.model.api;

import de.setsoftware.reviewtool.model.changestructure.FileInRevision;

/**
 * A singular change in a commit.
 */
public interface IChange {

    /**
     * Visitor-Pattern: Calls the method in the visitor that corresponds to this
     * change's type.
     */
    public abstract void accept(IChangeVisitor visitor);

    /**
     * Returns the {@link FileInRevision} the change is based upon.
     */
    public abstract IRevisionedFile getFrom();

    /**
     * Returns the {@link FileInRevision} the change was integrated into.
     */
    public abstract IRevisionedFile getTo();

    /**
     * Returns true if this change is regarded as irrelevant for code review.
     * When false is returned, this can be because the change is definitely relevant
     * or because nothing is known about the relevance.
     */
    public abstract boolean isIrrelevantForReview();

    /**
     * Returns true if this change is visible. Invisible changes would have been filtered out if they did not take part
     * in the relevant change history. For example, if a file A was changed in revisions 1-3, and the revisions 1 and
     * 3 belong to ticket A and revision 2 belongs to ticket B, and someone wants to analyze the changes of ticket A,
     * the changes in revision 2 need to be included because the changes in revision 3 depend upon them, but these
     * changes in revision 2 are marked as invisible as they are not part of ticket A.
     */
    public abstract boolean isVisible();

    /**
     * Returns a copy (if needed) of this change that is marked as "irrelevant for review".
     */
    public abstract IChange makeIrrelevant();

}
