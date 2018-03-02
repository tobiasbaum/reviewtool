package de.setsoftware.reviewtool.model.api;

/**
 * A real revision in the SCM repository.
 */
public interface IRepoRevision extends IRevision {

    /**
     * Returns the ID of the repository revision.
     */
    public abstract Object getId();

}
