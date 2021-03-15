package de.setsoftware.reviewtool.base.tree;

import java.util.Iterator;
import java.util.List;

import de.setsoftware.reviewtool.base.Pair;

/**
 * Represents an iterator over a tree.
 *
 * @param <K> The type of the keys in the tree.
 * @param <V> The type of the values in the tree.
 * @param <N> The concrete type of the tree node.
 */
public interface TreeNodeIterator<K, V, N extends TreeNode<K, V, N>> extends Iterator<Pair<List<K>, N>> {

}
