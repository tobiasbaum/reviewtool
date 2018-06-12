package de.setsoftware.reviewtool.model.changestructure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.setsoftware.reviewtool.model.api.IDiffAlgorithm;
import de.setsoftware.reviewtool.model.api.IFileHistoryGraph;
import de.setsoftware.reviewtool.model.api.IFileHistoryNode;
import de.setsoftware.reviewtool.model.api.IRepository;
import de.setsoftware.reviewtool.model.api.IRevisionedFile;

/**
 * Merges multiple file history graphs into one virtual file history graph.
 * Note that the nodes returned are snapshots and do not change over time if underlying file history graphs
 * are added or removed.
 */
public final class VirtualFileHistoryGraph extends AbstractFileHistoryGraph {

    private List<IFileHistoryGraph> graphs;

    public VirtualFileHistoryGraph(final IFileHistoryGraph ...graphs) {
        this.graphs = new ArrayList<>(Arrays.asList(graphs));
    }

    public int size() {
        return this.graphs.size();
    }

    public void add(final IFileHistoryGraph graph) {
        this.graphs.add(graph);
    }

    public void remove(final int index) {
        this.graphs.remove(index);
    }

    public void clear() {
        this.graphs.clear();
    }

    @Override
    public boolean contains(final String path, final IRepository repo) {
        for (final IFileHistoryGraph graph : this.graphs) {
            if (graph.contains(path, repo)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public IFileHistoryNode getNodeFor(final IRevisionedFile file) {
        final List<IFileHistoryNode> nodes = new ArrayList<>();
        for (final IFileHistoryGraph graph : this.graphs) {
            final IFileHistoryNode node = graph.getNodeFor(file);
            if (node != null) {
                nodes.add(node);
            }
        }

        if (!nodes.isEmpty()) {
            return new VirtualFileHistoryNode(this, file, nodes);
        } else {
            return null;
        }
    }

    /**
     * Returns the difference algorithm of the first graph being part of this virtual graph.
     * If no such graph exists, {@code null} is returned.
     */
    @Override
    public IDiffAlgorithm getDiffAlgorithm() {
        return this.graphs.isEmpty() ? null : this.graphs.get(0).getDiffAlgorithm();
    }
}
