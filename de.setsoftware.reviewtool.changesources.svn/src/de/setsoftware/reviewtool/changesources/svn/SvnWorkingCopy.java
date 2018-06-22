package de.setsoftware.reviewtool.changesources.svn;

import java.io.File;
import java.nio.file.Path;

import de.setsoftware.reviewtool.model.changestructure.AbstractWorkingCopy;
import de.setsoftware.reviewtool.model.changestructure.VirtualFileHistoryGraph;

/**
 * Represents a local Subversion working copy.
 */
final class SvnWorkingCopy extends AbstractWorkingCopy {

    private final ISvnRepo repo;
    private final File workingCopyRoot;
    private final String relPath;
    private final VirtualFileHistoryGraph combinedFileHistoryGraph;

    SvnWorkingCopy(final ISvnRepo repo, final File workingCopyRoot, final String relPath) {
        this.repo = repo;
        this.workingCopyRoot = workingCopyRoot;
        this.relPath = relPath;
        this.combinedFileHistoryGraph = new VirtualFileHistoryGraph(repo.getFileHistoryGraph());
        this.setLocalFileHistoryGraph(new SvnFileHistoryGraph());
    }

    @Override
    public ISvnRepo getRepository() {
        return this.repo;
    }

    @Override
    public File getLocalRoot() {
        return this.workingCopyRoot;
    }

    @Override
    public String getRelativePath() {
        return this.relPath;
    }

    @Override
    public File toAbsolutePathInWc(final String absolutePathInRepo) {
        if (absolutePathInRepo.equals(this.relPath)) {
            return this.workingCopyRoot;
        } else if (absolutePathInRepo.startsWith(this.relPath + "/")) {
            assert !absolutePathInRepo.contains("\\");
            return new File(
                    this.workingCopyRoot,
                    absolutePathInRepo.substring(this.relPath.length() + 1));
        } else {
            return null;
        }
    }

    @Override
    public String toAbsolutePathInRepo(final File absolutePathInWc) {
        final Path wcRootPath = this.workingCopyRoot.toPath();
        final Path wcPath = absolutePathInWc.toPath();
        if (wcPath.startsWith(wcRootPath)) {
            try {
                final String relativePath = wcRootPath.relativize(wcPath).toString().replaceAll("\\\\", "/");
                return this.relPath + '/' + relativePath;
            } catch (final IllegalArgumentException e) {
                return null;
            }
        } else {
            return null;
        }
    }

    @Override
    public VirtualFileHistoryGraph getFileHistoryGraph() {
        return this.combinedFileHistoryGraph;
    }

    @Override
    public String toString() {
        return this.workingCopyRoot.toString();
    }

    /**
     * Returns the {@link SvnFileHistoryGraph local file history graph}. May be {@code null}.
     */
    SvnFileHistoryGraph getLocalFileHistoryGraph() {
        return (SvnFileHistoryGraph) this.combinedFileHistoryGraph.getLocalFileHistoryGraph();
    }

    /**
     * Replaces the {@link SvnFileHistoryGraph} by the passed file history graph.
     * Note that it is not possible to change the file history graph afterwards, as the combined file history graph
     * would not recompute the connecting edges.
     */
    void setLocalFileHistoryGraph(final SvnFileHistoryGraph localFileHistoryGraph) {
        this.combinedFileHistoryGraph.setLocalFileHistoryGraph(localFileHistoryGraph);
    }
}
