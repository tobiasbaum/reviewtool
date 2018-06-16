package de.setsoftware.reviewtool.changesources.svn;

import java.io.File;

import de.setsoftware.reviewtool.model.changestructure.AbstractWorkingCopy;
import de.setsoftware.reviewtool.model.changestructure.VirtualFileHistoryGraph;

/**
 * Represents a local Subversion working copy.
 */
final class SvnWorkingCopy extends AbstractWorkingCopy {

    private final SvnRepo repo;
    private final File workingCopyRoot;
    private final String relPath;
    private SvnFileHistoryGraph localFileHistoryGraph;
    private VirtualFileHistoryGraph combinedFileHistoryGraph;

    SvnWorkingCopy(final SvnRepo repo, final File workingCopyRoot, final String relPath) {
        this.repo = repo;
        this.workingCopyRoot = workingCopyRoot;
        this.relPath = relPath;
        this.localFileHistoryGraph = new SvnFileHistoryGraph();
        this.combinedFileHistoryGraph = new VirtualFileHistoryGraph(
                repo.getFileHistoryGraph(),
                this.localFileHistoryGraph);
    }

    @Override
    public SvnRepo getRepository() {
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
    public String toAbsolutePathInWc(String absolutePathInRepo) {
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
     * Returns the local file history graph.
     */
    SvnFileHistoryGraph getLocalFileHistoryGraph() {
        return this.localFileHistoryGraph;
    }

    /**
     * Replaces the {@link SvnFileHistoryGraph} by an empty file history graph.
     */
    void clearLocalFileHistoryGraph() {
        assert this.combinedFileHistoryGraph.size() > 0;
        this.combinedFileHistoryGraph.remove(this.combinedFileHistoryGraph.size() - 1);
        this.localFileHistoryGraph = new SvnFileHistoryGraph();
        this.combinedFileHistoryGraph.add(this.localFileHistoryGraph);
    }
}
