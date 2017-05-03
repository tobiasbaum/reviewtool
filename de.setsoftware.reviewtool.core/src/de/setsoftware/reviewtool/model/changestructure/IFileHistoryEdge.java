package de.setsoftware.reviewtool.model.changestructure;

/**
 * An edge in a {@link IFileHistoryGraph} between an ancestor and a descendant {@link IFileHistoryNode}.
 * It contains a {@link FileDiff}.
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
     * Returns the {@link FileDiff} between ancestor and descendant.
     */
    public abstract FileDiff getDiff();

}
