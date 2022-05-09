package de.setsoftware.reviewtool.base.tree;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * TreeNode implementation using unordered keys.
 *
 * @param <K> The type of the keys in the tree.
 * @param <V> The type of the values in the tree.
 */
public final class UnorderedTreeNode<K, V> extends AbstractTreeNode<K, V, UnorderedTreeNode<K, V>> {

    private static final long serialVersionUID = -1870841040919574919L;
    private final HashMap<K, UnorderedTreeNode<K, V>> children;

    /**
     * Constructor. Sets the node's value to some user-supplied value.
     *
     * @param parent The parent of the node or {@code null} if this is the root node of the tree.
     * @param value The initial value of the node. May be {@code null}
     */
    private UnorderedTreeNode(final UnorderedTreeNode<K, V> parent, final V value) {
        super(parent, value);
        this.children = new HashMap<>();
    }

    /**
     * Creates the root node of the tree.
     * @param value The initial value of the node. May be {@code null}.
     */
    public static <K, V> UnorderedTreeNode<K, V> createRoot(final V value) {
        return new UnorderedTreeNode<>(null, value);
    }

    @Override
    public UnorderedTreeNode<K, V> getNode(final K key) {
        return this.children.get(key);
    }

    @Override
    public UnorderedTreeNode<K, V> putValue(final K key, final V value) {
        UnorderedTreeNode<K, V> child = this.children.get(key);
        if (child == null) {
            child = new UnorderedTreeNode<>(this, value);
            this.children.put(key, child);
        } else {
            child.setValue(value);
        }
        return child;
    }

    @Override
    public Set<Map.Entry<K, UnorderedTreeNode<K, V>>> getEntries() {
        return Collections.unmodifiableSet(this.children.entrySet());
    }

    @Override
    public void removeNode(final UnorderedTreeNode<K, V> childNode) {
        this.children.values().remove(childNode);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof UnorderedTreeNode<?, ?>) {
            return super.equals(obj);
        }
        return false;
    }

    @Override
    protected UnorderedTreeNode<K, V> getSelf() {
        return this;
    }
}
