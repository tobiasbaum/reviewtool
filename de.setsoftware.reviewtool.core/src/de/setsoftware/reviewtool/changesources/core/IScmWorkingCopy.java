package de.setsoftware.reviewtool.changesources.core;

import java.io.File;
import java.util.List;

import de.setsoftware.reviewtool.model.api.IWorkingCopy;

/**
 * Interface to a {@link IWorkingCopy} which allows to access and modify various data associated with the working copy.
 *
 * @param <ItemT> Type of a change item.
 * @param <CommitIdT> Type of a commit ID.
 * @param <CommitT> Type of a commit.
 * @param <RepoT> Type of the repository.
 * @param <LocalChangeT> Type of a local change.
 * @param <WcT> Type of the working copy.
 */
public interface IScmWorkingCopy<
        ItemT extends IScmChangeItem,
        CommitIdT extends IScmCommitId<CommitIdT>,
        CommitT extends IScmCommit<ItemT, CommitIdT>,
        RepoT extends IScmRepository<ItemT, CommitIdT, CommitT, RepoT>,
        LocalChangeT extends IScmLocalChange<ItemT>,
        WcT extends IScmWorkingCopy<ItemT, CommitIdT, CommitT, RepoT, LocalChangeT, WcT>> extends IWorkingCopy {

    /**
     * Returns a properly typed reference to this working copy.
     */
    public abstract WcT getThis();

    @Override
    public abstract IScmRepository<ItemT, CommitIdT, CommitT, RepoT> getRepository();

    /**
     * Collects local changes in this given working copy and integrates them into its file history graph.
     *
     * @param pathsToCheck The list of additional paths to check. If {@code null}, the whole working copy is analyzed.
     */
    public abstract void collectLocalChanges(final List<File> pathsToCheck) throws ScmException;

    /**
     * Returns the ID of the last commit of the working copy on its current branch.
     */
    public abstract CommitIdT getIdOfLastCommit() throws ScmException;

    /**
     * Updates the working copy by rebasing local changes onto the upstream's branch tip.
     *
     * @throws ScmException if rebasing fails, e.g. due to conflicts.
     */
    public abstract void update() throws ScmException;
}
