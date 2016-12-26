package de.setsoftware.reviewtool.ordering.basealgorithm;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.Test;

public class PartialOrdersTest {

    private static final Set<Integer> set(Integer... ints) {
        return new LinkedHashSet<>(Arrays.asList(ints));
    }

    @Test
    public void testDetermineMinElementsEmpty() {
        assertEquals(
                set(),
                PartialOrders.determineMinElements(Collections.<Integer>emptyList(), new DivOrder()));
    }

    @Test
    public void testDetermineMinElementsOneElement() {
        assertEquals(
                set(5),
                PartialOrders.determineMinElements(Arrays.asList(5), new DivOrder()));
    }

    @Test
    public void testDetermineMinElementsTwoElements() {
        assertEquals(
                set(1),
                PartialOrders.determineMinElements(Arrays.asList(1, 2), new DivOrder()));
    }

    @Test
    public void testDetermineMinElementsThreeElements() {
        assertEquals(
                set(2),
                PartialOrders.determineMinElements(Arrays.asList(6, 4, 2), new DivOrder()));
    }

    @Test
    public void testDetermineMinElementsIncomparableElements() {
        assertEquals(
                set(4, 6),
                PartialOrders.determineMinElements(Arrays.asList(4, 6), new DivOrder()));
    }

}
