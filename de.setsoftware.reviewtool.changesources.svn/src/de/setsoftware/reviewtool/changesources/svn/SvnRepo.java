package de.setsoftware.reviewtool.changesources.svn;

import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.tmatesoft.svn.core.ISVNLogEntryHandler;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.io.SVNRepository;

import de.setsoftware.reviewtool.base.ComparableWrapper;
import de.setsoftware.reviewtool.diffalgorithms.DiffAlgorithmFactory;
import de.setsoftware.reviewtool.model.api.IMutableFileHistoryGraph;
import de.setsoftware.reviewtool.model.api.IRepoRevision;
import de.setsoftware.reviewtool.model.changestructure.AbstractRepository;
import de.setsoftware.reviewtool.model.changestructure.ChangestructureFactory;
import de.setsoftware.reviewtool.model.changestructure.FileHistoryGraph;

/**
 * Represents a remote Subversion repository.
 * Such a repository contains a cache of the history to speed up the gathering of relevant entries
 * as well as a cache of requested file contents.
 */
final class SvnRepo extends AbstractRepository implements ISvnRepo {

    /**
     * References a SVN repository by its remote URL.
     */
    private static final class SvnRepoRef implements Serializable {

        private static final long serialVersionUID = 8155878129812235537L;
        private final String remoteUrl;

        /**
         * Constructor.
         * @param remoteUrl The remote URL of the repository.
         */
        SvnRepoRef(final SVNURL remoteUrl) {
            this.remoteUrl = remoteUrl.toString();
        }

        private Object readResolve() throws ObjectStreamException {
            try {
                final SvnRepo repo = SvnRepositoryManager.getInstance().getRepo(SVNURL.parseURIEncoded(this.remoteUrl));
                if (repo == null) {
                    throw new InvalidObjectException("No repository found at " + this.remoteUrl);
                }
                return repo;
            } catch (final SVNException e) {
                final InvalidObjectException ex =
                        new InvalidObjectException("Problem while creating URL for " + this.remoteUrl);
                ex.initCause(e);
                throw ex;
            }
        }
    }

    private static final long serialVersionUID = 8792151363600093081L;

    private final SVNRepository svnRepo;
    private final String id;
    private final SVNURL remoteUrl;
    private final SvnFileCache fileCache;
    private final List<CachedLogEntry> entries;
    private IMutableFileHistoryGraph fileHistoryGraph;

    SvnRepo(final SVNRepository svnRepo, final SVNURL remoteUrl) throws SVNException {
        this.svnRepo = svnRepo;
        this.id = svnRepo.getRepositoryUUID(true);
        this.remoteUrl = remoteUrl;
        this.fileCache = new SvnFileCache(this.svnRepo);
        this.entries = new ArrayList<>();
        this.fileHistoryGraph = new FileHistoryGraph(DiffAlgorithmFactory.createDefault());
    }

    @Override
    public SVNURL getRemoteUrl() {
        return this.remoteUrl;
    }

    @Override
    public List<CachedLogEntry> getEntries() {
        return Collections.unmodifiableList(this.entries);
    }

    @Override
    public void appendNewEntries(final Collection<CachedLogEntry> newEntries) {
        this.entries.addAll(newEntries);
    }

    @Override
    public IPath getCacheFilePath() {
        final Bundle bundle = FrameworkUtil.getBundle(this.getClass());
        final IPath dir = Platform.getStateLocation(bundle);
        return dir.append("svnlog-" + encodeString(this.remoteUrl.toString()) + ".cache");
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public byte[] getFileContents(final String path, final IRepoRevision<?> revision) throws SVNException {
        return this.fileCache.getFileContents(path, ComparableWrapper.<Long> unwrap(revision.getId()));
    }

    @Override
    public IRepoRevision<ComparableWrapper<Long>> toRevision(final String revisionId) {
        try {
            return ChangestructureFactory.createRepoRevision(ComparableWrapper.wrap(Long.valueOf(revisionId)), this);
        } catch (final NumberFormatException e) {
            return null;
        }
    }

    @Override
    public IMutableFileHistoryGraph getFileHistoryGraph() {
        return this.fileHistoryGraph;
    }

    @Override
    public void setFileHistoryGraph(final IMutableFileHistoryGraph fileHistoryGraph) {
        this.fileHistoryGraph = fileHistoryGraph;
    }

    @Override
    public String toString() {
        return this.remoteUrl.toString();
    }

    @Override
    public void getLog(final long startRevision, final ISVNLogEntryHandler handler) throws SVNException {
        this.svnRepo.log(
                null,   // no target paths (retrieve log entries of whole repository)
                startRevision,
                this.svnRepo.getLatestRevision(),
                true,   // discover changed paths
                false,  // don't stop at copy operations
                0,      // no log limit
                false,  // don't include merge history
                new String[0],
                handler);
    }

    @Override
    public long getLatestRevision() throws SVNException {
        return this.svnRepo.getLatestRevision();
    }

    private static String encodeString(final String s) {
        return Base64.getUrlEncoder().encodeToString(s.getBytes());
    }

    private Object writeReplace() {
        return new SvnRepoRef(this.remoteUrl);
    }
}
