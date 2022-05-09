package de.setsoftware.reviewtool.base.tree;

import java.util.List;

import de.setsoftware.reviewtool.base.Pair;

/**
 * Specifies in which order the child nodes are visited during tree traversal.
 *
 * @param <K> The type of the keys in the tree.
 * @param <V> The type of the values in the tree.
 * @param <N> The concrete type of the tree node.
 */
public interface TreeChildOrderStrategy<K, V, N extends TreeNode<K, V, N>> {

    /**
     * Finalizes the order of the tree nodes to be visited by the tree iterator.
     *
     * @param nodeList The list of nodes the tree iterator will visit.
     */
    public abstract void finalizeNodeList(List<Pair<List<K>, N>> nodeList);
}
