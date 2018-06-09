package de.setsoftware.reviewtool.model.api;

/**
 * An edge in a {@link IFileHistoryGraph} between an ancestor and a descendant {@link IFileHistoryNode}.
 * An edge has a {@link Type type}. An edge contains a {@link IFileDiff}.
 */
public interface IFileHistoryEdge {

    /**
     * Represents the type of an edge.
     */
    public enum Type {
        /**
         * A normal edge connecting two nodes in history without a path change. The flow is terminated iff the
         * target node is either deleted or replaced.
         */
        NORMAL,
        /**
         * A copy edge connecting two nodes in history with a possible path change and without flow termination.
         */
        COPY,
        /**
         * A copy edge connecting two nodes in history with a possible path change and with flow termination.
         */
        COPY_DELETED
    }

    /**
     * Returns the {@link IFileHistoryGraph} this edge belongs to.
     */
    public abstract IFileHistoryGraph getGraph();

    /**
     * Returns the ancestor {@link IFileHistoryNode} of this edge.
     */
    public abstract IFileHistoryNode getAncestor();

    /**
     * Returns the descendant {@link IFileHistoryNode} of this edge.
     */
    public abstract IFileHistoryNode getDescendant();

    /**
     * Returns the type of this edge.
     */
    public abstract Type getType();

    /**
     * Returns the {@link IFileDiff} between ancestor and descendant.
     */
    public abstract IFileDiff getDiff();

}
