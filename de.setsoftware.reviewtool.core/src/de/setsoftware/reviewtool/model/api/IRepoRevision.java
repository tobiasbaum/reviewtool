package de.setsoftware.reviewtool.model.api;

import de.setsoftware.reviewtool.base.IPartiallyComparable;

/**
 * A real revision in the SCM repository.
 *
 * @param <RevIdT> The type of the underlying revision identifier.
 */
public interface IRepoRevision<RevIdT extends IPartiallyComparable<RevIdT>> extends IRevision {

    /**
     * Returns the ID of the repository revision.
     */
    public abstract RevIdT getId();
}
