package de.setsoftware.reviewtool.changesources.svn;

import java.io.File;

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
    public String toAbsolutePathInWc(final String absolutePathInRepo) {
        if (absolutePathInRepo.equals(this.relPath)) {
            return this.workingCopyRoot.toString();
        } else if (absolutePathInRepo.startsWith(this.relPath + "/")) {
            assert !absolutePathInRepo.contains("\\");
            return new File(
                    this.workingCopyRoot,
                    absolutePathInRepo.substring(this.relPath.length() + 1)).toString();
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
     * Replaces the {@link SvnFileHistoryGraph} by the passed file history graph.
     * Note that it is not possible to change the file history graph afterwards, as the combined file history graph
     * would not recompute the connecting edges.
     */
    void setLocalFileHistoryGraph(final SvnFileHistoryGraph localFileHistoryGraph) {
        this.combinedFileHistoryGraph.setLocalFileHistoryGraph(localFileHistoryGraph);
    }
}
