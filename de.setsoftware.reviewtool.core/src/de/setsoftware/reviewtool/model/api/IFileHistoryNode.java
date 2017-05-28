package de.setsoftware.reviewtool.model.api;

import java.util.Set;

import de.setsoftware.reviewtool.model.changestructure.FileDiff;

/**
 * A node in a {@link IFileHistoryGraph}.
 * It is bound to a {@link IRevisionedFile} and knows its ancestors and descendants.
 */
public interface IFileHistoryNode {

    /**
     * Represents the type of an edge.
     */
    public enum Type {
        /**
         * A normal node denoting an addition or change, i.e. the start of a new or continuation of an existing flow.
         */
        NORMAL,
        /**
         * A node denoting a deletion, i.e. the termination of an existing flow.
         */
        DELETED,
        /**
         * A node denoting a replacement, i.e. the termination of an existing and the start of a new or continuation
         * of an existing flow (the latter happens when the node is replaced by a copy of some other node).
         */
        REPLACED,
        /**
         * A node denoting an unconfirmed copy source. "Unconfirmed" means that it is not clear which flow the node
         * belongs to.
         */
        UNCONFIRMED
    }

    /**
     * Returns the {@link IFileHistoryGraph} this node belongs to.
     */
    public abstract IFileHistoryGraph getGraph();

    /**
     * Returns the {@link IRevisionedFile} wrapped by this node.
     */
    public abstract IRevisionedFile getFile();

    /**
     * Checks whether this {@link IFileHistoryNode} is a root node, i.e. without an ancestor.
     * @return <code>true</code> if this node has no ancestor, else <code>false</code>
     */
    public abstract boolean isRoot();

    /**
     * Returns the type of this node.
     */
    public abstract Type getType();

    /**
     * Returns {@code true} if this is a confirmed node, i.e. iff {@code this.getType() != Type.UNCONFIRMED}.
     */
    public abstract boolean isConfirmed();

    /**
     * Checks whether this {@link IFileHistoryNode} denotes a copy target.
     * In case of a replaced node the copy target state refers to the replacing node in the new flow,
     * not the replaced one in the old flow.
     * @return <code>true</code> if this node is a copy target, else <code>false</code>
     */
    public abstract boolean isCopyTarget();

    /**
     * Returns the set of incoming edges originating from the nearest ancestor {@link IFileHistoryNode}s.
     * Note that the nodes returned by this operation may change over time when intermediate
     * {@link IFileHistoryNode}s are created due to recorded copy operations.
     */
    public abstract Set<? extends IFileHistoryEdge> getAncestors();

    /**
     * Returns the set of outgoing edges pointing to the the nearest descendant {@link IFileHistoryNode}s.
     * Note that the nodes returned by this operation may change over time when intermediate
     * {@link IFileHistoryNode}s are created due to recorded copy operations.
     */
    public abstract Set<? extends IFileHistoryEdge> getDescendants();

    /**
     * Computes {@link FileDiff}s from passed history node to this one. There may be none (if {@code from} is not an
     * ancestor), one (if {@code from} is reached by a single path backwards in history), or multiple ones
     * (if {@code from} can be reached by multiple paths backwards in history).
     */
    public abstract Set<? extends IFileDiff> buildHistories(IFileHistoryNode from);
}
