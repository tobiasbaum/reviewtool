package de.setsoftware.reviewtool.ordering.relationtypes;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class RelatednessVectorTest {

    @Test
    public void testCompareTo() {
        final RelatednessVector v1 = new RelatednessVector(new double[] {0.0, 0.0, 0.0, 0.0});
        final RelatednessVector v2 = new RelatednessVector(new double[] {0.0, 0.0, 1.0, 0.0});
        final RelatednessVector v3 = new RelatednessVector(new double[] {0.0, 0.0, 1.0, 1.0});
        final RelatednessVector v4 = new RelatednessVector(new double[] {0.0, 1.0, 0.0, 0.0});

        assertEquals(0, v1.compareTo(v1));
        assertEquals(1, v1.compareTo(v2));
        assertEquals(1, v1.compareTo(v3));
        assertEquals(1, v1.compareTo(v4));
        assertEquals(-1, v2.compareTo(v1));
        assertEquals(0, v2.compareTo(v2));
        assertEquals(1, v2.compareTo(v3));
        assertEquals(1, v2.compareTo(v4));
        assertEquals(-1, v3.compareTo(v1));
        assertEquals(-1, v3.compareTo(v2));
        assertEquals(0, v3.compareTo(v3));
        assertEquals(1, v3.compareTo(v4));
        assertEquals(-1, v4.compareTo(v1));
        assertEquals(-1, v4.compareTo(v2));
        assertEquals(-1, v4.compareTo(v3));
        assertEquals(0, v4.compareTo(v4));
    }

}
