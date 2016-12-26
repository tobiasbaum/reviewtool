package de.setsoftware.reviewtool.ordering.basealgorithm;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LexicographicOrderTest {

    @Test
    public void testTwoEmpty() {
        final LexicographicOrder<Integer> o = new LexicographicOrder<>(new NaturalOrder<Integer>());
        assertTrue(o.isLessOrEquals(new Integer[0], new Integer[0]));
    }

    @Test
    public void testOneEmpty() {
        final LexicographicOrder<Integer> o = new LexicographicOrder<>(new NaturalOrder<Integer>());
        assertTrue(o.isLessOrEquals(new Integer[0], new Integer[] {1}));
        assertFalse(o.isLessOrEquals(new Integer[] {1}, new Integer[0]));
    }

    @Test
    public void testOneElement() {
        final LexicographicOrder<Integer> o = new LexicographicOrder<>(new NaturalOrder<Integer>());
        assertTrue(o.isLessOrEquals(new Integer[] {1}, new Integer[] {1}));
        assertTrue(o.isLessOrEquals(new Integer[] {1}, new Integer[] {2}));
        assertFalse(o.isLessOrEquals(new Integer[] {2}, new Integer[] {1}));
    }

    @Test
    public void testTwoElements() {
        final LexicographicOrder<Integer> o = new LexicographicOrder<>(new NaturalOrder<Integer>());
        assertTrue(o.isLessOrEquals(new Integer[] {1, 2}, new Integer[] {1, 2}));
        assertTrue(o.isLessOrEquals(new Integer[] {1, 2}, new Integer[] {1, 3}));
        assertTrue(o.isLessOrEquals(new Integer[] {1, 2}, new Integer[] {2, 1}));
        assertFalse(o.isLessOrEquals(new Integer[] {2, 2}, new Integer[] {1, 2}));
    }

}
