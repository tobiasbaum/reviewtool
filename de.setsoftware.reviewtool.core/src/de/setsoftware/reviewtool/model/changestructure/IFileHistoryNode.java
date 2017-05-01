package de.setsoftware.reviewtool.model.changestructure;

import java.util.Set;

/**
 * A node in a {@link IFileHistoryGraph}.
 * It is bound to a {@link FileInRevision} and knows its ancestors and descendants.
 */
public interface IFileHistoryNode {

    /**
     * Returns the {@link FileInRevision} wrapped by this node.
     */
    public abstract FileInRevision getFile();

    /**
     * Checks whether this {@link IFileHistoryNode} is a root node, i.e. without an ancestor.
     * @return <code>true</code> if this node has no ancestor, else <code>false</code>
     */
    public abstract boolean isRoot();

    /**
     * Checks whether this {@link IFileHistoryNode} denotes a deleted node.
     * @return <code>true</code> if this node is deleted, else <code>false</code>
     */
    public abstract boolean isDeleted();

    /**
     * Returns the set of outgoing edges pointing to the nearest ancestor {@link IFileHistoryNode}s.
     * Note that the nodes returned by this operation may change over time when intermediate
     * {@link IFileHistoryNode}s are created due to recorded copy operations.
     */
    public abstract Set<? extends IFileHistoryEdge> getAncestors();

    /**
     * Returns the set of incoming edges originating from the nearest descendant {@link IFileHistoryNode}s.
     * Note that the nodes returned by this operation may change over time when intermediate
     * {@link IFileHistoryNode}s are created due to recorded copy operations.
     */
    public abstract Set<? extends IFileHistoryEdge> getDescendants();

    /**
     * Computes {@link FileDiff}s from passed history node to this one. There may be none (if {code from} is not an
     * ancestor), one (if {@code from} is reached by a single path backwards in history), or multiple ones
     * (if {@code from} can be reached by multiple paths backwards in history).
     */
    public abstract Set<FileDiff> buildHistories(IFileHistoryNode from);
}
