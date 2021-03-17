package de.setsoftware.reviewtool.base.tree;

/**
 * Tests {@link UnorderedTreeNode}.
 */
public final class UnorderedTreeNodeTest extends TreeNodeTest {

    @Override
    protected <K extends Comparable<K>, V> UnorderedTreeNode<K, V> createNode(final V value) {
        return UnorderedTreeNode.createRoot(value);
    }
}
