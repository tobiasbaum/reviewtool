package de.setsoftware.reviewtool.changesources.svn;

import java.io.File;
import java.util.Set;
import java.util.SortedMap;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.ISVNStatusHandler;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNInfo;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNStatus;

import de.setsoftware.reviewtool.changesources.core.IScmChangeItemHandler;
import de.setsoftware.reviewtool.changesources.core.IScmRepositoryManager;
import de.setsoftware.reviewtool.changesources.core.IScmWorkingCopyBridge;
import de.setsoftware.reviewtool.changesources.core.ScmException;
import de.setsoftware.reviewtool.model.api.IChangeSource;

/**
 * Implements {@link IScmWorkingCopyBridge} for Subversion.
 */
final class SvnWorkingCopyBridge implements
        IScmWorkingCopyBridge<SvnChangeItem, SvnCommitId, SvnCommit, SvnRepository, SvnLocalChange, SvnWorkingCopy> {

    private final SVNClientManager svnManager;

    /**
     * Constructor.
     * @param svnManager The underlying {@link SVNClientManager}.
     */
    SvnWorkingCopyBridge(final SVNClientManager svnManager) {
        this.svnManager = svnManager;
    }

    /**
     * Returns the underlying {@link SVNClientManager}.
     */
    SVNClientManager getSvnManager() {
        return this.svnManager;
    }

    @Override
    public File detectWorkingCopyRoot(final File directory) {
        File potentialRoot = directory;
        while (!this.isPotentialRoot(potentialRoot)) {
            potentialRoot = potentialRoot.getParentFile();
            if (potentialRoot == null) {
                return null;
            }
        }
        while (true) {
            final File next = potentialRoot.getParentFile();
            if (next == null || !this.isPotentialRoot(next)) {
                return potentialRoot;
            }
            potentialRoot = next;
        }
    }

    @Override
    public SvnWorkingCopy createWorkingCopy(
            final IChangeSource changeSource,
            final IScmRepositoryManager<SvnChangeItem, SvnCommitId, SvnCommit, SvnRepository> repoManager,
            final File workingCopyRoot) {

        final SVNInfo wcInfo;
        try {
            wcInfo = this.svnManager.getWCClient().doInfo(workingCopyRoot, SVNRevision.WORKING);
        } catch (final SVNException e) {
            // not a working copy
            // don't log this one as it is probably an unversioned project and hence not an error
            return null;
        }

        final SVNURL wcUrl = wcInfo.getURL();
        final SvnRepository repo = repoManager.getRepo(wcUrl.toString());
        if (repo == null) {
            return null;
        }

        final String relPath = wcUrl.toString().substring(repo.getId().length());
        return new SvnWorkingCopy(repo, this, workingCopyRoot, relPath);
    }

    @Override
    public SvnLocalChange createLocalChange(
            final SvnWorkingCopy wc,
            final SortedMap<String, SvnChangeItem> changeMap) {
        return new SvnLocalChange(wc, changeMap);
    }

    @Override
    public void collectLocalChanges(
            final SvnWorkingCopy wc,
            final Set<File> pathsToCheck,
            final IScmChangeItemHandler<SvnChangeItem, Void> handler) throws ScmException {

        final ISVNStatusHandler handlerAdapter = new ISVNStatusHandler() {
            @Override
            public void handleStatus(final SVNStatus status) throws SVNException {
                if (status.isVersioned()) {
                    final SvnChangeItem item = new SvnChangeItem(wc.getRepository(), status);
                    handler.processChangeItem(item);
                }
            }
        };

        try {
            if (pathsToCheck == null) {
                this.svnManager.getStatusClient().doStatus(
                        wc.getLocalRoot(), // analyze whole working copy
                        SVNRevision.WORKING,
                        SVNDepth.INFINITY,
                        false, // no remote
                        false, // report only modified files
                        false, // don't include ignored files
                        false, // ignored
                        handlerAdapter,
                        null); // no change lists
            } else {
                for (final File path : pathsToCheck) {
                    this.svnManager.getStatusClient().doStatus(
                            path,
                            SVNRevision.WORKING,
                            SVNDepth.EMPTY,
                            false, // no remote
                            false, // report only modified files
                            false, // don't include ignored files
                            false, // ignored
                            handlerAdapter,
                            null); // no change lists
                }
            }
        } catch (final SVNException e) {
            throw new ScmException(e);
        }
    }

    @Override
    public SvnCommitId getIdOfLastCommit(final SvnWorkingCopy wc) throws ScmException {
        try {
            final File wcRoot = wc.getLocalRoot();
            final long wcRev = this.svnManager.getStatusClient().doStatus(wcRoot, false).getRevision().getNumber();
            return new SvnCommitId(wcRev);
        } catch (final SVNException e) {
            throw new ScmException(e);
        }
    }

    @Override
    public void updateWorkingCopy(final SvnWorkingCopy wc) throws ScmException {
        try {
            this.svnManager.getUpdateClient().doUpdate(
                    wc.getLocalRoot(),
                    SVNRevision.HEAD,
                    SVNDepth.INFINITY,
                    true,
                    false);
        } catch (final SVNException e) {
            throw new ScmException(e);
        }
    }

    private boolean isPotentialRoot(final File next) {
        final File dotsvn = new File(next, ".svn");
        return dotsvn.isDirectory();
    }
}
