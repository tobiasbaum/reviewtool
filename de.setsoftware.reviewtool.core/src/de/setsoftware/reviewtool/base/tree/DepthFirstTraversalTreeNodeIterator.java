package de.setsoftware.reviewtool.base.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import de.setsoftware.reviewtool.base.Pair;

/**
 * Performs a depth-first traversal over a tree.
 *
 * @param <K> The type of the keys in the tree.
 * @param <V> The type of the values in the tree.
 * @param <N> The concrete type of the tree node.
 */
public final class DepthFirstTraversalTreeNodeIterator<K, V, N extends TreeNode<K, V, N>>
        implements TreeNodeIterator<K, V, N> {

    private final N root;
    private final TreeChildOrderStrategy<K, V, N> childNodeStrategy;
    private final TreeRootNodeStrategy<K, V, N> rootNodeStrategy;
    private final Iterator<Pair<List<K>, N>> it;
    private Iterator<Pair<List<K>, N>> nextLevelIt;

    /**
     * Constructor. Uses the empty path for the root node.
     *
     * @param root The root of the tree to traverse.
     * @param rootNodeStrategy Specifies when the root node is visited during tree traversal.
     * @param childNodeStrategy Specifies in which order the child nodes are visited during tree traversal.
     */
    public DepthFirstTraversalTreeNodeIterator(
            final N root,
            final TreeRootNodeStrategy<K, V, N> rootNodeStrategy,
            final TreeChildOrderStrategy<K, V, N> childNodeStrategy) {

        this(root, Collections.<K>emptyList(), rootNodeStrategy, childNodeStrategy);
    }

    /**
     * Constructor.
     *
     * @param root The root of the tree to traverse.
     * @param rootPath The path to the root of the tree.
     * @param rootNodeStrategy Specifies when the root node is visited during tree traversal.
     * @param childNodeStrategy Specifies in which order the child nodes are visited during tree traversal.
     */
    public DepthFirstTraversalTreeNodeIterator(
            final N root,
            final List<K> rootPath,
            final TreeRootNodeStrategy<K, V, N> rootNodeStrategy,
            final TreeChildOrderStrategy<K, V, N> childNodeStrategy) {

        this.root = root;
        this.childNodeStrategy = childNodeStrategy;
        this.rootNodeStrategy = rootNodeStrategy;

        final List<Pair<List<K>, N>> entries = new ArrayList<>();
        for (final Map.Entry<K, N> entry : root.getEntries()) {
            final List<K> childPath = new ArrayList<>(rootPath);
            childPath.add(entry.getKey());
            entries.add(Pair.create(childPath, entry.getValue()));
        }
        childNodeStrategy.finalizeNodeList(entries);
        rootNodeStrategy.insertRootNode(entries, Pair.create(rootPath, root));
        this.it = entries.iterator();
    }

    @Override
    public boolean hasNext() {
        return this.nextLevelIt != null && this.nextLevelIt.hasNext() || this.it.hasNext();
    }

    @Override
    public Pair<List<K>, N> next() {
        assert this.hasNext();
        if (this.nextLevelIt == null || !this.nextLevelIt.hasNext()) {
            final Pair<List<K>, N> entry = this.it.next();
            if (entry.getSecond() == this.root) {
                return entry;
            } else {
                this.nextLevelIt = new DepthFirstTraversalTreeNodeIterator<>(
                        entry.getSecond(),
                        entry.getFirst(),
                        this.rootNodeStrategy,
                        this.childNodeStrategy);
            }
        }
        return this.nextLevelIt.next();
    }
}
