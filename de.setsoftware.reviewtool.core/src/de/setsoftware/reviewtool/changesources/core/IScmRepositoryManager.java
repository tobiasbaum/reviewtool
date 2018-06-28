package de.setsoftware.reviewtool.changesources.core;

import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

import de.setsoftware.reviewtool.base.Pair;
import de.setsoftware.reviewtool.model.api.IChangeSourceUi;

/**
 * Manages all known remote repositories for a given change source.
 *
 * @param <ItemT> Type of a change item.
 * @param <CommitIdT> Type of a commit ID.
 * @param <CommitT> Type of a commit.
 * @param <RepoT> Type of the repository.
 */
public interface IScmRepositoryManager<
        ItemT extends IScmChangeItem,
        CommitIdT extends IScmCommitId<CommitIdT>,
        CommitT extends IScmCommit<ItemT, CommitIdT>,
        RepoT extends IScmRepository<ItemT, CommitIdT, CommitT, RepoT>> {

    /**
     * Returns the associated {@link IScmRepositoryBridge}.
     */
    public abstract IScmRepositoryBridge<ItemT, CommitIdT, CommitT, RepoT> getScmBridge();

    /**
     * Returns a read-only view of all known repositories.
     */
    public abstract Collection<RepoT> getRepositories();

    /**
     * Returns a {@link IScmRepository} given its ID.
     * @param id The ID of the remote repository.
     * @return A {@link IScmRepository} for the given ID.
     */
    public abstract RepoT getRepo(String id);

    /**
     * Filters all commits of the given {@link IScmRepository}.
     * If there are commits in the remote repository that have not been processed yet, they are loaded and processed
     * before being filtered.
     *
     * <p>Completely processed commits are stored to disk in the background even if not all commits could be loaded
     * due to cancellation via {@link IProgressMonitor#setCanceled(boolean)}.
     *
     * @param repo The repository.
     * @param filter The commit handler to use for filtering commits.
     * @return A pair of a boolean value and a list of repository revisions. The boolean flag indicates whether new
     *         commits have been processed.
     */
    public abstract Pair<Boolean, List<CommitT>> filterCommits(
            RepoT repo,
            IScmCommitHandler<ItemT, CommitIdT, CommitT, Boolean> filter,
            IChangeSourceUi ui) throws ScmException;
}
