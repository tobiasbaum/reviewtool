package de.setsoftware.reviewtool.ordering.efficientalgorithm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.junit.Test;

/**
 * Tests for {@link SubsettingSet}.
 */
public class SubsettingSetTest {

    private static MatchSet<String> ms(String... strings) {
        return new UnorderedMatchSet<>(Arrays.asList(strings));
    }

    @Test
    public void test1() {
        final SubsettingSet<String> s = new SubsettingSet<>(
                ms("a", "b", "c"), Collections.<MatchSet<String>>emptyList());
        assertTrue(s.contains("a"));
        assertTrue(s.contains("b"));
        assertTrue(s.contains("c"));
        assertFalse(s.contains("d"));
        assertEquals(s.toSet(), new HashSet<>(Arrays.asList("a", "b", "c")));
    }

    @Test
    public void test2() {
        final SubsettingSet<String> s = new SubsettingSet<>(
                ms("a", "b"), Arrays.asList(ms("c", "d"), ms("e", "f")));
        assertTrue(s.contains("a"));
        assertTrue(s.contains("b"));
        assertTrue(s.contains("c"));
        assertTrue(s.contains("d"));
        assertTrue(s.contains("e"));
        assertTrue(s.contains("f"));
        assertFalse(s.contains("g"));
        assertEquals(s.toSet(), new HashSet<>(Arrays.asList("a", "b", "c", "d", "e", "f")));
        assertEquals(s.potentialRemovals(), Arrays.asList(1, 0));
    }

    @Test
    public void test3() {
        final SubsettingSet<String> s = new SubsettingSet<>(
                ms("a", "b"), Arrays.asList(ms("c", "d"), ms("e", "f")));
        s.preliminaryRemove(0);
        assertTrue(s.contains("a"));
        assertTrue(s.contains("b"));
        assertFalse(s.contains("c"));
        assertFalse(s.contains("d"));
        assertTrue(s.contains("e"));
        assertTrue(s.contains("f"));
        assertFalse(s.contains("g"));
        assertEquals(s.toSet(), new HashSet<>(Arrays.asList("a", "b", "e", "f")));
    }

    @Test
    public void test4() {
        final SubsettingSet<String> s = new SubsettingSet<>(
                ms("a", "b"), Arrays.asList(ms("c", "d"), ms("e", "f")));
        s.preliminaryRemove(0);
        s.commitRemoval();
        assertTrue(s.contains("a"));
        assertTrue(s.contains("b"));
        assertFalse(s.contains("c"));
        assertFalse(s.contains("d"));
        assertTrue(s.contains("e"));
        assertTrue(s.contains("f"));
        assertFalse(s.contains("g"));
        assertEquals(s.toSet(), new HashSet<>(Arrays.asList("a", "b", "e", "f")));
        assertEquals(s.potentialRemovals(), Arrays.asList(1));
    }

    @Test
    public void test5() {
        final SubsettingSet<String> s = new SubsettingSet<>(
                ms("a", "b"), Arrays.asList(ms("c", "d"), ms("e", "f")));
        s.preliminaryRemove(0);
        s.rollbackRemoval();
        assertTrue(s.contains("a"));
        assertTrue(s.contains("b"));
        assertTrue(s.contains("c"));
        assertTrue(s.contains("d"));
        assertTrue(s.contains("e"));
        assertTrue(s.contains("f"));
        assertFalse(s.contains("g"));
        assertEquals(s.toSet(), new HashSet<>(Arrays.asList("a", "b", "c", "d", "e", "f")));
        assertEquals(s.potentialRemovals(), Arrays.asList(1, 0));
    }

    @Test
    public void test6() {
        final SubsettingSet<String> s = new SubsettingSet<>(
                ms("a", "b"), Arrays.asList(ms("b", "c"), ms("c", "d")));
        s.preliminaryRemove(0);
        assertTrue(s.contains("a"));
        assertTrue(s.contains("b"));
        assertTrue(s.contains("c"));
        assertTrue(s.contains("d"));
        assertFalse(s.contains("f"));
        assertEquals(s.toSet(), new HashSet<>(Arrays.asList("a", "b", "c", "d")));
    }

    @Test
    public void test7() {
        final SubsettingSet<String> s = new SubsettingSet<>(
                ms("a", "b"), Arrays.asList(ms("b", "c"), ms("c", "d")));
        s.preliminaryRemove(0);
        s.commitRemoval();
        s.preliminaryRemove(1);
        assertTrue(s.contains("a"));
        assertTrue(s.contains("b"));
        assertFalse(s.contains("c"));
        assertFalse(s.contains("d"));
        assertFalse(s.contains("f"));
        assertEquals(s.toSet(), new HashSet<>(Arrays.asList("a", "b")));
    }

}
