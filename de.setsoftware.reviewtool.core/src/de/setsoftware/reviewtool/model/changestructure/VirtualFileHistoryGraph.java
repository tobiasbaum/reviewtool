package de.setsoftware.reviewtool.model.changestructure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import de.setsoftware.reviewtool.base.ReviewtoolException;
import de.setsoftware.reviewtool.model.api.IFileDiff;
import de.setsoftware.reviewtool.model.api.IFileHistoryEdge;
import de.setsoftware.reviewtool.model.api.IFileHistoryGraph;
import de.setsoftware.reviewtool.model.api.IFileHistoryNode;
import de.setsoftware.reviewtool.model.api.ILocalRevision;
import de.setsoftware.reviewtool.model.api.IRepoRevision;
import de.setsoftware.reviewtool.model.api.IRepository;
import de.setsoftware.reviewtool.model.api.IRevisionVisitor;
import de.setsoftware.reviewtool.model.api.IRevisionedFile;
import de.setsoftware.reviewtool.model.api.IUnknownRevision;

/**
 * Merges multiple file history graphs into one virtual file history graph.
 * Note that the nodes returned are snapshots and do not change over time if underlying file history graphs
 * are added or removed.
 */
public final class VirtualFileHistoryGraph extends AbstractFileHistoryGraph {

    /**
     * A node in a {@link VirtualFileHistoryGraph}.
     */
    private final class VirtualFileHistoryNode extends AbstractFileHistoryNode {

        private final IRevisionedFile file;
        private final List<IFileHistoryNode> nodes;
        private final Type type;

        /**
         * Creates a virtual node from a non-empty list of underlying nodes.
         *
         * @param file The underlying {@link IRevisionedFile}.
         * @param nodes The underlying {@link IFileHistoryNode}s, all referrring to {@code file} above.
         * @throws ReviewtoolException if the underlying node types are incompatible.
         */
        VirtualFileHistoryNode(final IRevisionedFile file, final List<IFileHistoryNode> nodes) {
            assert !nodes.isEmpty();
            this.file = file;
            this.nodes = nodes;
            this.type = this.determineNodeType();
        }

        /**
         * Determines the type of this node from the types of the underlying nodes.
         * All underlying nodes types must conincide, with the exception of {@link Type#NORMAL} which may be overriden
         * by more specific types.
         *
         * @return The resulting node type.
         * @throws ReviewtoolException if the underlying node types are incompatible.
         */
        private Type determineNodeType() {
            Type resultingType = Type.NORMAL;
            for (final IFileHistoryNode node : this.nodes) {
                final Type nodeType = node.getType();
                if (!nodeType.equals(Type.NORMAL)) {
                    if (resultingType.equals(Type.NORMAL) || resultingType.equals(nodeType)) {
                        resultingType = nodeType;
                    } else {
                        throw new ReviewtoolException("Incompatible types for " + this.file + ": "
                                + resultingType + " # " + nodeType);
                    }
                }
            }
            return resultingType;
        }

        @Override
        public IRevisionedFile getFile() {
            return this.file;
        }

        @Override
        public boolean isRoot() {
            return this.getAncestors().isEmpty();
        }

        @Override
        public Type getType() {
            return this.type;
        }

        @Override
        public boolean isCopyTarget() {
            for (final IFileHistoryNode node : this.nodes) {
                if (node.isCopyTarget()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public Set<? extends IFileHistoryEdge> getAncestors() {
            final Set<IFileHistoryEdge> edges = new LinkedHashSet<>();
            final Set<IFileHistoryEdge> alphaEdges = new LinkedHashSet<>();
            for (final IFileHistoryNode node : this.nodes) {
                for (final IFileHistoryEdge ancestorEdge : node.getAncestors()) {
                    final IRevisionedFile ancestorFile = ancestorEdge.getAncestor().getFile();
                    final VirtualFileHistoryEdge edge = new VirtualFileHistoryEdge(
                            VirtualFileHistoryGraph.this.getNodeFor(ancestorFile),
                            VirtualFileHistoryGraph.this.getNodeFor(ancestorEdge.getDescendant().getFile()),
                            ancestorEdge.getType(),
                            ancestorEdge.getDiff());

                    ancestorFile.getRevision().accept(new IRevisionVisitor<Void>() {

                        @Override
                        public Void handleLocalRevision(ILocalRevision revision) {
                            edges.add(edge);
                            return null;
                        }

                        @Override
                        public Void handleRepoRevision(IRepoRevision revision) {
                            edges.add(edge);
                            return null;
                        }

                        @Override
                        public Void handleUnknownRevision(IUnknownRevision revision) {
                            alphaEdges.add(edge);
                            return null;
                        }
                    });
                }
            }

            return edges.isEmpty() ? alphaEdges : edges;
        }

        @Override
        public Set<? extends IFileHistoryEdge> getDescendants() {
            final Set<IFileHistoryEdge> edges = new LinkedHashSet<>();
            for (final IFileHistoryNode node : this.nodes) {
                for (final IFileHistoryEdge descendantEdge : node.getDescendants()) {
                    edges.add(new VirtualFileHistoryEdge(
                            VirtualFileHistoryGraph.this.getNodeFor(descendantEdge.getAncestor().getFile()),
                            VirtualFileHistoryGraph.this.getNodeFor(descendantEdge.getDescendant().getFile()),
                            descendantEdge.getType(),
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
        private final Type type;
        private final IFileDiff diff;

        /**
         * Creates an edge in a {@link VirtualFileHistoryGraph}. Unlike nodes, a {@link VirtualFileHistoryEdge}
         * does not know the underlying edge. This makes it possible to create virtual edges that do not have any
         * representation in the underlying file history graphs (e.g. edges between nodes of different underlying
         * file history graphs).
         *
         * @param ancestor The ancestor node of the edge.
         * @param descendant The descendant node of the edge.
         * @param type The type of the edge.
         * @param diff The associated {@link IFileDiff}.
         */
        public VirtualFileHistoryEdge(
                final IFileHistoryNode ancestor,
                final IFileHistoryNode descendant,
                final Type type,
                final IFileDiff diff) {
            this.ancestor = ancestor;
            this.descendant = descendant;
            this.type = type;
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
        public Type getType() {
            return this.type;
        }

        @Override
        public IFileDiff getDiff() {
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
            return new VirtualFileHistoryNode(file, nodes);
        } else {
            return null;
        }
    }

}
