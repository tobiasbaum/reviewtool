package de.setsoftware.reviewtool.changesources.core;

import java.io.File;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

import de.setsoftware.reviewtool.base.Multimap;
import de.setsoftware.reviewtool.model.api.IChangeSourceUi;

/**
 * Manages all known local working copies for a given change source.
 *
 * @param <ItemT> Type of a change item.
 * @param <CommitIdT> Type of a commit ID.
 * @param <CommitT> Type of a commit.
 * @param <RepoT> Type of the repository.
 * @param <LocalChangeT> Type of a local change.
 * @param <WcT> Type of the working copy.
 */
public interface IScmWorkingCopyManager<
        ItemT extends IScmChangeItem,
        CommitIdT extends IScmCommitId<CommitIdT>,
        CommitT extends IScmCommit<ItemT, CommitIdT>,
        RepoT extends IScmRepository<ItemT, CommitIdT, CommitT, RepoT>,
        LocalChangeT extends IScmLocalChange<ItemT>,
        WcT extends IScmWorkingCopy<ItemT, CommitIdT, CommitT, RepoT, LocalChangeT, WcT>> {

    /**
     * Returns the associated {@link IScmWorkingCopyBridge}.
     */
    public abstract IScmWorkingCopyBridge<ItemT, CommitIdT, CommitT, RepoT, LocalChangeT, WcT> getScmBridge();

    /**
     * Returns a read-only view of all known local working copies.
     */
    public abstract Collection<WcT> getWorkingCopies();

    /**
     * Tries to detect a working copy at passed directory.
     * The directory needs to point somewhere into the working copy, it is not required to be the its root directory.
     * A working copy returned is registered with the manager and needs to be removed via
     * {@link #removeWorkingCopy(File)} if not needed anymore.
     *
     * @param directory The directory assumed to be within a working copy.
     * @return A suitable {@link IScmWorkingCopy} object or {@code null} if no working copy has been detected.
     *         Note that this may mean either that the directory belongs to no working copy at all,
     *         or that it belongs to a working copy of some other SCM.
     */
    public abstract WcT getWorkingCopy(File directory) throws ScmException;

    /**
     * Removes a working copy.
     *
     * @param workingCopyRoot The root directory of the working copy.
     */
    public abstract void removeWorkingCopy(File workingCopyRoot);

    /**
     * Filters all commits of all known working copies.
     * If there are commits in the remote repositories that have not been processed yet, they are loaded and processed
     * before being filtered.
     *
     * <p>Completely processed commits are stored to disk in the background even if not all commits could be loaded
     * due to cancellation via {@link IProgressMonitor#setCanceled(boolean)}.
     *
     * @param filter The commit handler to use for filtering commits.
     * @return A list of pairs of a {@link IScmWorkingCopy} and an associated repository revision.
     */
    public abstract Multimap<WcT, CommitT> filterCommits(
            final IScmCommitHandler<ItemT, CommitIdT, CommitT, Boolean> filter,
            final IChangeSourceUi ui) throws ScmException;

    /**
     * Collects local changes in all known working copies and integrates them into their respective file history graphs.
     *
     * @param pathsToCheck The list of additional paths to check.
     *                     If {@code null}, the whole working copies are analyzed.
     */
    public abstract void collectLocalChanges(final List<File> pathsToCheck) throws ScmException;
}
