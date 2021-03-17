package de.setsoftware.reviewtool.base.tree;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests {@link OrderedTreeNode}.
 */
public abstract class TreeNodeTest {

    private TreeNode<String, Integer, ?> root;
    private TreeNode<String, Integer, ?> childA;
    private TreeNode<String, Integer, ?> childB;
    private TreeNode<String, Integer, ?> childAA;
    private TreeNode<String, Integer, ?> childBB;

    /**
     * Creates a node with an initial value.
     * @param value The initial value.
     */
    protected abstract <K extends Comparable<K>, V> TreeNode<K, V, ?> createNode(V value);

    /**
     * Creates an initial tree.
     */
    @Before
    public void setUp() {
        this.root = this.createNode(null);
        this.childA = this.root.putValue("a", 1);
        this.childB = this.root.putValue("b", 2);
        this.childAA = this.childA.putValue("aa", 11);
        this.childBB = this.childB.putValue("bb", 22);
    }

    /**
     * Tests {@link TreeNode#getParent()}.
     */
    @Test
    public void testGetParent() {
        assertSame(this.root, this.childA.getParent());
        assertSame(this.root, this.childB.getParent());
        assertSame(this.childA, this.childAA.getParent());
        assertSame(this.childB, this.childBB.getParent());
    }

    /**
     * Tests {@link TreeNode#getValue()}.
     */
    @Test
    public void testGetValue() {
        assertNull(this.root.getValue());
        assertSame(Integer.valueOf(1), this.childA.getValue());
        assertSame(Integer.valueOf(2), this.childB.getValue());
        assertSame(Integer.valueOf(11), this.childAA.getValue());
        assertSame(Integer.valueOf(22), this.childBB.getValue());
    }

    /**
     * Tests {@link TreeNode#getValue(Object)}.
     */
    @Test
    public void testGetValueByKey() {
        assertNull(this.root.getValue());
        assertSame(Integer.valueOf(1), this.root.getValue("a"));
        assertSame(Integer.valueOf(2), this.root.getValue("b"));
        assertSame(Integer.valueOf(11), this.childA.getValue("aa"));
        assertSame(Integer.valueOf(22), this.childB.getValue("bb"));
        assertNull(this.root.getValue("c"));
    }

    /**
     * Tests {@link TreeNode#getValue(java.util.List)}.
     */
    @Test
    public void testGetValueByPath() {
        final TreeNode<String, Integer, ?> newRoot = this.createNode(null);
        final TreeNode<String, Integer, ?> newChildA = newRoot.putValue("a", null);
        final TreeNode<String, Integer, ?> newChildB = newRoot.putValue("b", 2);
        final TreeNode<String, Integer, ?> newChildAA = newChildA.putValue("aa", 11);
        final TreeNode<String, Integer, ?> newChildBB = newChildB.putValue("bb", null);
        final TreeNode<String, Integer, ?> newChildAAA = newChildAA.putValue("aaa", null);
        final TreeNode<String, Integer, ?> newChildBBB = newChildBB.putValue("bbb", 222);

        assertNull(newRoot.getValue(Collections.singletonList("a")));
        assertSame(newChildB.getValue(), newRoot.getValue(Collections.singletonList("b")));
        assertNull(newRoot.getValue(Collections.singletonList("c")));
        assertSame(newChildAA.getValue(), newRoot.getValue(Arrays.asList("a", "aa")));
        assertSame(newChildBB.getValue(), newRoot.getValue(Arrays.asList("b", "bb")));
        assertNull(newRoot.getValue(Arrays.asList("a", "ab")));
        assertNull(newRoot.getValue(Arrays.asList("b", "bc")));
        assertSame(newChildAAA.getValue(), newRoot.getValue(Arrays.asList("a", "aa", "aaa")));
        assertNull(newRoot.getValue(Arrays.asList("a", "aa", "aab")));
        assertSame(newChildBBB.getValue(), newRoot.getValue(Arrays.asList("b", "bb", "bbb")));
        assertNull(newRoot.getValue(Arrays.asList("b", "bb", "bbc")));
        assertNull(newRoot.getValue(Arrays.asList()));
    }

    /**
     * Tests {@link TreeNode#setValue(Object)}.
     */
    @Test
    public void testSetValue() {
        this.root.setValue(0);
        assertEquals(Integer.valueOf(0), this.root.getValue());
        this.childA.setValue(4);
        assertEquals(Integer.valueOf(4), this.childA.getValue());
    }

