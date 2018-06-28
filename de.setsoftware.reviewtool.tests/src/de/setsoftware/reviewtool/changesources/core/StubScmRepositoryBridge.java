package de.setsoftware.reviewtool.changesources.core;

import java.util.Date;
import java.util.TreeMap;

import de.setsoftware.reviewtool.changesources.core.IScmCommitHandler;
import de.setsoftware.reviewtool.changesources.core.IScmRepositoryBridge;
import de.setsoftware.reviewtool.changesources.core.ScmException;
import de.setsoftware.reviewtool.model.api.IChangeSource;

public class StubScmRepositoryBridge implements IScmRepositoryBridge<
        StubChangeItem, StubCommitId, StubCommit, StubRepo> {

    @Override
    public final StubCommitId createCommitIdFromString(final String id) throws ScmException {
        try {
            return new StubCommitId(Long.parseLong(id));
        } catch (final NumberFormatException e) {
            throw new ScmException(e);
        }
    }

    @Override
    public final StubRepo createRepository(final IChangeSource changeSource, final String id) throws ScmException {
        return new StubRepo(changeSource, id, this);
    }

    @Override
    public void getInitialCommits(
            final StubRepo repo,
            final int maxNumberOfCommits,
            final IScmCommitHandler<StubChangeItem, StubCommitId, StubCommit, Void> handler) throws ScmException {
        handler.processCommit(new StubCommit(
                repo,
                new TreeMap<>(),
                0L,
                "",
                "",
                new Date(0)));
    }

    @Override
    public void getNextCommits(
            final StubRepo repo,
            final StubCommitId lastKnownCommitId,
            final IScmCommitHandler<StubChangeItem, StubCommitId, StubCommit, Void> handler) throws ScmException {
        // no commits
    }

    @Override
    public byte[] loadContents(
            final StubRepo repo,
            final String path,
            final StubCommitId commitId) throws ScmException {
        // no files
        return new byte[0];
    }
}
