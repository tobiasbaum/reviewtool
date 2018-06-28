package de.setsoftware.reviewtool.changesources.core;

import java.io.Serializable;
import java.util.Date;

import de.setsoftware.reviewtool.base.IPartiallyComparable;
import de.setsoftware.reviewtool.model.api.IRepoRevision;
import de.setsoftware.reviewtool.model.api.IRepository;

/**
 * Describes a repository change (a.k.a. commit) which is basically a {@link IScmChange} augmented by some meta data,
 * the most important of which is the commit ID.
 *
 * @param <ItemT> Type of a change item.
 * @param <CommitIdT> Type of a commit ID.
 */
public interface IScmCommit<ItemT extends IScmChangeItem, CommitIdT extends IScmCommitId<CommitIdT>>
            extends IScmChange<ItemT>, IPartiallyComparable<IScmCommit<ItemT, CommitIdT>>, Serializable {

    /**
     * Returns the repository of this commit.
     */
    public abstract IRepository getRepository();

    /**
     * Returns the ID of this commit.
     */
    public abstract CommitIdT getId();

    /**
     * Returns the person that committed this change.
     */
    public abstract String getCommitter();

    /**
     * Returns the date this change was committed to the repository.
     */
    public abstract Date getCommitDate();

    /**
     * Returns the message for this commit.
     */
    public abstract String getMessage();

    /**
     * Returns the pretty string representation of a commit.
     */
    public default String toPrettyString() {
        final StringBuilder sb = new StringBuilder();
        final String message = this.getMessage();
        if (!message.isEmpty()) {
            sb.append(message);
            sb.append(" ");
        }
        sb.append(String.format(
                "(Rev. %s, %s)",
                this.getId(),
                this.getCommitter()));
        return sb.toString();
    }

    /**
     * Returns the {@link IRepoRevision} for this repository change.
     */
    @Override
    public abstract IRepoRevision<CommitIdT> toRevision();

    /**
     * Commits are partially ordered according to their IDs.
     */
    @Override
    public default boolean le(final IScmCommit<ItemT, CommitIdT> other) {
        return this.getId().le(other.getId());
    }
}
