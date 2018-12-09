package de.setsoftware.reviewtool.ordering.efficientalgorithm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import org.junit.Test;

/**
 * Tests for the bundle combination tree.
 */
public class BundleCombinationTreeTest {

    private static BundleCombinationTestHelper create(Integer... items) {
        return new BundleCombinationTestHelper(BundleCombinationTreeElement.create(Arrays.asList(items)));
    }

    private static SimpleSet<Integer> set(Integer... set) {
        return new SimpleSetAdapter<>(new HashSet<>(Arrays.asList(set)));
    }

    /**
     * Helper for the tests that stores results and checks invariants.
     */
    private static class BundleCombinationTestHelper {
        private BundleCombinationTreeElement<Integer> current;
        private final List<SimpleSet<Integer>> historySuccess;
        private final List<SimpleSet<Integer>> historyConflict;

        public BundleCombinationTestHelper(BundleCombinationTreeElement<Integer> initial) {
            this.current = initial;
            this.historySuccess = new ArrayList<>();
            this.historyConflict = new ArrayList<>();
        }

        public boolean addAndCheckInvariants(Integer... set) {
            final SimpleSet<Integer> bundle = set(set);
            final BundleCombinationTreeElement<Integer> newTree = this.current.bundle(bundle);
            if (newTree != null) {
                this.current = newTree;
                this.historySuccess.add(bundle);
            } else {
                this.historyConflict.add(bundle);
            }
            this.checkInvariants();
            return newTree != null;
        }

        private void checkInvariants() {
            for (final SimpleSet<Integer> setInHistory : this.historySuccess) {
                final BundleCombinationTreeElement<Integer> addedAgain = this.current.bundle(setInHistory);
                //adding it again is possible
                assertNotNull("adding again was not possible for " + setInHistory + " to " + this.current
                        + " (history=" + this.historySuccess + ")",
                        addedAgain);
                //the returned order contains the bundled set as direct neighbors
                assertTrue("matched but not contained in order " + setInHistory + ", " + this.current.getPossibleOrder(noComparator())
                        + " of tree " + this.current + " (history=" + this.historySuccess + ")",
                        this.containsAsNeighbours(this.current.getPossibleOrder(noComparator()), setInHistory));
            }

            for (final SimpleSet<Integer> setInHistory : this.historyConflict) {
                final BundleCombinationTreeElement<Integer> addedAgain = this.current.bundle(setInHistory);
                //adding it again is still not possible
                assertNull("adding again became possible for " + setInHistory + " to "
                        + this.current + " (history=" + this.historySuccess + ")",
                        addedAgain);
                //the returned order does not contain the bundled set as direct neighbors
                assertFalse("not matched but contained in order " + setInHistory + ", "
                        + this.current.getPossibleOrder(noComparator()),
                        this.containsAsNeighbours(this.current.getPossibleOrder(noComparator()), setInHistory));
            }
        }

        private boolean containsAsNeighbours(List<Integer> possibleOrder, SimpleSet<Integer> set) {
            if (!possibleOrder.containsAll(set.toSet())) {
                return false;
            }
            boolean hadMatch = false;
            boolean lastWasContained = false;
            for (int i = 0; i < possibleOrder.size(); i++) {
                if (set.contains(possibleOrder.get(i))) {
                    if (lastWasContained) {
                        //ok
                    } else if (hadMatch) {
                        return false;
                    } else {
                        hadMatch = true;
                        lastWasContained = true;
                    }
                } else {
                    lastWasContained = false;
                }
            }
            return true;
        }

        public void addAndCheckInvariantsAndSuccess(Integer... set) {
            assertTrue("unexpected conflict for set " + Arrays.toString(set) + ", old tree is "
                     + this.current, this.addAndCheckInvariants(set));
        }

        public void addAndCheckInvariantsAndConflict(Integer... set) {
            assertFalse("expected conflict did not happen for set " + Arrays.toString(set) + ", new tree is "
                    + this.current, this.addAndCheckInvariants(set));
        }

        public BundleCombinationTreeElement<Integer> get() {
            return this.current;
        }

    }


