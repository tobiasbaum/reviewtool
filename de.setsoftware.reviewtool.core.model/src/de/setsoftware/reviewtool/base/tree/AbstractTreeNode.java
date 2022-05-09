package de.setsoftware.reviewtool.base.tree;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Implements the common parts of the {@link TreeNode} interface.
 *
 * @param <K> The type of the keys in the tree.
 * @param <V> The type of the values in the tree.
 * @param <SelfT> The concrete type of the tree node.
 */
public abstract class AbstractTreeNode<K, V, SelfT extends TreeNode<K, V, SelfT>>
        implements TreeNode<K, V, SelfT>, java.io.Serializable {

    private static final long serialVersionUID = 3535612955038411471L;
    private final SelfT parent;
    private V value;

    /**
     * Constructor. Sets the node's value to some user-supplied value.
     *
     * @param parent The parent of the node or {@code null} if this is the root node of the tree.
     * @param value The initial value of the node. May be {@code null}.
     */
    protected AbstractTreeNode(final SelfT parent, final V value) {
        this.parent = parent;
        this.value = value;
    }

    @Override
    public final SelfT getParent() {
        return this.parent;
    }

    @Override
    public final V getValue() {
        return this.value;
    }

    @Override
    public final V getValue(final K key) {
        final SelfT result = this.getNode(key);
        return result == null ? null : result.getValue();
    }

    @Override
    public final V getValue(final List<K> path) {
        final SelfT result = this.getNode(path);
        return result == null ? null : result.getValue();
    }

    @Override
    public final void setValue(final V value) {
        this.value = value;
    }

    @Override
    public final SelfT getNode(final List<K> path) {
        SelfT current = this.getSelf();
        final Iterator<K> it = path.iterator();
        while (it.hasNext()) {
            final K key = it.next();
            final SelfT child = current.getNode(key);
            if (child == null) {
                return null;
            } else {
                current = child;
            }
        }
        return current;
    }

    @Override
    public final SelfT putValue(final List<K> path, final V value) {
        SelfT current = this.getSelf();
        final Iterator<K> it = path.iterator();
        while (it.hasNext()) {
            final K key = it.next();
            SelfT child = current.getNode(key);
            if (child == null) {
                child = current.putValue(key, null);
            }
            current = child;
        }
        current.setValue(value);
        return current;
    }

    @Override
    public final List<V> getValues(final List<K> path) {
        final List<V> result = new LinkedList<>();
        SelfT current = this.getSelf();
        final Iterator<K> it = path.iterator();
        while (it.hasNext()) {
            final SelfT child = current.getNode(it.next());
            if (child == null) {
                break;
            } else {
                if (current.getValue() != null) {
                    result.add(0, current.getValue());
                }
                current = child;
            }
        }
        if (current.getValue() != null) {
            result.add(0, current.getValue());
        }
        return result;
    }

    @Override
    public final V getNearestValue(final List<K> path) {
        V lastKnownValue = null;
        SelfT current = this.getSelf();
        final Iterator<K> it = path.iterator();
        while (it.hasNext()) {
            final K key = it.next();
            if (current.getValue() != null) {
                lastKnownValue = current.getValue();
            }

            final SelfT child = current.getNode(key);
            if (child == null) {
                break;
            } else {
                current = child;
            }
        }
        return current.getValue() != null ? current.getValue() : lastKnownValue;
    }

    @Override
    public final int getHeight() {
        int maxChildHeight = 0;
        for (final Map.Entry<K, SelfT> entry : this.getEntries()) {
            final int childHeight = entry.getValue().getHeight();
            if (childHeight > maxChildHeight) {
                maxChildHeight = childHeight;
            }
        }
        return 1 + maxChildHeight;
    }

    @Override
    public boolean equals(final Object o) {
        if (o instanceof AbstractTreeNode<?, ?, ?>) {
            final AbstractTreeNode<?, ?, ?> other = (AbstractTreeNode<?, ?, ?>) o;
            return Objects.equals(this.value, other.value);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.value);
    }

    /**
     * Returns this object cast to SelfT.
     */
    protected abstract SelfT getSelf();
}
