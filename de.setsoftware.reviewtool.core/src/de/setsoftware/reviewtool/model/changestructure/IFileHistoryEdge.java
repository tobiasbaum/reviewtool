package de.setsoftware.reviewtool.model.changestructure;

/**
 * An edge in a {@link IFileHistoryGraph}. It contains a {@link FileDiff}. It points from a descendant node to
 * an ancestor node.
 */
public interface IFileHistoryEdge {

    /**
     * Returns the ancestor {@link IFileHistoryNode} of this edge.
     */
    public abstract IFileHistoryNode getAncestor();

    /**
     * Returns the descendant {@link IFileHistoryNode} of this edge.
     */
    public abstract IFileHistoryNode getDescendant();

    /**
     * Returns the {@link FileDiff} bound to this edge.
     * <p/>
     * <em>Note:</em> Whether the {@link FileDiff} is to be interpreted in or against the direction of the edge
     * is determined by the context.
     */
    public abstract FileDiff getDiff();

}