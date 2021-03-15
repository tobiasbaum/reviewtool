package de.setsoftware.reviewtool.model.changestructure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import de.setsoftware.reviewtool.base.ReviewtoolException;
import de.setsoftware.reviewtool.model.api.IFileDiff;
import de.setsoftware.reviewtool.model.api.IFileHistoryEdge;
import de.setsoftware.reviewtool.model.api.IFileHistoryNode;
import de.setsoftware.reviewtool.model.api.IFragment;
import de.setsoftware.reviewtool.model.api.IHunk;
import de.setsoftware.reviewtool.model.api.ILocalRevision;
import de.setsoftware.reviewtool.model.api.IRepoRevision;
import de.setsoftware.reviewtool.model.api.IRevisionVisitor;
import de.setsoftware.reviewtool.model.api.IRevisionedFile;
import de.setsoftware.reviewtool.model.api.IUnknownRevision;
import de.setsoftware.reviewtool.model.api.IncompatibleFragmentException;

/**
 * A node in a {@link VirtualFileHistoryGraph}.
 */
final class VirtualFileHistoryNode extends AbstractFileHistoryNode {

    private final VirtualFileHistoryGraph graph;
    private final IRevisionedFile file;
    private final List<IFileHistoryNode> nodes;
    private final List<IFileHistoryEdge> ancestors;
    private final List<IFileHistoryEdge> descendants;
    private final Type type;

    /**
     * Creates a virtual node from a non-empty list of underlying nodes.
     *
     * @param graph The {@link VirtualFileHistoryGraph} this node belongs to.
     * @param file The underlying {@link IRevisionedFile}.
     * @param nodes The underlying {@link IFileHistoryNode}s, all referring to {@code file} above.
     * @throws ReviewtoolException if the underlying node types are incompatible.
     */
    VirtualFileHistoryNode(
            final VirtualFileHistoryGraph graph,
            final IRevisionedFile file,
            final List<IFileHistoryNode> nodes) {

        assert !nodes.isEmpty();
        this.graph = graph;
        this.file = file;
        this.nodes = nodes;
        this.ancestors = new ArrayList<>();
        this.descendants = new ArrayList<>();
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
        Type resultingType = Type.UNCONFIRMED;
        for (final IFileHistoryNode node : this.nodes) {
            final Type nodeType = node.getType();
            if (resultingType.equals(Type.UNCONFIRMED)) {
                resultingType = nodeType;
            } else if (!nodeType.equals(Type.UNCONFIRMED) && !nodeType.equals(resultingType)) {
                throw new ReviewtoolException("Incompatible types for " + this.file + ": "
                        + resultingType + " # " + nodeType);
            }
        }
        return resultingType;
    }

    @Override
    public VirtualFileHistoryGraph getGraph() {
        return this.graph;
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
    public boolean isConfirmed() {
        return !this.type.equals(Type.UNCONFIRMED);
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
        final Set<IFileHistoryEdge> edges = new LinkedHashSet<>(this.ancestors);
        final Set<IFileHistoryEdge> alphaEdges = new LinkedHashSet<>();
        for (final IFileHistoryNode node : this.nodes) {
            for (final IFileHistoryEdge ancestorEdge : node.getAncestors()) {
                final IRevisionedFile ancestorFile = ancestorEdge.getAncestor().getFile();
                final VirtualFileHistoryEdge edge = new VirtualFileHistoryEdge(
                        this.graph,
                        this.graph.getNodeFor(ancestorFile),
                        this.graph.getNodeFor(ancestorEdge.getDescendant().getFile()),
                        ancestorEdge.getType(),
                        new IFileDiff() {

                            private static final long serialVersionUID = -4532395651797869193L;

                            @Override
                            public List<? extends IHunk> getHunks() {
                                return ancestorEdge.getDiff().getHunks();
                            }

                            @Override
                            public IRevisionedFile getFrom() {
                                return ancestorEdge.getDiff().getFrom();
                            }

                            @Override
                            public IRevisionedFile getTo() {
                                return ancestorEdge.getDiff().getTo();
                            }

                            @Override
                            public IFileDiff setTo(final IRevisionedFile newTo) {
                                return ancestorEdge.getDiff().setTo(newTo);
                            }

                            @Override
                            public IFragment traceFragment(final IFragment source) {
                                return ancestorEdge.getDiff().traceFragment(source);
                            }

                            @Override
                            public List<? extends IHunk> getHunksWithTargetChangesInOneOf(
                                    final Collection<? extends IFragment> fragments) {
                                return ancestorEdge.getDiff().getHunksWithTargetChangesInOneOf(fragments);
                            }

                            @Override
                            public IFileDiff merge(final IHunk hunkToMerge) throws IncompatibleFragmentException {
                                return ancestorEdge.getDiff().merge(hunkToMerge);
                            }

                            @Override
                            public IFileDiff merge(final Collection<? extends IHunk> hunksToMerge)
                                    throws IncompatibleFragmentException {
                                return ancestorEdge.getDiff().merge(hunksToMerge);
                            }

                            @Override
                            public IFileDiff merge(final IFileDiff diff) throws IncompatibleFragmentException {
                                return ancestorEdge.getDiff().merge(diff);
                            }
                        });

                ancestorFile.getRevision().accept(new IRevisionVisitor<Void>() {

                    @Override
                    public Void handleLocalRevision(final ILocalRevision revision) {
                        edges.add(edge);
                        return null;
                    }

                    @Override
                    public Void handleRepoRevision(final IRepoRevision<?> revision) {
                        edges.add(edge);
                        return null;
                    }

                    @Override
                    public Void handleUnknownRevision(final IUnknownRevision revision) {
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
        final Set<IFileHistoryEdge> edges = new LinkedHashSet<>(this.descendants);
        for (final IFileHistoryNode node : this.nodes) {
            for (final IFileHistoryEdge descendantEdge : node.getDescendants()) {
                edges.add(new VirtualFileHistoryEdge(
                        this.graph,
                        this.graph.getNodeFor(descendantEdge.getAncestor().getFile()),
                        this.graph.getNodeFor(descendantEdge.getDescendant().getFile()),
                        descendantEdge.getType(),
                        descendantEdge.getDiff()));
            }
        }
        return edges;
    }

    /**
     * Adds an ancestor node explicitly. This is necessary if a connection between two different revisions is needed.
     * @param ancestor The ancestor node to add.
     */
    void addAncestor(final IFileHistoryNode ancestor) {
        this.ancestors.add(new VirtualFileHistoryEdge(
                this.graph,
                ancestor,
                this,
                IFileHistoryEdge.Type.NORMAL,
                new FileDiff(ancestor.getFile(), this.getFile())));
    }

    /**
     * Adds a descendant node explicitly. This is necessary if a connection between two different revisions is needed.
     * @param descendant The descendant node to add.
     */
    void addDescendant(final IFileHistoryNode descendant) {
        this.descendants.add(new VirtualFileHistoryEdge(
                this.graph,
                this,
                descendant,
                IFileHistoryEdge.Type.NORMAL,
                new FileDiff(this.getFile(), descendant.getFile())));
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