    /**
     * Tests {@link TreeNode#getNode(Object)}.
     */
    @Test
    public void testGetNode() {
        TreeNode<String, Integer, ?> node = this.root.getNode("a");
        assertSame(this.childA, node);
        node = this.root.getNode("b");
        assertSame(this.childB, node);
        node = this.root.getNode("a").getNode("aa");
        assertSame(this.childAA, node);
        node = this.root.getNode("b").getNode("bb");
        assertSame(this.childBB, node);
        node = this.root.getNode("c");
        assertNull(node);
    }

    /**
     * Tests {@link TreeNode#getNode(java.util.List)}.
     */
    @Test
    public void testGetNodeByPath() {
        TreeNode<String, Integer, ?> node = this.root.getNode(Arrays.asList("a"));
        assertSame(this.childA, node);
        node = this.root.getNode(Arrays.asList("b"));
        assertSame(this.childB, node);
        node = this.root.getNode(Arrays.asList("a", "aa"));
        assertSame(this.childAA, node);
        node = this.root.getNode(Arrays.asList("b", "bb"));
        assertSame(this.childBB, node);
        node = this.root.getNode(Arrays.asList("c"));
        assertNull(node);
    }

    /**
     * Tests {@link TreeNode#putValue(Object, Object)}.
     */
    @Test
    public void testPutValue() {
        final TreeNode<String, Integer, ?> node = this.root.putValue("a", 3);
        assertSame(node, this.childA);
        assertEquals(Integer.valueOf(3), node.getValue());
    }

    /**
     * Tests {@link TreeNode#putValue(java.util.List, Object)} (all intermediate nodes are available).
     */
    @Test
    public void testPutValueByPath1() {
        final TreeNode<String, Integer, ?> newRoot = this.createNode(null);
        final TreeNode<String, Integer, ?> newChildA =
                newRoot.putValue(Collections.singletonList("a"), this.childA.getValue());
        final TreeNode<String, Integer, ?> newChildB =
                newRoot.putValue(Collections.singletonList("b"), this.childB.getValue());
        final TreeNode<String, Integer, ?> newChildAA =
                newRoot.putValue(Arrays.asList("a", "aa"), this.childAA.getValue());
        final TreeNode<String, Integer, ?> newChildBB =
                newRoot.putValue(Arrays.asList("b", "bb"), this.childBB.getValue());

        assertEquals(this.root, newRoot);
        assertEquals(this.childA, newChildA);
        assertEquals(this.childB, newChildB);
        assertEquals(this.childAA, newChildAA);
        assertEquals(this.childBB, newChildBB);
    }

    /**
     * Tests {@link TreeNode#putValue(java.util.List, Object)} (some intermediate nodes are missing).
     */
    @Test
    public void testPutValueByPath2() {
        final TreeNode<String, Integer, ?> newRoot = this.createNode(null);
        final TreeNode<String, Integer, ?> newChildAA =
                newRoot.putValue(Arrays.asList("a", "aa"), this.childAA.getValue());
        final TreeNode<String, Integer, ?> newChildBB =
                newRoot.putValue(Arrays.asList("b", "bb"), this.childBB.getValue());
        final TreeNode<String, Integer, ?> newChildA = newRoot.getNode("a");
        newChildA.setValue(this.childA.getValue());
        final TreeNode<String, Integer, ?> newChildB = newRoot.getNode("b");
        newChildB.setValue(this.childB.getValue());

        assertEquals(this.root, newRoot);
        assertEquals(this.childA, newChildA);
        assertEquals(this.childB, newChildB);
        assertEquals(this.childAA, newChildAA);
        assertEquals(this.childBB, newChildBB);
    }

    /**
     * Tests {@link TreeNode#getEntries()}.
     */
    @Test
    public void testGetEntries() {
        final Map<String, TreeNode<String, Integer, ?>> expectedRootEntries = new HashMap<>();
        expectedRootEntries.put("a", this.childA);
        expectedRootEntries.put("b", this.childB);
        assertEquals(expectedRootEntries.entrySet(), this.root.getEntries());

        final Map<String, TreeNode<String, Integer, ?>> expectedAEntries = new HashMap<>();
        expectedAEntries.put("aa", this.childAA);
        assertEquals(expectedAEntries.entrySet(), this.childA.getEntries());

        final Map<String, TreeNode<String, Integer, ?>> expectedBEntries = new HashMap<>();
        expectedBEntries.put("bb", this.childBB);
        assertEquals(expectedBEntries.entrySet(), this.childB.getEntries());

        assertEquals(new HashMap<>().entrySet(), this.childAA.getEntries());
        assertEquals(new HashMap<>().entrySet(), this.childBB.getEntries());
    }

