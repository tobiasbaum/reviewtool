package de.setsoftware.reviewtool.changesources.svn;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.tmatesoft.svn.core.SVNURL;

import de.setsoftware.reviewtool.model.changestructure.Repository;

/**
 * Wraps the information needed on a SVN repository and corresponding working copy.
 */
public class SvnRepo extends Repository {

    private final File workingCopyRoot;
    private final SVNURL remoteUrl;
    private final int checkoutPrefix;

    public SvnRepo(File workingCopyRoot, SVNURL rootUrl, int checkoutPrefix) {
        this.workingCopyRoot = workingCopyRoot;
        this.remoteUrl = rootUrl;
        this.checkoutPrefix = checkoutPrefix;
    }

    public SVNURL getRemoteUrl() {
        return this.remoteUrl;
    }

    @Override
    public String toAbsolutePathInWc(String absolutePathInRepo) {
        assert absolutePathInRepo.startsWith("/");
        assert !absolutePathInRepo.contains("\\");

        final Path p = Paths.get(absolutePathInRepo);
        return new File(this.workingCopyRoot, p.subpath(this.checkoutPrefix, p.getNameCount()).toString()).toString();
    }

}
