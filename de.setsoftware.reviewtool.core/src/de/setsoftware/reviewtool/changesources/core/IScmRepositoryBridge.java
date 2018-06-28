package de.setsoftware.reviewtool.changesources.core;

import de.setsoftware.reviewtool.model.api.IChangeSource;

/**
 * Translates high-level model operations to low-level SCM operations on a repository.
 *
 * @param <ItemT> Type of a change item.
 * @param <CommitIdT> Type of a commit ID.
 * @param <CommitT> Type of a commit.
 * @param <RepoT> Type of the repository.
 */
public interface IScmRepositoryBridge<
        ItemT extends IScmChangeItem,
        CommitIdT extends IScmCommitId<CommitIdT>,
        CommitT extends IScmCommit<ItemT, CommitIdT>,
        RepoT extends IScmRepository<ItemT, CommitIdT, CommitT, RepoT>> {

    /**
     * Creates an ID from a string.
     *
     * @param id The commit ID as a string.
     * @return An {@link CommitIdT} object.
     */
    public abstract CommitIdT createCommitIdFromString(String id) throws ScmException;

    /**
    * Returns a {@link RepoT} object for a repository with passed ID.
    *
    * @param changeSource The change source.
    * @param id The ID of the repository.
    * @return A suitable {@link RepoT} object or {@code null} if no repository has been detected.
    *         Note that this may mean either that the ID belongs to no repository at all,
    *         or that it belongs to a repository of some other SCM.
     */
    public abstract RepoT createRepository(IChangeSource changeSource, String id) throws ScmException;

    /**
     * Determines the initial list of commits on the current branch.
     *
     * @param repo The repository.
     * @param maxNumberOfCommits At most the last {@code maxNumberOfCommits} commits are returned.
     * @param handler A {@link IScmCommitHandler} to receive the commits.
     */
    public abstract void getInitialCommits(
            RepoT repo,
            int maxNumberOfCommits,
            IScmCommitHandler<ItemT, CommitIdT, CommitT, Void> handler) throws ScmException;

    /**
     * Determines all commits since passed commit and the tip of the current branch.
     *
     * @param repo The repository.
     * @param lastKnownCommitId The ID of the last commit known to the caller.
     * @param handler A {@link IScmCommitHandler} to receive the commits.
     */
    public abstract void getNextCommits(
            RepoT repo,
            CommitIdT lastKnownCommitId,
            IScmCommitHandler<ItemT, CommitIdT, CommitT, Void> handler) throws ScmException;

    /**
     * Loads the contents of some file in some commit of the repository.
     *
     * @param repo The repository.
     * @param path The file path.
     * @param commitId The commit ID.
     * @return The file contents as a byte array.
     */
    public abstract byte[] loadContents(
            RepoT repo,
            String path,
            CommitIdT commitId) throws ScmException;
}
