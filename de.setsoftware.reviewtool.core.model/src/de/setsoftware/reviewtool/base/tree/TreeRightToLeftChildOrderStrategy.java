package de.setsoftware.reviewtool.base.tree;

import java.util.Collections;
import java.util.List;

import de.setsoftware.reviewtool.base.Pair;

/**
 * Implements right-to-left child node traversal.
 *
 * @param <K> The type of the keys in the tree.
 * @param <V> The type of the values in the tree.
 * @param <N> The concrete type of the tree node.
 */
public final class TreeRightToLeftChildOrderStrategy<K, V, N extends TreeNode<K, V, N>>
        implements TreeChildOrderStrategy<K, V, N> {

    @Override
    public void finalizeNodeList(final List<Pair<List<K>, N>> nodeList) {
        Collections.reverse(nodeList);
    }
}
