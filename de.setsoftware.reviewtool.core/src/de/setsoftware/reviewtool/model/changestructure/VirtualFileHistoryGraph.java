package de.setsoftware.reviewtool.model.changestructure;

import java.util.ArrayList;
import java.util.List;

import de.setsoftware.reviewtool.model.api.IDiffAlgorithm;
import de.setsoftware.reviewtool.model.api.IFileHistoryGraph;
import de.setsoftware.reviewtool.model.api.IFileHistoryNode;
import de.setsoftware.reviewtool.model.api.IRepository;
import de.setsoftware.reviewtool.model.api.IRevisionedFile;

/**
 * Merges two file history graphs (typically a remote and a local one) into one combined file history graph.
 * Note that the nodes returned are snapshots and do not change if the underlying file history graphs change afterwards.
 */
public final class VirtualFileHistoryGraph extends AbstractFileHistoryGraph {

    private final IFileHistoryGraph remoteFileHistoryGraph;
    private IFileHistoryGraph localFileHistoryGraph;

    public VirtualFileHistoryGraph(
            final IFileHistoryGraph remoteFileHistoryGraph,
            final IFileHistoryGraph localFileHistoryGraph) {

        this.remoteFileHistoryGraph = remoteFileHistoryGraph;
        this.localFileHistoryGraph = localFileHistoryGraph;
    }

    public void setLocalFileHistoryGraph(final IFileHistoryGraph localFileHistoryGraph) {
        this.localFileHistoryGraph = localFileHistoryGraph;
    }

    @Override
    public boolean contains(final String path, final IRepository repo) {
        return this.remoteFileHistoryGraph.contains(path, repo)
                || this.localFileHistoryGraph.contains(path, repo);
    }

    @Override
    public IFileHistoryNode getNodeFor(final IRevisionedFile file) {
        final List<IFileHistoryNode> nodes = new ArrayList<>();

        final IFileHistoryNode remoteNode = this.remoteFileHistoryGraph.getNodeFor(file);
        if (remoteNode != null) {
            nodes.add(remoteNode);
        }

        final IFileHistoryNode localNode = this.localFileHistoryGraph.getNodeFor(file);
        if (localNode != null) {
            nodes.add(localNode);
        }

        if (!nodes.isEmpty()) {
            final VirtualFileHistoryNode node = new VirtualFileHistoryNode(this, file, nodes);
            return node;
        } else {
            return null;
        }
    }

    /**
     * Returns the difference algorithm of the remote file history graph.
     */
    @Override
    public IDiffAlgorithm getDiffAlgorithm() {
        return this.remoteFileHistoryGraph.getDiffAlgorithm();
    }
}
