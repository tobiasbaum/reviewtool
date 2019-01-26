package de.setsoftware.reviewtool.changesources.git;

import java.io.File;
import java.nio.file.Path;

import de.setsoftware.reviewtool.diffalgorithms.DiffAlgorithmFactory;
import de.setsoftware.reviewtool.model.api.IFileHistoryGraph;
import de.setsoftware.reviewtool.model.api.IMutableFileHistoryGraph;
import de.setsoftware.reviewtool.model.changestructure.AbstractWorkingCopy;
import de.setsoftware.reviewtool.model.changestructure.FileHistoryGraph;
import de.setsoftware.reviewtool.model.changestructure.VirtualFileHistoryGraph;

/**
 * Represents a local Git working copy.
 */
final class GitWorkingCopy extends AbstractWorkingCopy {

    private final GitRepository repository;
    private final File workingCopyRoot;
    private final VirtualFileHistoryGraph combinedFileHistoryGraph;

    /**
     * Constructor.
     */
    GitWorkingCopy(final File workingCopyRoot) {
        this.workingCopyRoot = workingCopyRoot;
        this.repository = new GitRepository(workingCopyRoot);
        this.combinedFileHistoryGraph = new VirtualFileHistoryGraph(this.repository.getFileHistoryGraph());
        this.setLocalFileHistoryGraph(new FileHistoryGraph(DiffAlgorithmFactory.createDefault()));
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
        return new File(
                this.workingCopyRoot,
                absolutePathInRepo);
    }

    @Override
    public String toAbsolutePathInRepo(File absolutePathInWc) {
        final Path wcRootPath = this.workingCopyRoot.toPath();
        final Path wcPath = absolutePathInWc.toPath();
        if (wcPath.startsWith(wcRootPath)) {
            try {
                final String relativePath = wcRootPath.relativize(wcPath).toString().replaceAll("\\\\", "/");
                return '/' + relativePath;
            } catch (final IllegalArgumentException e) {
                return null;
            }
        } else {
            return null;
        }
    }

    @Override
    public IFileHistoryGraph getFileHistoryGraph() {
        return this.combinedFileHistoryGraph;
    }

    /**
     * Replaces the local file history graph.
     */
    void setLocalFileHistoryGraph(final IMutableFileHistoryGraph localFileHistoryGraph) {
        this.combinedFileHistoryGraph.setLocalFileHistoryGraph(localFileHistoryGraph);
    }
}
