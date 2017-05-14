package de.setsoftware.reviewtool.ordering2.efficientalgorithm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;

public class PositionTreeTest {

    private static PositionTreeTestHelper create(PositionTreeNode<String> tree) {
        return new PositionTreeTestHelper(tree);
    }

    private static Set<String> set(String... set) {
        return new TreeSet<>(Arrays.asList(set));
    }

    private static final class HistoryItem {
        private final Set<String> match;
        private final String toFix;
        private final TargetPosition position;

        public HistoryItem(Set<String> match, String toFix, TargetPosition position) {
            this.match = Collections.unmodifiableSet(match);
            this.toFix = toFix;
            this.position = position;
        }

        public PositionTreeElement<String> replay(PositionTreeNode<String> current) {
            return current.fixPosition(this.match, this.toFix, this.position);
        }

        @Override
        public String toString() {
            return this.position + "=" + this.toFix + " in " + this.match;
        }

        public HistoryItem changedPosition(boolean b) {
            final TargetPosition newPos;
            if (this.match.size() == 2) {
                newPos = this.position != TargetPosition.FIRST ? TargetPosition.FIRST : TargetPosition.SECOND;
            } else {
                newPos = TargetPosition.values()[(this.position.ordinal() + (b ? 1 : 2)) % 3];
            }
            return new HistoryItem(this.match, this.toFix, newPos);
        }

        public HistoryItem changedValue() {
            for (final String s : this.match) {
                if (!s.equals(this.toFix)) {
                    return new HistoryItem(this.match, s, this.position);
                }
            }
            throw new AssertionError();
        }
    }

    private static final class PositionTreeTestHelper {
        private PositionTreeNode<String> current;
        private final List<HistoryItem> historySuccess;
        private final List<HistoryItem> historyConflict;

        public PositionTreeTestHelper(PositionTreeNode<String> initial) {
            this.current = initial;
            this.historySuccess = new ArrayList<>();
            this.historyConflict = new ArrayList<>();
        }

        public boolean fixPosition(Set<String> match, String toFix, TargetPosition position) {
            final PositionTreeNode<String> newTree = this.current.fixPosition(match, toFix, position);
            if (newTree != null) {
                this.historySuccess.add(new HistoryItem(match, toFix, position));
                this.current = newTree;
            } else {
                this.historyConflict.add(new HistoryItem(match, toFix, position));
            }
            this.checkInvariants();
            return newTree != null;
        }

        public void fixPositionAndCheckSuccess(Set<String> match, String toFix, TargetPosition position) {
             assertTrue("unexpected conflict for " + toFix + "=" + position + " in " + match + ", old tree is " + this.current,
                     this.fixPosition(match, toFix, position));
        }

        public void fixPositionAndCheckConflict(Set<String> match, String toFix, TargetPosition position) {
            assertFalse("expected conflict did not happen for " + toFix + "=" + position + " in " + match + ", new tree is " + this.current,
                    this.fixPosition(match, toFix, position));
        }

        private void checkInvariants() {
            for (final HistoryItem historyItem : this.historySuccess) {
                final PositionTreeElement<String> addedAgain = historyItem.replay(this.current);
                //adding it again is possible
                assertNotNull("adding again was not possible for " + historyItem + " to " + this.current
                        + " (history=" + this.historySuccess + ")",
                        addedAgain);
                //adding it at a different position is not possible anymore
                assertNull("adding at a different position is still possible for " + historyItem + " to " + this.current + " (history=" + this.historySuccess + ")",
                        historyItem.changedPosition(true).replay(this.current));
                assertNull("adding at a different position is still possible for " + historyItem + " to " + this.current + " (history=" + this.historySuccess + ")",
                        historyItem.changedPosition(false).replay(this.current));
                //adding another value at the same position is not possible anymore
                assertNull("adding another value at the same position is still possible for " + historyItem + " to " + this.current + " (history=" + this.historySuccess + ")",
                        historyItem.changedValue().replay(this.current));
                //the returned order contains the item at the correct position
                assertTrue("matched but not contained in order " + historyItem + ", " + this.current.getPossibleOrder()
                        + " of tree " + this.current + " (history=" + this.historySuccess + ")",
                        this.containsAtCorrectPosition(this.current.getPossibleOrder(), historyItem));
            }

            for (final HistoryItem historyItem : this.historyConflict) {
                final PositionTreeElement<String> addedAgain = historyItem.replay(this.current);
                //adding it again is still not possible
                assertNull("adding again became possible for " + historyItem + " to " + this.current + " (history=" + this.historySuccess + ")",
                        addedAgain);
                //the returned order does not contain the item at the correct position
                assertFalse("not matched but contained in order " + historyItem + ", " + this.current.getPossibleOrder(),
                        this.containsAtCorrectPosition(this.current.getPossibleOrder(), historyItem));
            }
        }

