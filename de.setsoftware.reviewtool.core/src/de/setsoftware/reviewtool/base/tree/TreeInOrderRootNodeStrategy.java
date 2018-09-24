package de.setsoftware.reviewtool.base.tree;

import java.util.List;

import de.setsoftware.reviewtool.base.Pair;

/**
 * Implements in-order tree traversal.
 *
 * @param <K> The type of the keys in the tree.
 * @param <V> The type of the values in the tree.
 * @param <N> The concrete type of the tree node.
 */
public final class TreeInOrderRootNodeStrategy<K, V, N extends TreeNode<K, V, N>>
        implements TreeRootNodeStrategy<K, V, N> {

    private final int index;

    /**
     * Constructor.
     *
     * @param index The index at which to insert the root node.
     */
    public TreeInOrderRootNodeStrategy(final int index) {
        this.index = index;
    }

    @Override
    public void insertRootNode(final List<Pair<List<K>, N>> nodeList,
            final Pair<List<K>, N> rootEntry) {
        nodeList.add(this.index, rootEntry);
    }
}
