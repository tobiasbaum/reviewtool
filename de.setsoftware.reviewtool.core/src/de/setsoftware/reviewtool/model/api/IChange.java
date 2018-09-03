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
     * Returns the classifications that were attached to this change. Can be empty.
     */
    public abstract IClassification[] getClassification();

    /**
     * Returns a copy (if needed) of this change that is marked as "irrelevant for review".
     */
    public abstract IChange makeIrrelevant();

    /**
     * Returns the associated {@link IWorkingCopy}.
     */
    public abstract IWorkingCopy getWorkingCopy();

}