        private boolean containsAtCorrectPosition(List<String> possibleOrder, HistoryItem historyItem) {
            final int startIndex = this.findFirstIndexFrom(possibleOrder, historyItem.match);
            switch (historyItem.position) {
            case FIRST:
                return possibleOrder.get(startIndex).equals(historyItem.toFix);
            case SECOND:
                return possibleOrder.get(startIndex + 1).equals(historyItem.toFix);
            case LAST:
                return possibleOrder.get(startIndex + historyItem.match.size() - 1).equals(historyItem.toFix);
            default:
                throw new AssertionError();
            }
        }

        private int findFirstIndexFrom(List<String> order, Set<String> match) {
            for (int i = 0; i < order.size(); i++) {
                if (match.contains(order.get(i))) {
                    return i;
                }
            }
            throw new AssertionError("not contained " + match);
        }

        public PositionTreeElement<String> get() {
            return this.current;
        }

    }

    private static PositionTreeLeaf<String> tl(String s) {
        return new PositionTreeLeaf<String>(s);
    }

    @SafeVarargs
    private static PositionTreeNode<String> ta(PositionTreeElement<String>... children) {
        return new PositionTreeNodeReorderable<String>(children);
    }

    @SafeVarargs
    private static PositionTreeNode<String> tf(PositionTreeElement<String>... children) {
        return new PositionTreeNodeFixedOrder<String>(children);
    }


    @Test
    public void testWithoutRestrictions() {
        final PositionTreeTestHelper b = create(tf(tl("a"), tl("b"), tl("c")));
        assertEquals(Arrays.asList("a", "b", "c"), b.get().getPossibleOrder());
    }

    @Test
    public void testSimpleFixedOrder() {
        final PositionTreeTestHelper b = create(tf(tl("a"), tl("b"), tl("c")));
        b.fixPositionAndCheckSuccess(set("a", "b", "c"), "a", TargetPosition.FIRST);
        b.fixPositionAndCheckSuccess(set("a", "b", "c"), "c", TargetPosition.LAST);
        assertEquals(Arrays.asList("a", "b", "c"), b.get().getPossibleOrder());
    }

    @Test
    public void testConflictInFixedOrder() {
        final PositionTreeTestHelper b = create(tf(tl("a"), tl("b"), tl("c")));
        b.fixPositionAndCheckConflict(set("a", "b", "c"), "a", TargetPosition.SECOND);
        b.fixPositionAndCheckConflict(set("a", "b", "c"), "b", TargetPosition.FIRST);
        b.fixPositionAndCheckConflict(set("a", "b", "c"), "b", TargetPosition.LAST);
        b.fixPositionAndCheckConflict(set("a", "b", "c"), "c", TargetPosition.SECOND);
        assertEquals(Arrays.asList("a", "b", "c"), b.get().getPossibleOrder());
    }

    @Test
    public void testReverseFixedOrderFirst() {
        final PositionTreeTestHelper b = create(tf(tl("a"), tl("b"), tl("c"), tl("d")));
        b.fixPositionAndCheckSuccess(set("a", "b", "c", "d"), "d", TargetPosition.FIRST);
        assertEquals(Arrays.asList("d", "c", "b", "a"), b.get().getPossibleOrder());
    }

