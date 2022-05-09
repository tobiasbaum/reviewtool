package de.setsoftware.reviewtool.base.tree;

/**
 * Tests {@link UnorderedTreeNode}.
 */
public final class OrderPreservingTreeNodeTest extends TreeNodeTest {

    @Override
    protected <K extends Comparable<K>, V> OrderPreservingTreeNode<K, V> createNode(final V value) {
        return OrderPreservingTreeNode.createRoot(value);
    }
}
