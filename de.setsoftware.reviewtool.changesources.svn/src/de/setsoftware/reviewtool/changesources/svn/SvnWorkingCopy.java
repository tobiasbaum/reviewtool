package de.setsoftware.reviewtool.changesources.svn;

import java.io.File;

import de.setsoftware.reviewtool.changesources.core.DefaultScmWorkingCopy;

/**
 * Implements {@link DefaultScmWorkingCopy} for Subversion.
 */
final class SvnWorkingCopy extends
        DefaultScmWorkingCopy<SvnChangeItem, SvnCommitId, SvnCommit, SvnRepository, SvnLocalChange, SvnWorkingCopy> {

    /**
     * Constructor.
     * @param repo The associated Subversion repository.
     * @param scmBridge The {@link SvnWorkingCopyBridge} to use.
     * @param workingCopyRoot The directory at the root of the working copy.
     * @param relPath The relative path of the working copy root wrt. the URL of the remote repository.
     */
    SvnWorkingCopy(
            final SvnRepository repo,
            final SvnWorkingCopyBridge scmBridge,
            final File workingCopyRoot,
            final String relPath) {
        super(repo, scmBridge, workingCopyRoot, relPath);
    }

    @Override
    public SvnWorkingCopy getThis() {
        return this;
    }
}
