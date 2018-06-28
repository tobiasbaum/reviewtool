package de.setsoftware.reviewtool.changesources.core;

import java.io.File;
import java.util.Set;
import java.util.SortedMap;

import de.setsoftware.reviewtool.changesources.core.IScmChangeItemHandler;
import de.setsoftware.reviewtool.changesources.core.IScmRepositoryManager;
import de.setsoftware.reviewtool.changesources.core.IScmWorkingCopyBridge;
import de.setsoftware.reviewtool.changesources.core.ScmException;
import de.setsoftware.reviewtool.model.api.IChangeSource;

public class StubScmWorkingCopyBridge implements IScmWorkingCopyBridge<
        StubChangeItem, StubCommitId, StubCommit, StubRepo, StubLocalChange, StubWorkingCopy> {

    @Override
    public File detectWorkingCopyRoot(final File directory) {
        return directory;
    }

    @Override
    public StubWorkingCopy createWorkingCopy(
            final IChangeSource changeSource,
            final IScmRepositoryManager<StubChangeItem, StubCommitId, StubCommit, StubRepo> repoManager,
            final File workingCopyRoot) throws ScmException {
        return new StubWorkingCopy(repoManager.getRepo(workingCopyRoot.toString()), this, workingCopyRoot, "");
    }

    @Override
    public StubLocalChange createLocalChange(
            final StubWorkingCopy wc,
            final SortedMap<String, StubChangeItem> changeMap) throws ScmException {
        return new StubLocalChange(wc, changeMap);
    }

    @Override
    public void collectLocalChanges(final StubWorkingCopy wc, final Set<File> pathsToCheck,
            final IScmChangeItemHandler<StubChangeItem, Void> handler) throws ScmException {
        // no local changes
    }

    @Override
    public StubCommitId getIdOfLastCommit(final StubWorkingCopy wc) throws ScmException {
        return new StubCommitId(0);
    }

    @Override
    public void updateWorkingCopy(final StubWorkingCopy wc) throws ScmException {
        // no unfetched commits
    }
}
