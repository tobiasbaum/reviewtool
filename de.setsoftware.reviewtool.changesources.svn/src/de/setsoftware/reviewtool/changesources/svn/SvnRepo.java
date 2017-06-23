package de.setsoftware.reviewtool.changesources.svn;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.SVNClientManager;

import de.setsoftware.reviewtool.model.api.IRepoRevision;
import de.setsoftware.reviewtool.model.api.IRevision;
import de.setsoftware.reviewtool.model.changestructure.AbstractRepository;
import de.setsoftware.reviewtool.model.changestructure.ChangestructureFactory;

/**
 * Wraps the information needed on a SVN repository and corresponding working copy.
 */
public class SvnRepo extends AbstractRepository {

    private final String id;
    private final File workingCopyRoot;
    private final SVNURL remoteUrl;
    private final String relPath;
    private final int checkoutPrefix;
    private final SVNRepository svnRepo;
    private final SvnFileCache fileCache;

    public SvnRepo(
            final SVNClientManager mgr,
            final String id,
            final File workingCopyRoot,
            final SVNURL rootUrl,
            final String relPath,
            final int checkoutPrefix) throws SVNException {
        this.id = id;
        this.workingCopyRoot = workingCopyRoot;
        this.remoteUrl = rootUrl;
        this.relPath = relPath + '/';
        this.checkoutPrefix = checkoutPrefix;
        this.svnRepo = mgr.getRepositoryPool().createRepository(rootUrl, false);
        this.fileCache = new SvnFileCache(this.svnRepo);
    }

    public SVNURL getRemoteUrl() {
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

    private File combineWcRootAndSuffix(final Path p, int prefixLength) {
        return new File(this.workingCopyRoot, p.subpath(prefixLength, p.getNameCount()).toString());
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
    public String toString() {
        return this.remoteUrl.toString();
    }
}
