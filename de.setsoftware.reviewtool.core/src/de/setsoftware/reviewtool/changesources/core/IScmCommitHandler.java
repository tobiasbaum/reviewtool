package de.setsoftware.reviewtool.changesources.core;

/**
 * Processes commits.
 *
 * @param <ItemT> Type of a change item.
 * @param <CommitIdT> Type of a commit ID.
 * @param <CommitT> Type of a commit.
 * @param <R> Return type of {@link #processCommit(IScmCommit)}.
 */
public interface IScmCommitHandler<
        ItemT extends IScmChangeItem,
        CommitIdT extends IScmCommitId<CommitIdT>,
        CommitT extends IScmCommit<ItemT, CommitIdT>,
        R> {

    /**
     * Processes a single commit.
     *
     * @param commit The commit to handle.
     */
    public abstract R processCommit(CommitT commit);
}
