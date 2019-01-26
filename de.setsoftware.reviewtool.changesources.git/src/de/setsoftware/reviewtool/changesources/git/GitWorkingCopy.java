package de.setsoftware.reviewtool.changesources.git;

import java.io.File;

import de.setsoftware.reviewtool.model.api.IFileHistoryGraph;
import de.setsoftware.reviewtool.model.changestructure.AbstractWorkingCopy;

/**
 * Represents a local Git working copy.
 */
final class GitWorkingCopy extends AbstractWorkingCopy {

    private final GitRepository repository;
    private final File workingCopyRoot;

    /**
     * Constructor.
     */
    GitWorkingCopy(final File workingCopyRoot) {
        this.workingCopyRoot = workingCopyRoot;
        this.repository = new GitRepository(workingCopyRoot);
    }

    @Override
    public GitRepository getRepository() {
        return this.repository;
    }

    @Override
    public File getLocalRoot() {
        return this.workingCopyRoot;
    }

    @Override
    public File toAbsolutePathInWc(String absolutePathInRepo) {
        // TODO implement toAbsolutePathInWc()
        return null;
    }

    @Override
    public String toAbsolutePathInRepo(File absolutePathInWc) {
        // TODO implement toAbsolutePathInRepo()
        return null;
    }

    @Override
    public IFileHistoryGraph getFileHistoryGraph() {
        return this.repository.getFileHistoryGraph();
    }
}
