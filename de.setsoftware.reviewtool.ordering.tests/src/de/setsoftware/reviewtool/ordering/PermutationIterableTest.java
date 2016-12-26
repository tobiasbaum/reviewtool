package de.setsoftware.reviewtool.ordering;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.Test;

import de.setsoftware.reviewtool.ordering.basealgorithm.Tour;

public class PermutationIterableTest {

    private static final Set<Integer> set(Integer... ints) {
        return new LinkedHashSet<>(Arrays.asList(ints));
    }

    private static String printAll(PermutationIterable<Integer> p) {
        final StringBuilder b = new StringBuilder();
        for (final Tour<Integer> t : p) {
            b.append(t).append("\n");
        }
        return b.toString();
    }

    @Test
    public void testPermutationIterableEmpty() {
        final PermutationIterable<Integer> p = new PermutationIterable<>(set());
        assertEquals("", printAll(p));
    }

    @Test
    public void testPermutationIterableOneElement() {
        final PermutationIterable<Integer> p = new PermutationIterable<>(set(1));
        assertEquals("[1]\n", printAll(p));
    }

    @Test
    public void testPermutationIterableTwoElements() {
        final PermutationIterable<Integer> p = new PermutationIterable<>(set(1, 2));
        assertEquals(
                "[1, 2]\n" +
                "[2, 1]\n",
                printAll(p));
    }

    @Test
    public void testPermutationIterableThreeElements() {
        final PermutationIterable<Integer> p = new PermutationIterable<>(set(1, 2, 3));
        assertEquals(
                "[1, 2, 3]\n" +
                "[1, 3, 2]\n" +
                "[3, 1, 2]\n" +
                "[3, 2, 1]\n" +
                "[2, 3, 1]\n" +
                "[2, 1, 3]\n",
                printAll(p));
    }

}
