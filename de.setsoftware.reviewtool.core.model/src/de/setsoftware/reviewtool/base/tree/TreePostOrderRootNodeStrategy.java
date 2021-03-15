package de.setsoftware.reviewtool.base.tree;

import java.util.List;

import de.setsoftware.reviewtool.base.Pair;

/**
 * Implements post-order tree traversal.
 *
 * @param <K> The type of the keys in the tree.
 * @param <V> The type of the values in the tree.
 * @param <N> The concrete type of the tree node.
 */
public final class TreePostOrderRootNodeStrategy<K, V, N extends TreeNode<K, V, N>>
        implements TreeRootNodeStrategy<K, V, N> {

    @Override
    public void insertRootNode(
            final List<Pair<List<K>, N>> nodeList,
            final Pair<List<K>, N> rootEntry) {
        nodeList.add(rootEntry);
    }
}
