package de.setsoftware.reviewtool.base.tree;

import java.util.List;
import java.util.function.Predicate;

import de.setsoftware.reviewtool.base.Pair;

/**
 * Implements a node iterator on the top of another one that returns only nodes fulfilling a predicate.
 *
 * @param <K> The type of the keys in the tree.
 * @param <V> The type of the values in the tree.
 * @param <N> The concrete type of the tree node.
 */
public final class FilteredTreeNodeIterator<K, V, N extends TreeNode<K, V, N>> implements TreeNodeIterator<K, V, N> {

    private final TreeNodeIterator<K, V, N> baseIt;
    private final Predicate<N> predicate;
    private Pair<List<K>, N> nextNode;

    /**
     * Constructor. Creates a tree node iterator that returns only nodes matching a given predicate.
     *
     * @param baseIt The base iterator.
     * @param predicate The predicate filtering tree nodes.
     */
    public FilteredTreeNodeIterator(final TreeNodeIterator<K, V, N> baseIt, final Predicate<N> predicate) {
        this.baseIt = baseIt;
        this.predicate = predicate;
        this.advance();
    }

    @Override
    public boolean hasNext() {
        return this.nextNode != null;
    }

    @Override
    public Pair<List<K>, N> next() {
        final Pair<List<K>, N> result = this.nextNode;
        this.advance();
        return result;
    }

    /**
     * Advances to next node that matches passed predicate.
     */
    private void advance() {
        while (this.baseIt.hasNext()) {
            this.nextNode = this.baseIt.next();
            if (this.predicate.test(this.nextNode.getSecond())) {
                return;
            }
        }
        this.nextNode = null;
    }
}