    @Test
    public void testReverseFixedOrderSecond() {
        final PositionTreeTestHelper b = create(tf(tl("a"), tl("b"), tl("c"), tl("d")));
        b.fixPositionAndCheckSuccess(set("a", "b", "c", "d"), "c", TargetPosition.SECOND);
        assertEquals(Arrays.asList("d", "c", "b", "a"), b.get().getPossibleOrder());
    }

    @Test
    public void testReverseFixedOrderLast() {
        final PositionTreeTestHelper b = create(tf(tl("a"), tl("b"), tl("c"), tl("d")));
        b.fixPositionAndCheckSuccess(set("a", "b", "c", "d"), "a", TargetPosition.LAST);
        assertEquals(Arrays.asList("d", "c", "b", "a"), b.get().getPossibleOrder());
    }

    @Test
    public void testReverseFixedOrderConflict() {
        final PositionTreeTestHelper b = create(tf(tl("a"), tl("b"), tl("c"), tl("d")));
        b.fixPositionAndCheckSuccess(set("a", "b", "c", "d"), "c", TargetPosition.SECOND);
        b.fixPositionAndCheckConflict(set("a", "b", "c", "d"), "a", TargetPosition.FIRST);
        assertEquals(Arrays.asList("d", "c", "b", "a"), b.get().getPossibleOrder());
    }

    @Test
    public void testFixedOrderWithSubsetMatch() {
        final PositionTreeTestHelper b = create(tf(tl("a"), tl("b"), tl("c")));
        b.fixPositionAndCheckSuccess(set("b", "c"), "b", TargetPosition.FIRST);
        b.fixPositionAndCheckConflict(set("b", "c"), "b", TargetPosition.SECOND);
        assertEquals(Arrays.asList("a", "b", "c"), b.get().getPossibleOrder());
    }

    @Test
    public void testSimpleReorderableTree1() {
        final PositionTreeTestHelper b = create(ta(tl("a"), tl("b"), tl("c")));
        b.fixPositionAndCheckSuccess(set("a", "b", "c"), "a", TargetPosition.LAST);
        b.fixPositionAndCheckSuccess(set("a", "b", "c"), "c", TargetPosition.SECOND);
        assertEquals(Arrays.asList("b", "c", "a"), b.get().getPossibleOrder());
    }

    @Test
    public void testSimpleReorderableTree2() {
        final PositionTreeTestHelper b = create(ta(tl("a"), tl("b"), tl("c")));
        b.fixPositionAndCheckSuccess(set("a", "b", "c"), "a", TargetPosition.FIRST);
        b.fixPositionAndCheckSuccess(set("a", "b", "c"), "b", TargetPosition.SECOND);
        b.fixPositionAndCheckSuccess(set("a", "b", "c"), "c", TargetPosition.LAST);
        assertEquals(Arrays.asList("a", "b", "c"), b.get().getPossibleOrder());
    }

    @Test
    public void testSimpleReorderableTree3() {
        final PositionTreeTestHelper b = create(ta(tl("a"), tl("b"), tl("c"), tl("d")));
        b.fixPositionAndCheckSuccess(set("a", "b", "c", "d"), "c", TargetPosition.FIRST);
        assertEquals(Arrays.asList("c", "a", "b", "d"), b.get().getPossibleOrder());
    }

    @Test
    public void testConflictInSimpleReorderableTreeByReassignmentOfValue() {
        final PositionTreeTestHelper b = create(ta(tl("a"), tl("b"), tl("c")));
        b.fixPositionAndCheckSuccess(set("a", "b", "c"), "a", TargetPosition.LAST);
        b.fixPositionAndCheckConflict(set("a", "b", "c"), "a", TargetPosition.SECOND);
        assertEquals(Arrays.asList("b", "c", "a"), b.get().getPossibleOrder());
    }

