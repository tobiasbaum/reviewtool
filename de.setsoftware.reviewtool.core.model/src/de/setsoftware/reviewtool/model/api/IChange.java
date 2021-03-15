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
     * Returns the classifications that were attached to this change. Can be empty.
     */
    public abstract IClassification[] getClassification();

    /**
     * Returns a copy (if needed) of this change that additionally contains the given classification.
     */
    public abstract IChange addClassification(IClassification cl);

    /**
     * Returns the associated {@link IWorkingCopy}.
     */
    public abstract IWorkingCopy getWorkingCopy();

    /**
     * Returns the {@link FileChangeType} for the underlying file change.
     */
    public abstract FileChangeType getType();

}
