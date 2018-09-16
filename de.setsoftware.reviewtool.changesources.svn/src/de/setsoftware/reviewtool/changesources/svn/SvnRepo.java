package de.setsoftware.reviewtool.changesources.svn;

import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.tmatesoft.svn.core.ISVNLogEntryHandler;
import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.core.io.ISVNReporter;
import org.tmatesoft.svn.core.io.ISVNReporterBaton;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.diff.SVNDiffWindow;
import org.tmatesoft.svn.core.wc.SVNRevision;

import de.setsoftware.reviewtool.base.ComparableWrapper;
import de.setsoftware.reviewtool.base.Logger;
import de.setsoftware.reviewtool.base.ValueWrapper;
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
     * Represents a versioned file in a Subversion repository.
     * Symbolic links are treated like files (even if they point to directories).
     */
    private static final class File implements ISvnRepo.File {
        private final String path;

        private File(final String path) {
            this.path = path;
        }

        @Override
        public String getName() {
            return this.path;
        }

        @Override
        public String toString() {
            return this.path;
        }
    }

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
    private final String relPath;
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

        final SVNURL repositoryRoot = svnRepo.getRepositoryRoot(true);
        if (!repositoryRoot.equals(remoteUrl)) {
            this.relPath = remoteUrl.toString().substring(repositoryRoot.toString().length());
        } else {
            this.relPath = "";
        }
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
    public Set<File> getFiles(final String path, final IRepoRevision<?> revision) {
        final Set<File> result = new LinkedHashSet<>();
        final long revisionNumber = ComparableWrapper.<Long> unwrap(revision.getId()).longValue();

        final ISVNReporterBaton reporter = new ISVNReporterBaton() {
            @Override
            public void report(final ISVNReporter reporter) throws SVNException {
                // simulate an empty working copy, such that ISVNEditor is called with all entries in the given commit
                reporter.setPath("", null, revisionNumber, SVNDepth.INFINITY, true);
                reporter.finishReport();
            }
        };

        final ISVNEditor editor = new ISVNEditor() {

            @Override
            public void abortEdit() {
            }

            @Override
            public void absentDir(final String path) {
            }

            @Override
            public void absentFile(final String path) {
            }

            @Override
            public void addFile(final String filePath, final String copyFromPath, final long copyFromRevision) {
                result.add(new File(path.isEmpty() ? filePath : path + "/" + filePath));
            }

            @Override
            public SVNCommitInfo closeEdit() {
                return null;
            }

            @Override
            public void closeFile(final String path, final String textChecksum) {
            }

            @Override
            public void deleteEntry(final String path, final long revision) {
            }

            @Override
            public void openFile(final String path, final long revision) {
            }

            @Override
            public void targetRevision(final long revision) {
            }

            @Override
            public void applyTextDelta(final String path, final String baseChecksum) {
            }

            @Override
            public OutputStream textDeltaChunk(final String path, final SVNDiffWindow diffWindow) {
                return null;
            }

            @Override
            public void textDeltaEnd(final String path) {
            }

            @Override
            public void addDir(final String path, final String copyFromPath, final long copyFromRevision) {
            }

            @Override
            public void changeDirProperty(final String name, final SVNPropertyValue value) {
            }

            @Override
            public void changeFileProperty(
                    final String path,
                    final String propertyName,
                    final SVNPropertyValue propertyValue) {
            }

            @Override
            public void closeDir() {
            }

            @Override
            public void openDir(final String path, final long revision) {
            }

            @Override
            public void openRoot(final long revision) {
            }
        };

        try {
            //
            // We have to use a temporary repository here for two reasons:
            // (1) The subversion protocol requires that the target path passed to the status() call below consist of
            //     at most one path component. (Note that an empty target path (or {@code null}, equivalently) is also
            //     allowed.) Paths with multiple components are not allowed and lead to server errors as follows:
            //       Provider encountered an error while streaming a REPORT response.  [404, #0]
            //       A failure occurred while driving the update report editor  [404, #160013]
            //       File not found: revision 43975, path '/pango/pango.modules'  [404, #160013]
            //     See the discussion of the topic "ra_dav and mod_dav_svn target problem" at the URL
            //     https://svn.haxx.se/dev/archive-2003-08/0184.shtml for details.
            // (2) While we process a commit in an ISVNLogEntryHandler, we are not allowed to call back the server
            //     using the same repository object as SVNRepository methods are not reentrant.
            //
            final SVNRepository repo = SvnRepositoryManager.getInstance().getTemporaryRepo(
                    this.svnRepo.getRepositoryRoot(false),
                    path);
            repo.status(revisionNumber, null, SVNDepth.INFINITY, reporter, editor);
        } catch (final SVNException e) {
            Logger.warn("Error while collecting files for directory " + path + "@" + revisionNumber, e);
        }
        return result;
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
        final ValueWrapper<Long> latestRevision = new ValueWrapper<>(SVNRevision.UNDEFINED.getNumber());
        final ISVNLogEntryHandler handler = new ISVNLogEntryHandler() {
            @Override
            public void handleLogEntry(final SVNLogEntry logEntry) {
                latestRevision.setValue(logEntry.getRevision());
            }
        };
        this.svnRepo.log(
                null,   // no target paths (retrieve log entries of whole repository)
                this.svnRepo.getLatestRevision(),
                0,
                false,  // don't discover changed paths
                true,   // stop at copy operations
                1,      // consider only the latest revision
                false,  // don't include merge history
                new String[0],
                handler);
        return latestRevision.get();
    }

    @Override
    public String getRelativePath() {
        return this.relPath;
    }

    private static String encodeString(final String s) {
        return Base64.getUrlEncoder().encodeToString(s.getBytes());
    }

    private Object writeReplace() {
        return new SvnRepoRef(this.remoteUrl);
    }
}
