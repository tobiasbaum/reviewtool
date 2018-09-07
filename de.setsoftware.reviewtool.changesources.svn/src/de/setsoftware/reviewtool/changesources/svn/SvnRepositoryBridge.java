package de.setsoftware.reviewtool.changesources.svn;

import java.io.ByteArrayOutputStream;

import org.tmatesoft.svn.core.ISVNLogEntryHandler;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.SVNClientManager;

import de.setsoftware.reviewtool.changesources.core.IScmCommitHandler;
import de.setsoftware.reviewtool.changesources.core.IScmRepositoryBridge;
import de.setsoftware.reviewtool.changesources.core.ScmException;
import de.setsoftware.reviewtool.model.api.IChangeSource;

/**
 * Implements {@link IScmRepositoryBridge} for Subversion.
 */
final class SvnRepositoryBridge implements IScmRepositoryBridge<SvnChangeItem, SvnCommitId, SvnCommit, SvnRepository> {

    private final SVNClientManager svnManager;

    /**
     * Constructor.
     * @param svnManager The underlying {@link SVNClientManager}.
     */
    SvnRepositoryBridge(final SVNClientManager svnManager) {
        this.svnManager = svnManager;
    }

    /**
     * Returns the underlying {@link SVNClientManager}.
     */
    SVNClientManager getSvnManager() {
        return this.svnManager;
    }

    @Override
    public SvnCommitId createCommitIdFromString(final String id) throws ScmException {
        try {
            return new SvnCommitId(Long.parseLong(id));
        } catch (final NumberFormatException e) {
            throw new ScmException(e);
        }
    }

    @Override
    public SvnRepository createRepository(final IChangeSource changeSource, final String id) throws ScmException {
        try {
            final SVNURL url = SVNURL.parseURIEncoded(id);
            final SvnRepository svnRepo = new SvnRepository(changeSource, url, this);
            final SVNURL repositoryRoot = svnRepo.getSvnRepository().getRepositoryRoot(true);
            if (!repositoryRoot.equals(url)) {
                return this.createRepository(changeSource, repositoryRoot.toString());
            } else {
                return svnRepo;
            }
        } catch (final SVNException e) {
            throw new ScmException(e);
        }
    }

    @Override
    public void getInitialCommits(
            final SvnRepository repo,
            final int maxNumberOfCommits,
            final IScmCommitHandler<SvnChangeItem, SvnCommitId, SvnCommit, Void> handler) throws ScmException {

        try {
            final long latestRevision = repo.getSvnRepository().getLatestRevision();
            final long startRevision = Math.max(0, latestRevision - maxNumberOfCommits + 1);
            this.getCommits(repo, startRevision, latestRevision, handler);
        } catch (final SVNException e) {
            throw new ScmException(e);
        }
    }

    @Override
    public void getNextCommits(
            final SvnRepository repo,
            final SvnCommitId lastKnownCommitId,
            final IScmCommitHandler<SvnChangeItem, SvnCommitId, SvnCommit, Void> handler) throws ScmException {

        try {
            final long latestRevision = repo.getSvnRepository().getLatestRevision();
            final long startRevision = lastKnownCommitId.getNumber() + 1;
            if (startRevision <= latestRevision) {
                this.getCommits(repo, startRevision, latestRevision, handler);
            }
        } catch (final SVNException e) {
            throw new ScmException(e);
        }
    }

    @Override
    public byte[] loadContents(
            final SvnRepository repo,
            final String path,
            final SvnCommitId commitId) throws ScmException {

        try {
            if (repo.getSvnRepository().checkPath(path, commitId.getNumber()) != SVNNodeKind.FILE) {
                return new byte[0];
            }

            final ByteArrayOutputStream contents = new ByteArrayOutputStream();
            repo.getSvnRepository().getFile(path, commitId.getNumber(), null, contents);
            return contents.toByteArray();
        } catch (final SVNException e) {
            throw new ScmException(e);
        }
    }

    private void getCommits(
            final SvnRepository repo,
            final long startRevision,
            final long endRevision,
            final IScmCommitHandler<SvnChangeItem, SvnCommitId, SvnCommit, Void> handler) throws ScmException {

        final ISVNLogEntryHandler handlerAdapter = new ISVNLogEntryHandler() {
            @Override
            public void handleLogEntry(final SVNLogEntry logEntry) throws SVNException {
                final SvnCommit commit = new SvnCommit(repo, logEntry);
                handler.processCommit(commit);
            }
        };

        try {
            repo.getSvnRepository().log(
                    null,   // no target paths (retrieve log entries of whole repository)
                    startRevision,
                    endRevision,
                    true,   // discover changed paths
                    false,  // don't stop at copy operations
                    0L,      // no log limit
                    false,  // don't include merge history
                    new String[0],
                    handlerAdapter);
        } catch (final SVNException e) {
            throw new ScmException(e);
        }
    }
}
