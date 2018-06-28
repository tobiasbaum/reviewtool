package de.setsoftware.reviewtool.changesources.core;

import java.io.File;

import de.setsoftware.reviewtool.changesources.core.DefaultChangeSource;

/**
 * Contains helper operations for ChangeSource test cases.
 */
public abstract class AbstractChangeSourceTestCase {

    /**
     * Creates a test change source.
     */
    protected static DefaultChangeSource<
                StubChangeItem,
                StubCommitId,
                StubCommit,
                StubRepo,
                StubLocalChange,
                StubWorkingCopy,
                StubScmRepositoryBridge,
                StubScmWorkingCopyBridge> changeSource() {
        return new DefaultChangeSource<>(
                new StubScmRepositoryBridge(),
                new StubScmWorkingCopyBridge(),
                ".*$${key}([^0-9].*)?",
                1048576L,
                5000);
    }

    /**
     * Creates a test repository.
     */
    protected static StubRepo repo(final DefaultChangeSource<
                    StubChangeItem,
                    StubCommitId,
                    StubCommit,
                    StubRepo,
                    StubLocalChange,
                    StubWorkingCopy,
                    StubScmRepositoryBridge,
                    StubScmWorkingCopyBridge> changeSource, final String id) {
        return new StubRepo(changeSource, id, changeSource.getScmRepoBridge());
    }

    /**
     * Creates a test working copy.
     */
    protected static StubWorkingCopy wc(final DefaultChangeSource<
                    StubChangeItem,
                    StubCommitId,
                    StubCommit,
                    StubRepo,
                    StubLocalChange,
                    StubWorkingCopy,
                    StubScmRepositoryBridge,
                    StubScmWorkingCopyBridge> changeSource, final StubRepo repo, final File localRoot, final String relPath) {
        return new StubWorkingCopy(repo, changeSource.getScmWcBridge(), localRoot, relPath);
    }
}
