package de.setsoftware.reviewtool.model.changestructure;

import java.util.Set;

/**
 * A node in a {@link FileHistoryGraph}.
 * It is bound to a {@link FileInRevision} and knows at most one direct ancestor.
 */
public interface FileHistoryNode {

    /**
     * Returns the {@link FileInRevision} wrapped by this node.
     */
    public abstract FileInRevision getFile();

    /**
     * Checks whether this {@link FileHistoryNode} is a root node, i.e. without an ancestor.
     * @return <code>true</code> if this node has no ancestor, else <code>false</code>
     */
    public abstract boolean isRoot();

    /**
     * Returns the nearest ancestor {@link FileHistoryNode}.
     * Note that the node returned by this operation may change over time when intermediate
     * {@link FileHistoryNode}s are created due to recorded copy operations.
     */
    public abstract FileHistoryEdge getAncestor();

    /**
     * Returns a list of descendant {@link FileHistoryNode}s this node evolves to.
     */
    public abstract Set<? extends FileHistoryNode> getDescendants();

    /**
     * Computes a combined {@link FileDiff} from passed history node to this one.
     */
    public abstract FileDiff buildHistory(final FileHistoryNode from);
}
