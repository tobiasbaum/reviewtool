package de.setsoftware.reviewtool.model.changestructure;

import java.io.File;

import de.setsoftware.reviewtool.model.api.IFileHistoryGraph;
import de.setsoftware.reviewtool.model.api.IRepository;

/**
 * Implements {@link IWorkingCopy} for test cases.
 */
final class TestWorkingCopy extends AbstractWorkingCopy {

    private final TestRepository repo;
    private final File localRoot;

    TestWorkingCopy(final TestRepository repo, final File localRoot) {
        this.repo = repo;
        this.localRoot = localRoot;
    }

    @Override
    public IRepository getRepository() {
        return this.repo;
    }

    @Override
    public File getLocalRoot() {
        return this.localRoot;
    }

    @Override
    public String getRelativePath() {
        return "";
    }

    @Override
    public String toAbsolutePathInWc(String absolutePathInRepo) {
        return new File(this.localRoot, absolutePathInRepo).getAbsolutePath();
    }

    @Override
    public IFileHistoryGraph getFileHistoryGraph() {
        return null;
    }
}
