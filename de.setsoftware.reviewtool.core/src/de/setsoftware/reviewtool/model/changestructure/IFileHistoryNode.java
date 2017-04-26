package de.setsoftware.reviewtool.model.changestructure;

import java.util.Set;

/**
 * A node in a {@link IFileHistoryGraph}.
 * It is bound to a {@link FileInRevision} and knows at most one direct ancestor.
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
     * Returns the nearest ancestor {@link IFileHistoryNode}.
     * Note that the node returned by this operation may change over time when intermediate
     * {@link IFileHistoryNode}s are created due to recorded copy operations.
     */
    public abstract IFileHistoryEdge getAncestor();

    /**
     * Returns a list of descendant {@link IFileHistoryNode}s this node evolves to.
     */
    public abstract Set<? extends IFileHistoryNode> getDescendants();

    /**
     * Computes a combined {@link FileDiff} from passed history node to this one.
     */
    public abstract FileDiff buildHistory(final IFileHistoryNode from);
}
