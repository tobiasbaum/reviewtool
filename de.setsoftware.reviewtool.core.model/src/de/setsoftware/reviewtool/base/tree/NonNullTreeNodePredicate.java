package de.setsoftware.reviewtool.base.tree;

import java.util.function.Predicate;

/**
 * Matches tree nodes with a non-null value.
 *
 * @param <K> The type of the keys in the tree.
 * @param <V> The type of the values in the tree.
 * @param <N> The concrete type of the tree node.
 */
public final class NonNullTreeNodePredicate<K, V, N extends TreeNode<K, V, N>> implements Predicate<N> {

    @Override
    public boolean test(final N node) {
        return node.getValue() != null;
    }
}