    @Test
    public void testConflictInSimpleReorderableTreeByReassignmentOfPosition() {
        final PositionTreeTestHelper b = create(ta(tl("a"), tl("b"), tl("c")));
        b.fixPositionAndCheckSuccess(set("a", "b", "c"), "a", TargetPosition.LAST);
        b.fixPositionAndCheckConflict(set("a", "b", "c"), "c", TargetPosition.LAST);
        assertEquals(Arrays.asList("b", "c", "a"), b.get().getPossibleOrder());
    }

    @Test
    public void testHierarchical1() {
        final PositionTreeTestHelper b = create(ta(tl("a"), ta(tl("b"), tl("c"))));
        b.fixPositionAndCheckSuccess(set("a", "b", "c"), "b", TargetPosition.LAST);
        b.fixPositionAndCheckConflict(set("a", "b", "c"), "c", TargetPosition.FIRST);
        assertEquals(Arrays.asList("a", "c", "b"), b.get().getPossibleOrder());
    }

    @Test
    public void testHierarchical2() {
        final PositionTreeTestHelper b = create(ta(tl("a"), ta(tl("b"), tl("c"))));
        b.fixPositionAndCheckConflict(set("a", "b", "c"), "a", TargetPosition.SECOND);
        assertEquals(Arrays.asList("a", "b", "c"), b.get().getPossibleOrder());
    }

    @Test
    public void testHierarchical3() {
        final PositionTreeTestHelper b = create(ta(tl("a"), ta(tl("b"), tl("c"))));
        b.fixPositionAndCheckSuccess(set("a", "b", "c"), "c", TargetPosition.FIRST);
        assertEquals(Arrays.asList("c", "b", "a"), b.get().getPossibleOrder());
    }

    @Test
    public void testHierarchical4() {
        final PositionTreeTestHelper b = create(ta(tl("a"), ta(tl("b"), tl("c"))));
        b.fixPositionAndCheckSuccess(set("a", "b", "c"), "c", TargetPosition.SECOND);
        b.fixPositionAndCheckSuccess(set("a", "b", "c"), "b", TargetPosition.FIRST);
        assertEquals(Arrays.asList("b", "c", "a"), b.get().getPossibleOrder());
    }

    @Test
    public void testHierarchical5() {
        final PositionTreeTestHelper b = create(ta(tl("a"), ta(tl("b"), tl("c"))));
        b.fixPositionAndCheckSuccess(set("a", "b", "c"), "c", TargetPosition.SECOND);
        b.fixPositionAndCheckSuccess(set("a", "b", "c"), "a", TargetPosition.LAST);
        assertEquals(Arrays.asList("b", "c", "a"), b.get().getPossibleOrder());
    }

    @Test
    public void testHierarchical6() {
        final PositionTreeTestHelper b = create(ta(tl("a"), ta(tl("b"), tl("c"))));
        b.fixPositionAndCheckSuccess(set("b", "c"), "c", TargetPosition.SECOND);
        b.fixPositionAndCheckSuccess(set("a", "b", "c"), "a", TargetPosition.FIRST);
        b.fixPositionAndCheckConflict(set("a", "b", "c"), "c", TargetPosition.SECOND);
        assertEquals(Arrays.asList("a", "b", "c"), b.get().getPossibleOrder());
    }

    @Test
    public void testHierarchical7() {
        final PositionTreeTestHelper b = create(ta(tl("a"), ta(tl("b"), tl("c"))));
        b.fixPositionAndCheckSuccess(set("b", "c"), "c", TargetPosition.SECOND);
        b.fixPositionAndCheckSuccess(set("a", "b", "c"), "a", TargetPosition.FIRST);
        b.fixPositionAndCheckConflict(set("a", "b", "c"), "b", TargetPosition.LAST);
        assertEquals(Arrays.asList("a", "b", "c"), b.get().getPossibleOrder());
    }

