package de.setsoftware.reviewtool.changesources.core;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.OperationCanceledException;

import de.setsoftware.reviewtool.base.Logger;
import de.setsoftware.reviewtool.base.Multimap;
import de.setsoftware.reviewtool.base.Pair;
import de.setsoftware.reviewtool.model.api.IChangeSource;
import de.setsoftware.reviewtool.model.api.IChangeSourceUi;

/**
 * Default implementation of the {@link IScmWorkingCopyManager} interface.
 *
 * @param <ItemT> Type of a change item.
 * @param <CommitIdT> Type of a commit ID.
 * @param <CommitT> Type of a commit.
 * @param <RepoT> Type of the repository.
 * @param <LocalChangeT> Type of a local change.
 * @param <WcT> Type of the working copy.
 */
final class DefaultScmWorkingCopyManager<
        ItemT extends IScmChangeItem,
        CommitIdT extends IScmCommitId<CommitIdT>,
        CommitT extends IScmCommit<ItemT, CommitIdT>,
        RepoT extends DefaultScmRepository<ItemT, CommitIdT, CommitT, RepoT>,
        LocalChangeT extends IScmLocalChange<ItemT>,
        WcT extends DefaultScmWorkingCopy<ItemT, CommitIdT, CommitT, RepoT, LocalChangeT, WcT>>
            implements IScmWorkingCopyManager<ItemT, CommitIdT, CommitT, RepoT, LocalChangeT, WcT> {

    private final IChangeSource changeSource;
    private final IScmRepositoryManager<ItemT, CommitIdT, CommitT, RepoT> repoManager;
    private final IScmWorkingCopyBridge<ItemT, CommitIdT, CommitT, RepoT, LocalChangeT, WcT> scmBridge;
    private final Map<String, WcT> wcPerRootDirectory;

    /**
     * Constructor.
     *
     * @param changeSource The associated change source.
     * @param scmBridge The SCM bridge to use.
     */
    DefaultScmWorkingCopyManager(
            final IChangeSource changeSource,
            final IScmRepositoryManager<ItemT, CommitIdT, CommitT, RepoT> repoManager,
            final IScmWorkingCopyBridge<ItemT, CommitIdT, CommitT, RepoT, LocalChangeT, WcT> scmBridge) {
        this.changeSource = changeSource;
        this.repoManager = repoManager;
        this.scmBridge = scmBridge;
        this.wcPerRootDirectory = new LinkedHashMap<>();
        ManagerOfScmManagers.getInstance().addWorkingCopyManager(changeSource.getId(), this);
    }

    @Override
    public IScmWorkingCopyBridge<ItemT, CommitIdT, CommitT, RepoT, LocalChangeT, WcT> getScmBridge() {
        return this.scmBridge;
    }

    @Override
    public synchronized Collection<WcT> getWorkingCopies() {
        return Collections.unmodifiableCollection(this.wcPerRootDirectory.values());
    }

    @Override
    public WcT getWorkingCopy(final File directory) {
        final File workingCopyRoot = this.scmBridge.detectWorkingCopyRoot(directory);
        if (workingCopyRoot == null) {
            return null;
        }

        synchronized (this) {
            WcT wc = this.wcPerRootDirectory.get(workingCopyRoot.toString());
            if (wc == null) {
                try {
                    wc = this.scmBridge.createWorkingCopy(this.changeSource, this.repoManager, workingCopyRoot);
                } catch (final ScmException e) {
                    Logger.error("Could not access working copy at " + workingCopyRoot, e);
                }
            }
            if (wc != null) {
                this.wcPerRootDirectory.put(workingCopyRoot.toString(), wc);
            }
            return wc;
        }
    }

    @Override
    public synchronized Multimap<WcT, CommitT> filterCommits(
            final IScmCommitHandler<ItemT, CommitIdT, CommitT, Boolean> filter,
            final IChangeSourceUi ui) throws ScmException {

        final Multimap<WcT, CommitT> commitsPerWc = new Multimap<>();
        for (final WcT wc : this.wcPerRootDirectory.values()) {
            if (ui.isCanceled()) {
                throw new OperationCanceledException();
            }

            final IScmRepositoryManager<ItemT, CommitIdT, CommitT, RepoT> repoManager =
                    ManagerOfScmManagers.getInstance().getRepositoryManager(this.changeSource.getId());
            final Pair<Boolean, List<CommitT>> getEntriesResult =
                    repoManager.filterCommits(wc.getRepository(), filter, ui);
            for (final CommitT commit : getEntriesResult.getSecond()) {
                commitsPerWc.put(wc, commit);
            }

            if (getEntriesResult.getFirst()) {
                // remote history has changed, we have to rebuild the local file history graph
                wc.collectLocalChanges(Collections.<File>emptyList());
            }
        }
        return commitsPerWc;
    }

    /**
     * Removes a working copy.
     * @param workingCopyRoot The root directory of the working copy.
     */
    @Override
    public synchronized void removeWorkingCopy(final File workingCopyRoot) {
        this.wcPerRootDirectory.remove(workingCopyRoot.toString());
    }

    @Override
    public void collectLocalChanges(final List<File> pathsToCheck) throws ScmException {
        for (final WcT wc : this.getWorkingCopies()) {
            wc.collectLocalChanges(pathsToCheck);
        }
    }
}
