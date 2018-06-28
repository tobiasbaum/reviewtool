package de.setsoftware.reviewtool.changesources.core;

import java.io.Serializable;

import de.setsoftware.reviewtool.base.IPartiallyComparable;
import de.setsoftware.reviewtool.model.api.IRepoRevision;
import de.setsoftware.reviewtool.model.api.IRepository;

/**
 * Represents a commit ID.
 *
 * @param <CommitIdT> The concrete type of the commit ID. Must be {@link IPartiallyComparable partially ordered}.
 */
public interface IScmCommitId<CommitIdT extends IScmCommitId<CommitIdT>>
        extends IPartiallyComparable<CommitIdT>, Serializable {

    /**
     * Converts this commit ID into a {@link IRepoRevision} object.
     *
     * @param repo The repository to associate the commit with.
     */
    public abstract IRepoRevision<CommitIdT> toRevision(final IRepository repo);

    /**
     * Returns the string representation of a commit ID.
     */
    @Override
    public abstract String toString();
}
