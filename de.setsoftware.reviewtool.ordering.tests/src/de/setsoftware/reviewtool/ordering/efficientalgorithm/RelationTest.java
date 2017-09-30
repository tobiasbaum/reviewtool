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
        return new MatchSet<>(new TreeSet<String>(Arrays.asList(s)));
    }

    private static PositionRequest<String> pr(TargetPosition pos, String... s) {
        return new PositionRequest<String>(ms(s), s[0], pos);
    }

    @Test
    public void testSimpleEquality() {
        final List<MatchSet<String>> ms = Collections.emptyList();
        final List<PositionRequest<String>> pr = Collections.emptyList();

        assertTrue(Relation.isBetterThanOrEqual(Arrays.asList("a", "b"), Arrays.asList("a", "b"), ms, pr));
        assertTrue(Relation.isBetterThanOrEqual(Arrays.asList("b", "a"), Arrays.asList("b", "a"), ms, pr));
        assertFalse(Relation.isBetterThanOrEqual(Arrays.asList("a", "b"), Arrays.asList("b", "a"), ms, pr));
        assertFalse(Relation.isBetterThanOrEqual(Arrays.asList("b", "a"), Arrays.asList("a", "b"), ms, pr));
    }

    @Test
    public void testSimplePositionCheck() {
        final List<MatchSet<String>> ms = Arrays.asList(ms("a", "b"));
        final List<PositionRequest<String>> pr = Arrays.asList(pr(TargetPosition.FIRST, "a", "b"));

        assertTrue(Relation.isBetterThanOrEqual(Arrays.asList("a", "b"), Arrays.asList("a", "b"), ms, pr));
        assertTrue(Relation.isBetterThanOrEqual(Arrays.asList("b", "a"), Arrays.asList("b", "a"), ms, pr));
        assertTrue(Relation.isBetterThanOrEqual(Arrays.asList("a", "b"), Arrays.asList("b", "a"), ms, pr));
        assertFalse(Relation.isBetterThanOrEqual(Arrays.asList("b", "a"), Arrays.asList("a", "b"), ms, pr));
    }

    @Test
    public void testSpacedApart() {
        final List<MatchSet<String>> ms = Arrays.asList(ms("a", "b"));
        final List<PositionRequest<String>> pr = Arrays.asList(pr(TargetPosition.FIRST, "a", "b"));

        assertTrue(Relation.isBetterThanOrEqual(Arrays.asList("a", "b", "c"), Arrays.asList("a", "c", "b"), ms, pr));
        assertTrue(Relation.isBetterThanOrEqual(Arrays.asList("b", "a", "c"), Arrays.asList("a", "c", "b"), ms, pr));
        assertTrue(Relation.isBetterThanOrEqual(Arrays.asList("a", "b", "c"), Arrays.asList("b", "a", "c"), ms, pr));
    }

    @Test
    public void testMultipleIncomparable() {
        final List<MatchSet<String>> ms = Arrays.asList(ms("a", "b"), ms("c", "d"));
        final List<PositionRequest<String>> pr = Collections.emptyList();

        checkBetter(Arrays.asList("a", "b", "c", "d"), Arrays.asList("c", "a", "b", "d"), ms, pr);
        checkBetter(Arrays.asList("a", "b", "c", "d"), Arrays.asList("a", "c", "d", "b"), ms, pr);
        checkIncomparable(Arrays.asList("c", "a", "b", "d"), Arrays.asList("a", "c", "d", "b"), ms, pr);
        checkBetter(Arrays.asList("c", "a", "b", "d"), Arrays.asList("c", "a", "d", "b"), ms, pr);
        checkBetter(Arrays.asList("a", "c", "d", "b"), Arrays.asList("c", "a", "d", "b"), ms, pr);
    }

    private static void checkBetter(
            List<String> better,
            List<String> worse,
            List<MatchSet<String>> ms,
            List<PositionRequest<String>> pr) {
        assertTrue(Relation.isBetterThanOrEqual(better, worse, ms, pr));
        assertFalse(Relation.isBetterThanOrEqual(worse, better, ms, pr));
    }

    private static void checkIncomparable(
            List<String> o1,
            List<String> o2,
            List<MatchSet<String>> ms,
            List<PositionRequest<String>> pr) {
        assertFalse(Relation.isBetterThanOrEqual(o1, o2, ms, pr));
        assertFalse(Relation.isBetterThanOrEqual(o2, o1, ms, pr));
    }

}
