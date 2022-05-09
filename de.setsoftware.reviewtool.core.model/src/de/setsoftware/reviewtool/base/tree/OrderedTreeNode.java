package de.setsoftware.reviewtool.base.tree;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * TreeNode implementation using {@link Comparable} keys.
 *
 * @param <K> The type of the keys in the tree.
 * @param <V> The type of the values in the tree.
 */
public final class OrderedTreeNode<K extends Comparable<K>, V> extends AbstractTreeNode<K, V, OrderedTreeNode<K, V>> {

    private static final long serialVersionUID = 8089192927259795001L;
    private final TreeMap<K, OrderedTreeNode<K, V>> children;

    /**
     * Constructor. Sets the node's value to some user-supplied value.
     *
     * @param parent The parent of the node or {@code null} if this is the root node of the tree.
     * @param value The initial value of the node. May be {@code null}.
     */
    private OrderedTreeNode(final OrderedTreeNode<K, V> parent, final V value) {
        super(parent, value);
        this.children = new TreeMap<>();
    }

    /**
     * Creates the root node of the tree.
     * @param value The initial value of the node. May be {@code null}.
     */
    public static <K extends Comparable<K>, V> OrderedTreeNode<K, V> createRoot(final V value) {
        return new OrderedTreeNode<>(null, value);
    }

    @Override
    public OrderedTreeNode<K, V> getNode(final K key) {
        return this.children.get(key);
    }

    @Override
    public OrderedTreeNode<K, V> putValue(final K key, final V value) {
        OrderedTreeNode<K, V> child = this.children.get(key);
        if (child == null) {
            child = new OrderedTreeNode<>(this, value);
            this.children.put(key, child);
        } else {
            child.setValue(value);
        }
        return child;
    }

    @Override
    public Set<Map.Entry<K, OrderedTreeNode<K, V>>> getEntries() {
        return Collections.unmodifiableSet(this.children.entrySet());
    }

    @Override
    public void removeNode(final OrderedTreeNode<K, V> childNode) {
        this.children.values().remove(childNode);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof OrderedTreeNode<?, ?>) {
            return super.equals(obj);
        }
        return false;
    }

    @Override
    protected OrderedTreeNode<K, V> getSelf() {
        return this;
    }
}
