package de.setsoftware.reviewtool.base.tree;

import java.util.List;

import de.setsoftware.reviewtool.base.Pair;

/**
 * Specifies when the root node is visited during tree traversal.
 *
 * @param <K> The type of the keys in the tree.
 * @param <V> The type of the values in the tree.
 * @param <N> The concrete type of the tree node.
 */
public interface TreeRootNodeStrategy<K, V, N extends TreeNode<K, V, N>> {

    /**
     * Inserts the root node into the node list.
     *
     * @param nodeList The list of nodes the tree iterator will visit.
     * @param rootEntry The root entry to be inserted into the node list.
     */
    public abstract void insertRootNode(List<Pair<List<K>, N>> nodeList, Pair<List<K>, N> rootEntry);

}
