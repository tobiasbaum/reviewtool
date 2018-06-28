package de.setsoftware.reviewtool.changesources.core;

import java.io.File;
import java.util.Set;
import java.util.SortedMap;

import de.setsoftware.reviewtool.model.api.IChangeSource;

/**
 * Translates high-level model operations to low-level SCM operations on a working copy.
 *
 * @param <ItemT> Type of a change item.
 * @param <CommitIdT> Type of a commit ID.
 * @param <CommitT> Type of a commit.
 * @param <RepoT> Type of the repository.
 * @param <LocalChangeT> Type of a local change.
 * @param <WcT> Type of the working copy.
 */
public interface IScmWorkingCopyBridge<
        ItemT extends IScmChangeItem,
        CommitIdT extends IScmCommitId<CommitIdT>,
        CommitT extends IScmCommit<ItemT, CommitIdT>,
        RepoT extends IScmRepository<ItemT, CommitIdT, CommitT, RepoT>,
        LocalChangeT extends IScmLocalChange<ItemT>,
        WcT extends IScmWorkingCopy<ItemT, CommitIdT, CommitT, RepoT, LocalChangeT, WcT>> {

    /**
     * Tries to detect a working copy at passed directory.
     * The directory needs to point somewhere into the working copy, it is not required to be the its root directory.
     *
     * @param directory The directory assumed to be within a working copy.
     * @return The root of the working copy or {@code null} if no working copy has been detected.
     *         Note that this may mean either that the directory belongs to no working copy at all,
     *         or that it belongs to a working copy of some other SCM.
     */
    public abstract File detectWorkingCopyRoot(File directory);

    /**
    * Returns a {@link WcT} object for a working copy rooted at passed directory.
    * A working copy returned is registered with the manager and needs to be removed via
    * {@link #removeWorkingCopy(File)} if not needed anymore.
    *
    * @param changeSource The change source.
    * @param repoManager The repository manager.
    * @param workingCopyRoot The root directory of the working copy.
    * @return A suitable {@link WcT} object or {@code null} if no working copy has been detected.
    *         Note that this may mean either that the directory belongs to no working copy at all,
    *         or that it belongs to a working copy of some other SCM.
     */
    public abstract WcT createWorkingCopy(
            IChangeSource changeSource,
            IScmRepositoryManager<ItemT, CommitIdT, CommitT, RepoT> repoManager,
            File workingCopyRoot) throws ScmException;

    /**
     * Creates a local change.
     *
     * @param wc The working copy.
     * @param changeMap The map of changed paths.
     * @return A {@link LocalChangeT} object.
     */
    public abstract LocalChangeT createLocalChange(
            WcT wc,
            SortedMap<String, ItemT> changeMap) throws ScmException;

    /**
     * Collects local changes in a working copy.
     *
     * @param wc The working copy to check.
     * @param pathsToCheck The paths to check. If {@code null}, the whole working copy is analyzed.
     * @param handler Receives information about changed items.
     */
    public abstract void collectLocalChanges(
            final WcT wc,
            Set<File> pathsToCheck,
            IScmChangeItemHandler<ItemT, Void> handler) throws ScmException;

    /**
     * Returns the ID of the last commit of the working copy on its current branch.
     *
     * @param wc The working copy to check.
     */
    public abstract CommitIdT getIdOfLastCommit(final WcT wc) throws ScmException;

    /**
     * Updates the working copy by rebasing local changes onto the upstream's branch tip.
     *
     * @param wc The working copy to update.
     * @throws ScmException if rebasing fails, e.g. due to conflicts.
     */
    public abstract void updateWorkingCopy(final WcT wc) throws ScmException;
}