    @Test
    public void testHierarchical8() {
        final PositionTreeTestHelper b = create(ta(ta(tl("a"), tl("b")), tl("c"), tl("d")));
        b.fixPositionAndCheckSuccess(set("a", "b", "c", "d"), "d", TargetPosition.SECOND);
        assertEquals(Arrays.asList("c", "d", "a", "b"), b.get().getPossibleOrder());
    }

    @Test
    public void testHierarchical9() {
        final PositionTreeTestHelper b = create(ta(ta(tl("a"), tl("b")), ta(tl("c"), tl("d")), tl("e")));
        b.fixPositionAndCheckSuccess(set("a", "b", "c", "d", "e"), "c", TargetPosition.SECOND);
        assertEquals(Arrays.asList("d", "c", "a", "b", "e"), b.get().getPossibleOrder());
    }

    @Test
    public void testHierarchical10() {
        final PositionTreeTestHelper b = create(ta(ta(tl("a"), tl("b")), tl("c"), ta(tl("d"), tl("e"))));
        b.fixPositionAndCheckConflict(set("a", "b", "c", "d", "e"), "c", TargetPosition.SECOND);
        assertEquals(Arrays.asList("a", "b", "c", "d", "e"), b.get().getPossibleOrder());
    }

    @Test
    public void testHierarchical11() {
        final PositionTreeTestHelper b = create(ta(ta(ta(tl("a"), tl("b")), ta(tl("c"), tl("d"))), tl("e")));
        b.fixPositionAndCheckSuccess(set("a", "b", "c", "d", "e"), "c", TargetPosition.FIRST);
        b.fixPositionAndCheckConflict(set("a", "b", "c", "d", "e"), "a", TargetPosition.SECOND);
        assertEquals(Arrays.asList("c", "d", "a", "b", "e"), b.get().getPossibleOrder());
    }

    @Test
    public void testReorderablesInFixed1() {
        final PositionTreeTestHelper b = create(tf(ta(tl("a"), tl("b")), ta(tl("c"), tl("d"))));
        b.fixPositionAndCheckSuccess(set("a", "b", "c", "d"), "b", TargetPosition.FIRST);
        b.fixPositionAndCheckConflict(set("a", "b", "c", "d"), "a", TargetPosition.FIRST);
        assertEquals(Arrays.asList("b", "a", "c", "d"), b.get().getPossibleOrder());
    }

    @Test
    public void testReorderablesInFixed2() {
        final PositionTreeTestHelper b = create(tf(ta(tl("a"), tl("b")), ta(tl("c"), tl("d"))));
        b.fixPositionAndCheckSuccess(set("a", "b", "c", "d"), "b", TargetPosition.FIRST);
        b.fixPositionAndCheckSuccess(set("a", "b", "c", "d"), "a", TargetPosition.SECOND);
        b.fixPositionAndCheckSuccess(set("a", "b", "c", "d"), "c", TargetPosition.LAST);
        assertEquals(Arrays.asList("b", "a", "d", "c"), b.get().getPossibleOrder());
    }

    @Test
    public void testReorderablesInFixed3() {
        final PositionTreeTestHelper b = create(tf(ta(tl("a"), tl("b")), ta(tl("c"), tl("d"))));
        b.fixPositionAndCheckSuccess(set("a", "b", "c", "d"), "c", TargetPosition.SECOND);
        assertEquals(Arrays.asList("d", "c", "a", "b"), b.get().getPossibleOrder());
    }

    @Test
    public void testReorderablesInFixed4() {
        final PositionTreeTestHelper b = create(tf(ta(tl("a"), tl("b")), ta(tl("c"), tl("d"))));
        b.fixPositionAndCheckSuccess(set("a", "b"), "b", TargetPosition.FIRST);
        b.fixPositionAndCheckConflict(set("a", "b", "c", "d"), "a", TargetPosition.FIRST);
        assertEquals(Arrays.asList("b", "a", "c", "d"), b.get().getPossibleOrder());
    }

