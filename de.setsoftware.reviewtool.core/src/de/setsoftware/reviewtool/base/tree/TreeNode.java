package de.setsoftware.reviewtool.base.tree;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A tree (node) represented as a map of maps. The number of children of a node is not fixed. Each node is the potential
 * root node of a (sub)tree.
 *
 * @param <K> The type of the keys in the tree.
 * @param <V> The type of the values in the tree.
 * @param <SelfT> The concrete type of the tree node.
 */
public interface TreeNode<K, V, SelfT extends TreeNode<K, V, SelfT>> {

    /**
     * Returns the parent node or {@code null} if this is the root node of the tree.
     */
    public abstract SelfT getParent();

    /**
     * Returns the value of this node.
     *
     * @return The value of this node or {@code null} if not set.
     */
    public abstract V getValue();

    /**
     * Returns the value of a direct child node given its key.
     * Note that the caller cannot distinguish a missing node from an existing node without a value.
     *
     * @param key The key.
     * @return The node's value or {@code null} if no node exists for the given key.
     */
    public abstract V getValue(final K key);

    /**
     * Returns the value of a node given a path of keys. If the path is empty, {@code this.getValue()} is returned.
     * Note that the caller cannot distinguish a missing node from an existing node without a value.
     *
     * @param path The path of keys.
     * @return The node's value or {@code null} if no node can be reached using the given path.
     */
    public abstract V getValue(final List<K> path);

    /**
     * Sets the value of this node.
     *
     * @param value The new value of this node.
     */
    public abstract void setValue(V value);

    /**
     * Returns a direct child node given its key.
     *
     * @param key The key.
     * @return The node or {@code null} if no node exists for the given key.
     */
    public abstract SelfT getNode(K key);

    /**
     * Returns a node given a path of keys. If the path is empty, {@code this} is returned.
     *
     * @param path The path of keys.
     * @return The node or {@code null} if no node can be reached using the given path.
     */
    public abstract SelfT getNode(List<K> path);

    /**
     * Sets the value of a direct child node given its key. If the node does not exist yet, it is created.
     *
     * @param key The key.
     * @param value The value to put. May be {@code null}.
     * @return The child node.
     */
    public abstract SelfT putValue(K key, V value);

    /**
     * Sets the value of the node at the position specified by a path of keys. The node and all intermediate nodes
     * are created if necessary (the latter with a {@code null} value). The empty path is allowed.
     *
     * @param path The path of keys.
     * @param value The value to put.
     * @return The node having its value set.
     */
    public abstract SelfT putValue(List<K> path, V value);

    /**
     * Returns all direct children of this node together with their keys. The set returned is unmodifiable.
     */
    public abstract Set<Map.Entry<K, SelfT>> getEntries();

    /**
     * Returns all non-{@code null} values on the way from the selected node to the root, given a path of keys.
     *
     * @param path The path of keys.
     * @return The non-{@code null} values on the way from the selected node to the root. The list is empty iff no node
     *         on the path has a non-{@code null} value.
     */
    public abstract List<V> getValues(List<K> path);

    /**
     * Returns the last non-null value on a node path. If the path is empty, {@code this.getValue()} is returned.
     * The tree traversal stops at the first non-existing node.
     *
     * @param path The path of keys.
     * @return The value or {@code null} if there are no non-{@code null} values on the path.
     */
    public abstract V getNearestValue(List<K> path);

    /**
     * Computes the height of the tree starting at this node.
     *
     * @return The tree height.
     */
    public abstract int getHeight();

    /**
     * Removes a child node from this node.
     * @param childNode The child node to delete.
     */
    public abstract void removeNode(final SelfT childNode);
}
