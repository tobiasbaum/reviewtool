package de.setsoftware.reviewtool.model.changestructure;

import de.setsoftware.reviewtool.model.api.IFileDiff;
import de.setsoftware.reviewtool.model.api.IFileHistoryNode;

/**
 * An edge in a {@link VirtualFileHistoryGraph}.
 */
final class VirtualFileHistoryEdge extends AbstractFileHistoryEdge {

    private final VirtualFileHistoryGraph graph;
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
     * @param graph The {@link VirtualFileHistoryGraph} this edge belongs to.
     * @param ancestor The ancestor node of the edge.
     * @param descendant The descendant node of the edge.
     * @param type The type of the edge.
     * @param diff The associated {@link IFileDiff}.
     */
    public VirtualFileHistoryEdge(
            final VirtualFileHistoryGraph graph,
            final IFileHistoryNode ancestor,
            final IFileHistoryNode descendant,
            final Type type,
            final IFileDiff diff) {

        this.graph = graph;
        this.ancestor = ancestor;
        this.descendant = descendant;
        this.type = type;
        this.diff = diff;
    }

    @Override
    public VirtualFileHistoryGraph getGraph() {
        return this.graph;
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
