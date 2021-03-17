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
    public File toAbsolutePathInWc(final String absolutePathInRepo) {
        return new File(absolutePathInRepo);
    }

    @Override
    public String toAbsolutePathInRepo(final File absolutePathInWc) {
        return absolutePathInWc.toString();
    }

    @Override
    public IFileHistoryGraph getFileHistoryGraph() {
        return null;
    }
}
