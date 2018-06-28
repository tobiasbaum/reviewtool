package de.setsoftware.reviewtool.changesources.core;

import java.io.File;

import de.setsoftware.reviewtool.changesources.core.DefaultScmWorkingCopy;

public final class StubWorkingCopy extends DefaultScmWorkingCopy<
        StubChangeItem, StubCommitId, StubCommit, StubRepo, StubLocalChange, StubWorkingCopy> {

    public StubWorkingCopy(
            final StubRepo repo,
            final StubScmWorkingCopyBridge scmBridge,
            final File localRoot,
            final String relPath) {
        super(repo, scmBridge, localRoot, relPath);
    }

    @Override
    public StubWorkingCopy getThis() {
        return this;
    }
}
