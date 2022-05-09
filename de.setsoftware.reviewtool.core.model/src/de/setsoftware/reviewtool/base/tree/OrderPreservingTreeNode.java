package de.setsoftware.reviewtool.base.tree;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * TreeNode implementation using unordered keys but preserving insertion order.
 *
 * @param <K> The type of the keys in the tree.
 * @param <V> The type of the values in the tree.
 */
public final class OrderPreservingTreeNode<K extends Comparable<K>, V> extends
        AbstractTreeNode<K, V, OrderPreservingTreeNode<K, V>> {

    private static final long serialVersionUID = 168352569852009515L;
    private final LinkedHashMap<K, OrderPreservingTreeNode<K, V>> children;

    /**
     * Constructor. Sets the node's value to some user-supplied value.
     *
     * @param parent The parent of the node or {@code null} if this is the root node of the tree.
     * @param value The initial value of the node. May be {@code null}.
     */
    private OrderPreservingTreeNode(final OrderPreservingTreeNode<K, V> parent, final V value) {
        super(parent, value);
        this.children = new LinkedHashMap<>();
    }

    /**
     * Creates the root node of the tree.
     * @param value The initial value of the node. May be {@code null}.
     */
    public static <K extends Comparable<K>, V> OrderPreservingTreeNode<K, V> createRoot(final V value) {
        return new OrderPreservingTreeNode<>(null, value);
    }

    @Override
    public OrderPreservingTreeNode<K, V> getNode(final K key) {
        return this.children.get(key);
    }

    @Override
    public OrderPreservingTreeNode<K, V> putValue(final K key, final V value) {
        OrderPreservingTreeNode<K, V> child = this.children.get(key);
        if (child == null) {
            child = new OrderPreservingTreeNode<>(this, value);
            this.children.put(key, child);
        } else {
            child.setValue(value);
        }
        return child;
    }

    @Override
    public Set<Map.Entry<K, OrderPreservingTreeNode<K, V>>> getEntries() {
        return Collections.unmodifiableSet(this.children.entrySet());
    }

    @Override
    public void removeNode(final OrderPreservingTreeNode<K, V> childNode) {
        this.children.values().remove(childNode);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof OrderPreservingTreeNode<?, ?>) {
            return super.equals(obj);
        }
        return false;
    }

    @Override
    protected OrderPreservingTreeNode<K, V> getSelf() {
        return this;
    }
}