    private static List<Integer> getPossibleOrder(final BundleCombinationTestHelper b) {
        return b.get().getPossibleOrder(noComparator());
    }

    private static Comparator<Integer> noComparator() {
        return new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return 0;
            }
        };
    }

    @Test
    public void testWithoutRestrictions() {
        final BundleCombinationTestHelper b = create(1, 2, 3);
        assertEquals(Arrays.asList(1, 2, 3), getPossibleOrder(b));
        assertEquals("{1, 2, 3}", b.get().toString());
    }

    @Test
    public void testSingleElement() {
        final BundleCombinationTestHelper b = create(5);
        assertEquals(Arrays.asList(5), getPossibleOrder(b));
    }

    @Test
    public void testSimple1() {
        final BundleCombinationTestHelper b = create(1, 2, 3, 4, 5);
        b.addAndCheckInvariantsAndSuccess(1, 2);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), getPossibleOrder(b));
        assertEquals("{{1, 2}, 3, 4, 5}", b.get().toString());
    }

    @Test
    public void testSimple2() {
        final BundleCombinationTestHelper b = create(1, 2, 3, 4);
        b.addAndCheckInvariantsAndSuccess(3, 4);
        b.addAndCheckInvariantsAndSuccess(1, 2);
        assertEquals(Arrays.asList(1, 2, 3, 4), getPossibleOrder(b));
        assertEquals("{{1, 2}, {3, 4}}", b.get().toString());
    }

    @Test
    public void testFixWholeOrder() {
        final BundleCombinationTestHelper b = create(1, 2, 3);
        b.addAndCheckInvariantsAndSuccess(1, 2);
        b.addAndCheckInvariantsAndSuccess(2, 3);
        assertEquals(Arrays.asList(1, 2, 3), getPossibleOrder(b));
        assertEquals("[1, 2, 3]", b.get().toString());
    }

    @Test
    public void testConflict1() {
        final BundleCombinationTestHelper b = create(1, 2, 3);
        b.addAndCheckInvariantsAndSuccess(1, 2);
        b.addAndCheckInvariantsAndSuccess(2, 3);
        b.addAndCheckInvariantsAndConflict(1, 3);
        assertEquals(Arrays.asList(1, 2, 3), getPossibleOrder(b));
        assertEquals("[1, 2, 3]", b.get().toString());
    }

    @Test
    public void testInversalDoesHappen1() {
        final BundleCombinationTestHelper b = create(1, 2, 3, 4);
        b.addAndCheckInvariantsAndSuccess(1, 2);
        b.addAndCheckInvariantsAndSuccess(3, 4);
        b.addAndCheckInvariantsAndSuccess(1, 4);
        assertEquals(Arrays.asList(3, 4, 1, 2), getPossibleOrder(b));
        assertEquals("[3, 4, 1, 2]", b.get().toString());
    }

    @Test
    public void testConflictWithMultiplePartialBottoms() {
        final BundleCombinationTestHelper b = create(1, 2, 3, 4, 5, 6);
        b.addAndCheckInvariantsAndSuccess(1, 2);
        b.addAndCheckInvariantsAndSuccess(3, 4);
        b.addAndCheckInvariantsAndSuccess(5, 6);
        b.addAndCheckInvariantsAndConflict(2, 4, 6);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), getPossibleOrder(b));
        assertEquals("{{1, 2}, {3, 4}, {5, 6}}", b.get().toString());
    }

    @Test
    public void testConflictWithMultiplePartialTops() {
        final BundleCombinationTestHelper b = create(1, 2, 3, 4, 5, 6);
        b.addAndCheckInvariantsAndSuccess(1, 2);
        b.addAndCheckInvariantsAndSuccess(3, 4);
        b.addAndCheckInvariantsAndSuccess(5, 6);
        b.addAndCheckInvariantsAndConflict(1, 3, 5);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), getPossibleOrder(b));
        assertEquals("{{1, 2}, {3, 4}, {5, 6}}", b.get().toString());
    }

    @Test
    public void testInversalDoesHappen2() {
        final BundleCombinationTestHelper b = create(1, 2, 3, 4);
        b.addAndCheckInvariantsAndSuccess(1, 2);
        b.addAndCheckInvariantsAndSuccess(3, 4);
        b.addAndCheckInvariantsAndSuccess(1, 3);
        assertEquals(Arrays.asList(4, 3, 1, 2), getPossibleOrder(b));
        assertEquals("[4, 3, 1, 2]", b.get().toString());
    }

    @Test
    public void testInversalDoesHappen3() {
        final BundleCombinationTestHelper b = create(1, 2, 3, 4);
        b.addAndCheckInvariantsAndSuccess(1, 2);
        b.addAndCheckInvariantsAndSuccess(3, 4);
        b.addAndCheckInvariantsAndSuccess(2, 4);
        assertEquals(Arrays.asList(3, 4, 2, 1), getPossibleOrder(b));
        assertEquals("[3, 4, 2, 1]", b.get().toString());
    }

    @Test
    public void testConflictInversalNotPossible() {
        final BundleCombinationTestHelper b = create(1, 2, 3, 4, 5, 6);
        b.addAndCheckInvariantsAndSuccess(1, 2, 3);
        b.addAndCheckInvariantsAndSuccess(4, 5, 6);
        b.addAndCheckInvariantsAndSuccess(3, 4);
        b.addAndCheckInvariantsAndConflict(2, 5);
        b.addAndCheckInvariantsAndConflict(1, 6);
        b.addAndCheckInvariantsAndConflict(1, 5);
        b.addAndCheckInvariantsAndConflict(2, 6);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), getPossibleOrder(b));
        assertEquals("[{1, 2}, 3, 4, {5, 6}]", b.get().toString());
    }

    @Test
    public void testPartialTopInFixedOrder() {
        final BundleCombinationTestHelper b = create(1, 2, 3, 4, 5, 6, 7, 8);
        b.addAndCheckInvariantsAndSuccess(1, 2, 3, 4, 5, 6);
        b.addAndCheckInvariantsAndSuccess(5, 6, 7, 8);
        b.addAndCheckInvariantsAndSuccess(1, 2, 3);
        b.addAndCheckInvariantsAndSuccess(1, 2);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8), getPossibleOrder(b));
        assertEquals("[{{{1, 2}, 3}, 4}, {5, 6}, {7, 8}]", b.get().toString());
    }

    @Test
    public void testPartialTopInFixedOrder2() {
        final BundleCombinationTestHelper b = create(1, 2, 3, 4, 5, 6);
        b.addAndCheckInvariantsAndSuccess(1, 2);
        b.addAndCheckInvariantsAndSuccess(2, 3, 4, 5, 6);
        b.addAndCheckInvariantsAndSuccess(2, 3, 4, 5);
        b.addAndCheckInvariantsAndSuccess(2, 3);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), getPossibleOrder(b));
        assertEquals("[1, 2, 3, {4, 5}, 6]", b.get().toString());
    }

    @Test
    public void testPartialBottomInFixedOrder() {
        final BundleCombinationTestHelper b = create(1, 2, 3, 4, 5, 6, 7, 8);
        b.addAndCheckInvariantsAndSuccess(3, 4, 5, 6, 7, 8);
        b.addAndCheckInvariantsAndSuccess(1, 2, 3, 4);
        b.addAndCheckInvariantsAndSuccess(6, 7, 8);
        b.addAndCheckInvariantsAndSuccess(7, 8);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8), getPossibleOrder(b));
        assertEquals("[{1, 2}, {3, 4}, {5, {6, {7, 8}}}]", b.get().toString());
    }

    @Test
    public void testFurtherBinding1() {
        final BundleCombinationTestHelper b = create(1, 2, 3, 4, 5, 6);
        b.addAndCheckInvariantsAndSuccess(1, 2, 3);
        b.addAndCheckInvariantsAndSuccess(4, 5, 6);
        b.addAndCheckInvariantsAndSuccess(3, 4);
        b.addAndCheckInvariantsAndSuccess(1, 3, 4, 6);
        assertEquals(Arrays.asList(2, 1, 3, 4, 6, 5), getPossibleOrder(b));
        assertEquals("[2, 1, 3, 4, 6, 5]", b.get().toString());
    }

    @Test
    public void testFurtherBinding2() {
        final BundleCombinationTestHelper b = create(1, 2, 3, 4, 5, 6);
        b.addAndCheckInvariantsAndSuccess(2, 3, 4, 5);
        b.addAndCheckInvariantsAndSuccess(1, 2, 3);
        b.addAndCheckInvariantsAndSuccess(3, 4, 5, 6);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), getPossibleOrder(b));
        assertEquals("[1, 2, 3, {4, 5}, 6]", b.get().toString());
    }

    @Test
    public void testFurtherBinding3() {
        final BundleCombinationTestHelper b = create(1, 2, 3, 4, 5);
        b.addAndCheckInvariantsAndSuccess(1, 2, 3);
        b.addAndCheckInvariantsAndSuccess(1, 2, 4, 5);
        b.addAndCheckInvariantsAndConflict(1, 3, 5);
        assertEquals(Arrays.asList(4, 5, 1, 2, 3), getPossibleOrder(b));
        assertEquals("[{4, 5}, {1, 2}, 3]", b.get().toString());
    }

    @Test
    public void testFurtherBinding4() {
        final BundleCombinationTestHelper b = create(1, 2, 3, 4, 5);
        b.addAndCheckInvariantsAndSuccess(1, 2, 3);
        b.addAndCheckInvariantsAndSuccess(1, 2, 4, 5);
        b.addAndCheckInvariantsAndSuccess(2, 3);
        assertEquals(Arrays.asList(4, 5, 1, 2, 3), getPossibleOrder(b));
        assertEquals("[{4, 5}, 1, 2, 3]", b.get().toString());
    }

    @Test
    public void testFurtherBinding5() {
        final BundleCombinationTestHelper b = create(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19);
        b.addAndCheckInvariantsAndSuccess(1, 9, 11, 12, 15, 17, 19);
        b.addAndCheckInvariantsAndSuccess(2, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19);
        b.addAndCheckInvariantsAndSuccess(4, 11, 12, 17, 18);
        assertEquals(Arrays.asList(3, 1, 9, 15, 19, 11, 12, 17, 18, 4, 16, 14, 13, 10, 8, 7, 6, 5, 2),
                getPossibleOrder(b));
        assertEquals("{3, [1, {9, 15, 19}, {11, 12, 17}, {18, 4}, {16, 14, 13, 10, 8, 7, 6, 5, 2}]}",
                b.get().toString());
    }

    @Test
    public void testFurtherBinding6() {
        final BundleCombinationTestHelper b = create(1, 2, 3, 4, 5);
        b.addAndCheckInvariantsAndSuccess(1, 2, 4, 5);
        b.addAndCheckInvariantsAndSuccess(3, 4, 5);
        b.addAndCheckInvariantsAndConflict(1, 3, 4);
        assertEquals(Arrays.asList(1, 2, 4, 5, 3), getPossibleOrder(b));
        assertEquals("[{1, 2}, {4, 5}, 3]", b.get().toString());
    }

    @Test
    public void testFurtherBinding7() {
        final BundleCombinationTestHelper b = create(1, 2, 3, 4, 5);
        b.addAndCheckInvariantsAndSuccess(2, 3, 5);
        b.addAndCheckInvariantsAndSuccess(1, 3);
        b.addAndCheckInvariantsAndSuccess(1, 2, 3, 4);
        assertEquals(Arrays.asList(5, 2, 3, 1, 4), getPossibleOrder(b));
        assertEquals("[5, 2, 3, 1, 4]", b.get().toString());
    }

    @Test
    public void testFurtherBinding8() {
        final BundleCombinationTestHelper b = create(1, 2, 3, 4, 5);
        b.addAndCheckInvariantsAndSuccess(2, 3, 4, 5);
        b.addAndCheckInvariantsAndSuccess(1, 5);
        b.addAndCheckInvariantsAndSuccess(2, 4);
        b.addAndCheckInvariantsAndSuccess(1, 2, 5);
        assertEquals(Arrays.asList(3, 4, 2, 5, 1), getPossibleOrder(b));
        assertEquals("[3, 4, 2, 5, 1]", b.get().toString());
    }

    @Test
    public void testFurtherBinding9() {
        final BundleCombinationTestHelper b = create(1, 2, 3, 4, 5);
        b.addAndCheckInvariantsAndSuccess(1, 2);
        b.addAndCheckInvariantsAndSuccess(1, 3, 4);
        b.addAndCheckInvariantsAndSuccess(4, 5);
        assertEquals(Arrays.asList(5, 4, 3, 1, 2), getPossibleOrder(b));
        assertEquals("[5, 4, 3, 1, 2]", b.get().toString());
    }

    @Test
    public void testFurtherBinding10() {
        final BundleCombinationTestHelper b = create(1, 2, 3, 4, 5);
        b.addAndCheckInvariantsAndSuccess(1, 2);
        b.addAndCheckInvariantsAndSuccess(1, 2, 3, 5);
        b.addAndCheckInvariantsAndSuccess(2, 3, 5);
        b.addAndCheckInvariantsAndSuccess(3, 4);
        b.addAndCheckInvariantsAndSuccess(2, 3, 4, 5);
        assertEquals(Arrays.asList(1, 2, 5, 3, 4), getPossibleOrder(b));
        assertEquals("[1, 2, 5, 3, 4]", b.get().toString());
    }

    @Test
    public void testFurtherBinding11() {
        final BundleCombinationTestHelper b = create(1, 2, 3, 4, 5);
        b.addAndCheckInvariantsAndSuccess(1, 3, 4, 5);
        b.addAndCheckInvariantsAndSuccess(1, 2, 5);
        b.addAndCheckInvariantsAndSuccess(3, 4);
        b.addAndCheckInvariantsAndSuccess(1, 4);
        b.addAndCheckInvariantsAndSuccess(1, 4, 5);
        assertEquals(Arrays.asList(2, 5, 1, 4, 3), getPossibleOrder(b));
        assertEquals("[2, 5, 1, 4, 3]", b.get().toString());
    }

    @Test
    public void testFurtherBinding12() {
        final BundleCombinationTestHelper b = create(1, 2, 3, 4, 5, 6);
        b.addAndCheckInvariantsAndSuccess(1, 5, 6);
        b.addAndCheckInvariantsAndSuccess(1, 2, 3, 4, 6);
        b.addAndCheckInvariantsAndSuccess(1, 2, 5, 6);
        b.addAndCheckInvariantsAndConflict(1, 2, 4, 5);
        assertEquals(Arrays.asList(4, 3, 2, 1, 6, 5), getPossibleOrder(b));
        assertEquals("[{4, 3}, 2, {1, 6}, 5]", b.get().toString());
    }

    @Test
    public void testSplit1() {
        final BundleCombinationTreeNode<Integer> tree = ta(tl(1), tl(2), tl(3));
        assertEquals(Arrays.asList(tl(1), ta(tl(2), tl(3))), tree.split(set(1)));
        assertEquals(Arrays.asList(ta(tl(1), tl(2)), tl(3)), tree.split(set(3)));
        assertEquals(Arrays.asList(ta(tl(1), tl(2)), tl(3)), tree.split(set(1, 2)));
        assertEquals(Arrays.asList(tl(1), ta(tl(2), tl(3))), tree.split(set(2, 3)));
    }

    @Test
    public void testSplit2() {
        final BundleCombinationTreeNode<Integer> tree = tf(tl(1), tl(2), tl(3));
        assertEquals(Arrays.asList(tl(1), tl(2), tl(3)), tree.split(set(1)));
        assertEquals(Arrays.asList(tl(1), tl(2), tl(3)), tree.split(set(3)));
        assertEquals(Arrays.asList(tl(1), tl(2), tl(3)), tree.split(set(1, 2)));
        assertEquals(Arrays.asList(tl(1), tl(2), tl(3)), tree.split(set(2, 3)));
    }

    @Test
    public void testSplit3() {
        final BundleCombinationTreeNode<Integer> tree = ta(tl(1), tl(2), tl(3), tl(4));
        assertEquals(Arrays.asList(ta(tl(1), tl(2)), ta(tl(3), tl(4))), tree.split(set(1, 2)));
        assertEquals(Arrays.asList(ta(tl(1), tl(2)), ta(tl(3), tl(4))), tree.split(set(3, 4)));
    }

    @Test
    public void testSplit4() {
        final BundleCombinationTreeNode<Integer> tree = tf(tl(1), tl(2), tl(3), tl(4));
        assertEquals(Arrays.asList(tl(1), tl(2), tl(3), tl(4)), tree.split(set(1, 2)));
        assertEquals(Arrays.asList(tl(1), tl(2), tl(3), tl(4)), tree.split(set(3, 4)));
    }

    @Test
    public void testSplit5() {
        final BundleCombinationTreeNode<Integer> tree = ta(ta(tl(1), tl(2)), tl(3), tl(4));
        assertEquals(Arrays.asList(tl(1), tl(2), ta(tl(3), tl(4))), tree.split(set(1)));
        assertEquals(Arrays.asList(ta(tl(1), tl(2)), ta(tl(3), tl(4))), tree.split(set(1, 2)));
        assertEquals(Arrays.asList(ta(ta(tl(1), tl(2)), tl(3)), tl(4)), tree.split(set(1, 2, 3)));
    }

    @Test
    public void testSplit6() {
        final BundleCombinationTreeNode<Integer> tree = tf(ta(tl(1), tl(2)), tl(3), tl(4));
        assertEquals(Arrays.asList(tl(1), tl(2), tl(3), tl(4)), tree.split(set(1)));
        assertEquals(Arrays.asList(ta(tl(1), tl(2)), tl(3), tl(4)), tree.split(set(1, 2)));
        assertEquals(Arrays.asList(ta(tl(1), tl(2)), tl(3), tl(4)), tree.split(set(1, 2, 3)));
    }

    @Test
    public void testSplit7() {
        final BundleCombinationTreeNode<Integer> tree = tf(ta(tl(1), tl(2)), tl(3), tl(4), ta(tl(5), tl(6)));
        assertEquals(Arrays.asList(ta(tl(1), tl(2)), tl(3), tl(4), ta(tl(5), tl(6))), tree.split(set(1, 2, 3)));
    }

    private static BundleCombinationTreeLeaf<Integer> tl(int i) {
        return new BundleCombinationTreeLeaf<>(i);
    }

    @SafeVarargs
    private static BundleCombinationTreeNode<Integer> ta(BundleCombinationTreeElement<Integer>... children) {
        return new BundleCombinationTreeNode<>(children, true, true);
    }

    @SafeVarargs
    private static BundleCombinationTreeNode<Integer> tf(BundleCombinationTreeElement<Integer>... children) {
        return new BundleCombinationTreeNode<>(children, false, true);
    }

    private static void doTestWithGeneratedData(Random r) {
        final int size = r.nextInt(30) + 3;
        final List<Integer> ints = new ArrayList<>();
        for (int i = 1; i <= size; i++) {
            ints.add(i);
        }
        final BundleCombinationTestHelper b = create(ints.toArray(new Integer[ints.size()]));
        final int rounds = r.nextInt(20) + 1;
        for (int round = 0; round < rounds; round++) {
            Collections.shuffle(ints, r);
            final int setSize = 2 + r.nextInt(ints.size() - 2);
            final Integer[] set = ints.subList(0, setSize).toArray(new Integer[setSize]);
            b.addAndCheckInvariants(set);
        }
    }

    @Test
    public void testSmokeTest() {
        for (int i = 0; i < 2000; i++) {
            try {
                doTestWithGeneratedData(new Random(i));
            } catch (final AssertionError e) {
                throw new AssertionError("problem with seed " + i, e);
            }
        }
    }

}
