package de.setsoftware.reviewtool.changesources.svn;

import java.io.File;
import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.SVNClientManager;

import de.setsoftware.reviewtool.model.api.IFileHistoryGraph;
import de.setsoftware.reviewtool.model.api.IRepoRevision;
import de.setsoftware.reviewtool.model.api.IRevision;
import de.setsoftware.reviewtool.model.changestructure.AbstractRepository;
import de.setsoftware.reviewtool.model.changestructure.ChangestructureFactory;
import de.setsoftware.reviewtool.model.changestructure.VirtualFileHistoryGraph;

/**
 * Wraps the information needed on a SVN repository and corresponding working copy.
 */
final class SvnRepo extends AbstractRepository {

    /**
     * References a SVN repository by its working copy root.
     */
    private static final class SvnRepoRef implements Serializable {

        private static final long serialVersionUID = 8155878129812235537L;
        private final File wcRoot;

        /**
         * Constructor.
         * @param workingCopyRoot The root directory of the working copy.
         */
        SvnRepoRef(final File workingCopyRoot) {
            this.wcRoot = workingCopyRoot;
        }

        private Object readResolve() throws ObjectStreamException {
            final SvnRepo repo = CachedLog.getInstance().getRepositoryByWorkingCopyRoot(this.wcRoot);
            if (repo == null) {
                throw new InvalidObjectException("No repository for working copy at " + this.wcRoot);
            }
            return repo;
        }
    }

    private static final long serialVersionUID = 8792151363600093081L;

    private final String id;
    private final File workingCopyRoot;
    private final SVNURL remoteUrl;
    private final String relPath;
    private final int checkoutPrefix;
    private final SVNRepository svnRepo;
    private final SvnFileCache fileCache;
    private SvnFileHistoryGraph remoteHistoryGraph;
    private SvnFileHistoryGraph localHistoryGraph;
    private final VirtualFileHistoryGraph combinedHistoryGraph;

    SvnRepo(
            final SVNClientManager mgr,
            final String id,
            final File workingCopyRoot,
            final SVNURL rootUrl,
            final String relPath,
            final int checkoutPrefix,
            final SvnFileHistoryGraph remoteHistoryGraph) throws SVNException {

        this.id = id;
        this.workingCopyRoot = workingCopyRoot;
        this.remoteUrl = rootUrl;
        this.relPath = relPath + '/';
        this.checkoutPrefix = checkoutPrefix;
        this.svnRepo = mgr.createRepository(rootUrl, false);
        this.fileCache = new SvnFileCache(this.svnRepo);
        this.remoteHistoryGraph = remoteHistoryGraph == null ? new SvnFileHistoryGraph() : remoteHistoryGraph;
        this.localHistoryGraph = new SvnFileHistoryGraph();
        this.combinedHistoryGraph = new VirtualFileHistoryGraph(this.remoteHistoryGraph, this.localHistoryGraph);
    }

    SVNURL getRemoteUrl() {
        return this.remoteUrl;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public String toAbsolutePathInWc(String absolutePathInRepo) {
        assert absolutePathInRepo.startsWith("/");
        assert !absolutePathInRepo.contains("\\");

        final Path p = Paths.get(absolutePathInRepo);
        final File probableFile = this.combineWcRootAndSuffix(p, this.checkoutPrefix);
        if (probableFile.exists()) {
            return probableFile.toString();
        }

        //when the working copy has been switched to a branch, the checkout prefix might
        //  be wrong and we have to heuristically find the right path (until we have a better idea)
        for (int i = 0; i < p.getNameCount() - 1; i++) {
            final File f = this.combineWcRootAndSuffix(p, this.checkoutPrefix);
            if (f.exists()) {
                return f.toString();
            }
        }

        return probableFile.toString();

    }

    private File combineWcRootAndSuffix(final Path p, int prefixLength) {
        return new File(this.workingCopyRoot, p.subpath(prefixLength, p.getNameCount()).toString());
    }

    @Override
    public String fromAbsolutePathInWc(final String absolutePathInWc) {
        assert absolutePathInWc.startsWith(this.workingCopyRoot.getPath());
        final String path = new File(
                this.relPath,
                absolutePathInWc.toString().substring(this.workingCopyRoot.getPath().length()))
                    .getPath().replace('\\', '/');
        return path;
    }

    @Override
    public byte[] getFileContents(final String path, final IRepoRevision revision) throws SVNException {
        return this.fileCache.getFileContents(path, (Long) revision.getId());
    }

    @Override
    public IRevision getSmallestRevision(Collection<? extends IRevision> revisions) {
        return getSmallestOfComparableRevisions(revisions);
    }

    @Override
    public File getLocalRoot() {
        return this.workingCopyRoot;
    }

    @Override
    public IRepoRevision toRevision(final String revisionId) {
        try {
            return ChangestructureFactory.createRepoRevision(Long.valueOf(revisionId), this);
        } catch (final NumberFormatException e) {
            return null;
        }
    }

    @Override
    public IFileHistoryGraph getFileHistoryGraph() {
        return this.combinedHistoryGraph;
    }

    @Override
    public String toString() {
        return this.remoteUrl.toString();
    }

    /**
     * Returns the relative path of the working copy root wrt. the URL of the remote repository.
     * For example, if the remote repository's URL is https://example.com/svn/repo and the path "trunk/Workspace"
     * is checked out, then the relative path returned is "trunk/Workspace".
     */
    String getRelativePath() {
        return this.relPath;
    }

    /**
     * Returns the latest revision of this repository.
     */
    long getLatestRevision() throws SVNException {
        return this.svnRepo.getLatestRevision();
    }

    /**
     * Returns the {@link SvnFileHistoryGraph} describing the changes in the remote repository.
     */
    SvnFileHistoryGraph getRemoteFileHistoryGraph() {
        return this.remoteHistoryGraph;
    }

    /**
     * Returns the {@link SvnFileHistoryGraph} describing the changes in the local working copy.
     */
    SvnFileHistoryGraph getLocalFileHistoryGraph() {
        return this.localHistoryGraph;
    }

    /**
     * Replaces the {@link SvnFileHistoryGraph} of the local working copy by an empty file history graph.
     */
    void clearLocalFileHistoryGraph() {
        assert this.combinedHistoryGraph.size() > 0;
        this.combinedHistoryGraph.remove(this.combinedHistoryGraph.size() - 1);
        this.localHistoryGraph = new SvnFileHistoryGraph();
        this.combinedHistoryGraph.add(this.localHistoryGraph);
    }

    /**
     * Replaces the {@link SvnFileHistoryGraph} of the remote repository.
     * @param remoteFileHistoryGraph The new remote file history graph.
     */
    void setRemoteFileHistoryGraph(final SvnFileHistoryGraph remoteFileHistoryGraph) {
        this.remoteHistoryGraph = remoteFileHistoryGraph;
        this.combinedHistoryGraph.clear();
        this.combinedHistoryGraph.add(this.remoteHistoryGraph);
        this.combinedHistoryGraph.add(this.localHistoryGraph);
    }

    private Object writeReplace() {
        return new SvnRepoRef(this.workingCopyRoot);
    }
}