    @Test
    public void testReorderablesInFixed5() {
        final PositionTreeTestHelper b = create(tf(
                ta(tl("a"), tl("b"), tl("c")),
                ta(tl("d"), tl("e"), tl("f")),
                ta(tl("g"), tl("h"), tl("i"))));
        b.fixPositionAndCheckSuccess(set("d", "e", "f", "g", "h", "i"), "h", TargetPosition.LAST);
        b.fixPositionAndCheckSuccess(set("a", "b", "c", "d", "e", "f"), "c", TargetPosition.SECOND);
        b.fixPositionAndCheckSuccess(set("d", "e", "f", "g", "h", "i"), "f", TargetPosition.FIRST);
        assertEquals(Arrays.asList("a", "c", "b", "f", "d", "e", "g", "i", "h"), b.get().getPossibleOrder());
    }

    @Test
    public void testFixedInReorderable1() {
        final PositionTreeTestHelper b = create(ta(tf(tl("a"), tl("b"), tl("c")), tf(tl("d"), tl("e"), tl("f"))));
        b.fixPositionAndCheckSuccess(set("a", "b", "c", "d", "e", "f"), "c", TargetPosition.FIRST);
        b.fixPositionAndCheckSuccess(set("a", "b", "c", "d", "e", "f"), "d", TargetPosition.LAST);
        b.fixPositionAndCheckSuccess(set("d", "e", "f"), "f", TargetPosition.FIRST);
        b.fixPositionAndCheckConflict(set("a", "b", "c", "d", "e", "f"), "d", TargetPosition.SECOND);
        assertEquals(Arrays.asList("c", "b", "a", "f", "e", "d"), b.get().getPossibleOrder());
    }

    @Test
    public void testFixedInReorderable2() {
        final PositionTreeTestHelper b = create(ta(tf(tl("a"), tl("b"), tl("c")), tf(tl("d"), tl("e"), tl("f"))));
        b.fixPositionAndCheckSuccess(set("a", "b", "c"), "a", TargetPosition.FIRST);
        b.fixPositionAndCheckSuccess(set("a", "b", "c", "d", "e", "f"), "c", TargetPosition.LAST);
        b.fixPositionAndCheckSuccess(set("a", "b", "c", "d", "e", "f"), "d", TargetPosition.FIRST);
        b.fixPositionAndCheckConflict(set("a", "b", "c", "d", "e", "f"), "d", TargetPosition.LAST);
        assertEquals(Arrays.asList("d", "e", "f", "a", "b", "c"), b.get().getPossibleOrder());
    }

    @Test
    public void testFixedInReorderable3() {
        final PositionTreeTestHelper b = create(ta(tf(tl("a"), tl("b"), tl("c")), tf(tl("d"), tl("e"), tl("f"))));
        b.fixPositionAndCheckSuccess(set("d", "e", "f"), "f", TargetPosition.FIRST);
        b.fixPositionAndCheckSuccess(set("a", "b", "c", "d", "e", "f"), "c", TargetPosition.LAST);
        b.fixPositionAndCheckConflict(set("a", "b", "c", "d", "e", "f"), "d", TargetPosition.FIRST);
        assertEquals(Arrays.asList("f", "e", "d", "a", "b", "c"), b.get().getPossibleOrder());
    }

    @Test
    public void testConflictInMultiLayerFixed() {
        final PositionTreeTestHelper b = create(
                ta(
                    tf(
                        tl("a"),
                        tl("b"),
                        ta(
                            ta(
                                tl("c"),
                                tl("d")
                            ),
                            tl("e")
                        ),
                        tl("f"),
                        tl("g")
                    ),
                    tl("h")
                ));
        b.fixPositionAndCheckSuccess(set("a", "b"), "a", TargetPosition.FIRST);
        b.fixPositionAndCheckConflict(set("f", "g"), "g", TargetPosition.FIRST);
        assertEquals(Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h"), b.get().getPossibleOrder());
    }
}
