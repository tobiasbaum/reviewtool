package de.setsoftware.reviewtool.changesources.svn;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.SVNClientManager;

import de.setsoftware.reviewtool.model.changestructure.RepoRevision;
import de.setsoftware.reviewtool.model.changestructure.Repository;
import de.setsoftware.reviewtool.model.changestructure.Revision;

/**
 * Wraps the information needed on a SVN repository and corresponding working copy.
 */
public class SvnRepo extends Repository {

    private final File workingCopyRoot;
    private final SVNURL remoteUrl;
    private final int checkoutPrefix;
    private final SvnFileCache fileCache;

    public SvnRepo(final SVNClientManager mgr, File workingCopyRoot, SVNURL rootUrl, int checkoutPrefix) {
        this.workingCopyRoot = workingCopyRoot;
        this.remoteUrl = rootUrl;
        this.checkoutPrefix = checkoutPrefix;
        this.fileCache = new SvnFileCache(mgr, this);
    }

    public SVNURL getRemoteUrl() {
        return this.remoteUrl;
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
    public byte[] getFileContents(final String path, final RepoRevision revision) throws SVNException {
        return this.fileCache.getFileContents(path, (Long) revision.getId());
    }

    private File combineWcRootAndSuffix(final Path p, int prefixLength) {
        return new File(this.workingCopyRoot, p.subpath(prefixLength, p.getNameCount()).toString());
    }

    @Override
    public Revision getSmallestRevision(Collection<? extends Revision> revisions) {
        return this.getSmallestOfComparableRevisions(revisions);
    }

    File getLocalRoot() {
        return this.workingCopyRoot;
    }

    @Override
    public String toString() {
        return this.remoteUrl.toString();
    }
}
