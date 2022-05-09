package de.setsoftware.reviewtool.base.tree;

/**
 * Tests {@link OrderedTreeNode}.
 */
public final class OrderedTreeNodeTest extends TreeNodeTest {

    @Override
    protected <K extends Comparable<K>, V> OrderedTreeNode<K, V> createNode(final V value) {
        return OrderedTreeNode.createRoot(value);
    }
}
