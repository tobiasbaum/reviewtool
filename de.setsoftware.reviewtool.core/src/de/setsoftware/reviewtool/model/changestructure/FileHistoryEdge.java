package de.setsoftware.reviewtool.model.changestructure;

import de.setsoftware.reviewtool.model.api.IFileDiff;
import de.setsoftware.reviewtool.model.api.IMutableFileHistoryEdge;

/**
 * Implementation of a {@link IMutableFileHistoryEdge}.
 */
public final class FileHistoryEdge extends AbstractFileHistoryEdge implements IMutableFileHistoryEdge {

    private final FileHistoryGraph graph;
    private final FileHistoryNode ancestor;
    private final FileHistoryNode descendant;
    private Type type;
    private IFileDiff diff;

    /**
     * Constructor.
     * @param ancestor The ancestor node of the edge.
     * @param descendant The descendant node of the edge.
     * @param type The type of the edge.
     * @param diff The associated {@link IFileDiff}. It can be changed later using {@link #setDiff(IFileDiff)}.
     */
    public FileHistoryEdge(
            final FileHistoryGraph graph,
            final FileHistoryNode ancestor,
            final FileHistoryNode descendant,
            final Type type,
            final IFileDiff diff) {
        this.graph = graph;
        this.ancestor = ancestor;
        this.descendant = descendant;
        this.type = type;
        this.diff = diff;
    }

    @Override
    public FileHistoryGraph getGraph() {
        return this.graph;
    }

    @Override
    public FileHistoryNode getAncestor() {
        return this.ancestor;
    }

    @Override
    public FileHistoryNode getDescendant() {
        return this.descendant;
    }

    @Override
    public Type getType() {
        return this.type;
    }

    /**
     * Changes the type of the edge. This is used to replace a {@link Type#COPY} edge by a {@link Type#COPY_DELETED}
     * edge when a flow is terminated in a copy target.
     * @param type The new type.
     */
    void setType(final Type type) {
        this.type = type;
    }

    @Override
    public IFileDiff getDiff() {
        return this.diff;
    }

    @Override
    public void setDiff(final IFileDiff diff) {
        this.diff = diff;
    }

    @Override
    public boolean equals(final Object o) {
        if (o instanceof FileHistoryEdge) {
            final FileHistoryEdge other = (FileHistoryEdge) o;
            return this.ancestor.equals(other.ancestor)
                    && this.descendant.equals(other.descendant)
                    && this.type.equals(other.type);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.ancestor.hashCode() ^ this.descendant.hashCode();
    }
}
