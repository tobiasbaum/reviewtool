package de.setsoftware.reviewtool.model.api;

/**
 * Represents the most recent version in the local working copy.
 */
public interface ILocalRevision extends IRevision {

    /**
     * Returns the working copy this revision is associated with.
     */
    public abstract IWorkingCopy getWorkingCopy();
}
