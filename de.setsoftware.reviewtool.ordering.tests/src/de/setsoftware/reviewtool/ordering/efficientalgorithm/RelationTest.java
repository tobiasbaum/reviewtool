package de.setsoftware.reviewtool.ordering.efficientalgorithm;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

import org.junit.Test;

/**
 * Tests for {@link Relation}.
 */
public class RelationTest {

    private static MatchSet<String> ms(String... s) {
        return new UnorderedMatchSet<>(new TreeSet<>(Arrays.asList(s)));
    }

    private static MatchSet<String> pr(String... s) {
        return new StarMatchSet<>(s[0], new TreeSet<>(Arrays.asList(s)));
    }

    @Test
    public void testSimpleEquality() {
        final List<MatchSet<String>> ms = Collections.emptyList();

        assertTrue(Relation.isBetterThanOrEqual(Arrays.asList("a", "b"), Arrays.asList("a", "b"), ms));
        assertTrue(Relation.isBetterThanOrEqual(Arrays.asList("b", "a"), Arrays.asList("b", "a"), ms));
        assertFalse(Relation.isBetterThanOrEqual(Arrays.asList("a", "b"), Arrays.asList("b", "a"), ms));
        assertFalse(Relation.isBetterThanOrEqual(Arrays.asList("b", "a"), Arrays.asList("a", "b"), ms));
    }

    @Test
    public void testSimplePositionCheck() {
        final List<MatchSet<String>> ms = Arrays.asList(pr("a", "b"));

        assertTrue(Relation.isBetterThanOrEqual(Arrays.asList("a", "b"), Arrays.asList("a", "b"), ms));
        assertTrue(Relation.isBetterThanOrEqual(Arrays.asList("b", "a"), Arrays.asList("b", "a"), ms));
        assertTrue(Relation.isBetterThanOrEqual(Arrays.asList("a", "b"), Arrays.asList("b", "a"), ms));
        assertFalse(Relation.isBetterThanOrEqual(Arrays.asList("b", "a"), Arrays.asList("a", "b"), ms));
    }

    @Test
    public void testSpacedApart() {
        final List<MatchSet<String>> ms = Arrays.asList(pr("a", "b"));

        assertTrue(Relation.isBetterThanOrEqual(Arrays.asList("a", "b", "c"), Arrays.asList("a", "c", "b"), ms));
        assertTrue(Relation.isBetterThanOrEqual(Arrays.asList("b", "a", "c"), Arrays.asList("a", "c", "b"), ms));
        assertTrue(Relation.isBetterThanOrEqual(Arrays.asList("a", "b", "c"), Arrays.asList("b", "a", "c"), ms));
    }

    @Test
    public void testMultipleIncomparable() {
        final List<MatchSet<String>> ms = Arrays.asList(ms("a", "b"), ms("c", "d"));

        checkBetter(Arrays.asList("a", "b", "c", "d"), Arrays.asList("c", "a", "b", "d"), ms);
        checkBetter(Arrays.asList("a", "b", "c", "d"), Arrays.asList("a", "c", "d", "b"), ms);
        checkIncomparable(Arrays.asList("c", "a", "b", "d"), Arrays.asList("a", "c", "d", "b"), ms);
        checkBetter(Arrays.asList("c", "a", "b", "d"), Arrays.asList("c", "a", "d", "b"), ms);
        checkBetter(Arrays.asList("a", "c", "d", "b"), Arrays.asList("c", "a", "d", "b"), ms);
    }

    private static void checkBetter(
            List<String> better,
            List<String> worse,
            List<MatchSet<String>> ms) {
        assertTrue(Relation.isBetterThanOrEqual(better, worse, ms));
        assertFalse(Relation.isBetterThanOrEqual(worse, better, ms));
    }

    private static void checkIncomparable(
            List<String> o1,
            List<String> o2,
            List<MatchSet<String>> ms) {
        assertFalse(Relation.isBetterThanOrEqual(o1, o2, ms));
        assertFalse(Relation.isBetterThanOrEqual(o2, o1, ms));
    }

}
