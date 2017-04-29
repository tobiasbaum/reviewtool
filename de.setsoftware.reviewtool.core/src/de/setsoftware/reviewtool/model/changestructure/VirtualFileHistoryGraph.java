package de.setsoftware.reviewtool.model.changestructure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Merges multiple file history graphs into one virtual file history graph.
 */
public final class VirtualFileHistoryGraph extends AbstractFileHistoryGraph {

    /**
     * A node in a {@link VirtualFileHistoryGraph}.
     */
    private final class VirtualFileHistoryNode extends AbstractFileHistoryNode {

        private final FileInRevision file;
        private final Set<IFileHistoryNode> nodes;

        VirtualFileHistoryNode(final FileInRevision file, final Set<IFileHistoryNode> nodes) {
            this.file = file;
            this.nodes = nodes;
        }

        @Override
        public FileInRevision getFile() {
            return this.file;
        }

        @Override
        public boolean isRoot() {
            return this.getAncestors().isEmpty();
        }

        @Override
        public boolean isDeleted() {
            for (final IFileHistoryNode node : this.nodes) {
                if (node.isDeleted()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public Set<? extends IFileHistoryEdge> getAncestors() {
            final Set<IFileHistoryEdge> edges = new LinkedHashSet<>();
            for (final IFileHistoryNode node : this.nodes) {
                for (final IFileHistoryEdge ancestorEdge : node.getAncestors()) {
                    edges.add(new VirtualFileHistoryEdge(
                            VirtualFileHistoryGraph.this.getNodeFor(ancestorEdge.getAncestor().getFile()),
                            VirtualFileHistoryGraph.this.getNodeFor(ancestorEdge.getDescendant().getFile()),
                            ancestorEdge.getDiff()));
                }
            }
            return edges;
        }

        @Override
        public Set<? extends IFileHistoryEdge> getDescendants() {
            final Set<IFileHistoryEdge> edges = new LinkedHashSet<>();
            for (final IFileHistoryNode node : this.nodes) {
                for (final IFileHistoryEdge descendantEdge : node.getDescendants()) {
                    edges.add(new VirtualFileHistoryEdge(
                            VirtualFileHistoryGraph.this.getNodeFor(descendantEdge.getAncestor().getFile()),
                            VirtualFileHistoryGraph.this.getNodeFor(descendantEdge.getDescendant().getFile()),
                            descendantEdge.getDiff()));
                }
            }
            return edges;
        }

        @Override
        public boolean equals(final Object o) {
            if (o instanceof VirtualFileHistoryNode) {
                final VirtualFileHistoryNode other = (VirtualFileHistoryNode) o;
                return this.file.equals(other.getFile());
            }
            return false;
        }

        @Override
        public int hashCode() {
            return this.file.hashCode();
        }
    }

    /**
     * An edge in a {@link VirtualFileHistoryGraph}.
     */
    private final class VirtualFileHistoryEdge implements IFileHistoryEdge {

        private final IFileHistoryNode ancestor;
        private final IFileHistoryNode descendant;
        private FileDiff diff;

        /**
         * Constructor.
         * @param ancestor The ancestor node of the edge.
         * @param descendant The descendant node of the edge.
         * @param diff The associated {@link FileDiff} object. It can be changed later using {@link #setDiff(FileDiff)}.
         */
        public VirtualFileHistoryEdge(
                final IFileHistoryNode ancestor,
                final IFileHistoryNode descendant,
                final FileDiff diff) {
            this.ancestor = ancestor;
            this.descendant = descendant;
            this.diff = diff;
        }

        @Override
        public IFileHistoryNode getAncestor() {
            return this.ancestor;
        }

        @Override
        public IFileHistoryNode getDescendant() {
            return this.descendant;
        }

        @Override
        public FileDiff getDiff() {
            return this.diff;
        }

        @Override
        public boolean equals(final Object o) {
            if (o instanceof VirtualFileHistoryEdge) {
                final VirtualFileHistoryEdge other = (VirtualFileHistoryEdge) o;
                return this.ancestor.equals(other.getAncestor())
                        && this.descendant.equals(other.getDescendant());
            }
            return false;
        }

        @Override
        public int hashCode() {
            return this.ancestor.hashCode() ^ this.descendant.hashCode();
        }
    }

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

    @Override
    public boolean contains(final String path, final Repository repo) {
        for (final IFileHistoryGraph graph : this.graphs) {
            if (graph.contains(path, repo)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public IFileHistoryNode getNodeFor(final FileInRevision file) {
        final Set<IFileHistoryNode> nodes = new LinkedHashSet<>();
        for (final IFileHistoryGraph graph : this.graphs) {
            final IFileHistoryNode node = graph.getNodeFor(file);
            if (node != null) {
                nodes.add(node);
            }
        }

        if (!nodes.isEmpty()) {
            return new VirtualFileHistoryNode(file, nodes);
        } else {
            return null;
        }
    }

}