    /**
     * Tests {@link TreeNode#getValues(java.util.List)}.
     */
    @Test
    public void testGetValues() {
        assertEquals(Arrays.asList(11, 1), this.root.getValues(Arrays.asList("a", "aa")));
        assertEquals(Arrays.asList(22, 2), this.root.getValues(Arrays.asList("b", "bb")));

        assertEquals(Arrays.asList(1), this.root.getValues(Arrays.asList("a", "x")));
        assertEquals(Arrays.asList(2), this.root.getValues(Arrays.asList("b", "y")));

        this.root.setValue(0);
        assertEquals(Arrays.asList(11, 1, 0), this.root.getValues(Arrays.asList("a", "aa")));
        assertEquals(Arrays.asList(22, 2, 0), this.root.getValues(Arrays.asList("b", "bb")));

        this.childA.setValue(null);
        this.childB.setValue(null);
        assertEquals(Arrays.asList(11, 0), this.root.getValues(Arrays.asList("a", "aa")));
        assertEquals(Arrays.asList(22, 0), this.root.getValues(Arrays.asList("b", "bb")));

        this.childAA.setValue(null);
        this.childBB.setValue(null);
        assertEquals(Arrays.asList(0), this.root.getValues(Arrays.asList("a", "aa")));
        assertEquals(Arrays.asList(0), this.root.getValues(Arrays.asList("b", "bb")));

        this.root.setValue(null);
        assertEquals(Collections.emptyList(), this.root.getValues(Arrays.asList("a", "aa")));
        assertEquals(Collections.emptyList(), this.root.getValues(Arrays.asList("b", "bb")));
    }

    /**
     * Tests {@link TreeNode#getNearestValue(java.util.List)}.
     */
    @Test
    public void testGetDeepestNode() {
        final TreeNode<String, Integer, ?> newRoot = this.createNode(null);
        final TreeNode<String, Integer, ?> newChildA = newRoot.putValue("a", null);
        final TreeNode<String, Integer, ?> newChildB = newRoot.putValue("b", 2);
        final TreeNode<String, Integer, ?> newChildAA = newChildA.putValue("aa", 11);
        final TreeNode<String, Integer, ?> newChildBB = newChildB.putValue("bb", null);
        newChildAA.putValue("aaa", null);
        final TreeNode<String, Integer, ?> newChildBBB = newChildBB.putValue("bbb", 222);

        assertNull(newRoot.getNearestValue(Collections.singletonList("a")));
        assertSame(newChildB.getValue(), newRoot.getNearestValue(Collections.singletonList("b")));
        assertNull(newRoot.getNearestValue(Collections.singletonList("c")));
        assertSame(newChildAA.getValue(), newRoot.getNearestValue(Arrays.asList("a", "aa")));
        assertSame(newChildB.getValue(), newRoot.getNearestValue(Arrays.asList("b", "bb")));
        assertNull(newRoot.getNearestValue(Arrays.asList("a", "ab")));
        assertSame(newChildB.getValue(), newRoot.getNearestValue(Arrays.asList("b", "bc")));
        assertSame(newChildAA.getValue(), newRoot.getNearestValue(Arrays.asList("a", "aa", "aaa")));
        assertSame(newChildAA.getValue(), newRoot.getNearestValue(Arrays.asList("a", "aa", "aab")));
        assertSame(newChildBBB.getValue(), newRoot.getNearestValue(Arrays.asList("b", "bb", "bbb")));
        assertSame(newChildB.getValue(), newRoot.getNearestValue(Arrays.asList("b", "bb", "bbc")));
        assertNull(newRoot.getNearestValue(Arrays.asList()));
    }

    /**
     * Tests {@link TreeNode#getHeight()}.
     */
    @Test
    public void testGetHeight() {
        assertEquals(3, this.root.getHeight());
        assertEquals(2, this.childA.getHeight());
        assertEquals(2, this.childB.getHeight());
        assertEquals(1, this.childAA.getHeight());
        assertEquals(1, this.childBB.getHeight());
    }
}
