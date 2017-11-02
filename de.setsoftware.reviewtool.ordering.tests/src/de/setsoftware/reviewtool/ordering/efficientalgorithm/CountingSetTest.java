package de.setsoftware.reviewtool.ordering.efficientalgorithm;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.junit.Test;

/**
 * Test for {@ink CountingSet}.
 */
public class CountingSetTest {

    @Test
    public void testEmpty() {
        final CountingSet<String> s = new CountingSet<>();
        assertEquals(0, s.get("a"));
        assertEquals(0, s.get("b"));
    }

    @Test
    public void testAddOnce() {
        final CountingSet<String> s = new CountingSet<>();
        s.addAll(Collections.singleton("a"));
        assertEquals(1, s.get("a"));
        assertEquals(0, s.get("b"));
    }

    @Test
    public void testAddMultiple() {
        final CountingSet<String> s = new CountingSet<>();
        s.addAll(new HashSet<>(Arrays.asList("a", "b")));
        s.addAll(new HashSet<>(Arrays.asList("b", "c")));
        assertEquals(1, s.get("a"));
        assertEquals(2, s.get("b"));
        assertEquals(1, s.get("c"));
        assertEquals(0, s.get("d"));
    }

    @Test
    public void testSmokeTest() {
        final Random r = new Random(42);
        for (int i = 0; i < 100; i++) {
            final Map<Integer, Integer> expected = new HashMap<>();
            final CountingSet<Integer> actual = new CountingSet<>();
            final int max = r.nextInt(100) + 5;
            for (int j = 0; j < max; j++) {
                final int val = r.nextInt(1000) - 200;
                actual.addAll(Collections.singleton(val));
                if (expected.containsKey(val)) {
                    expected.put(val, expected.get(val) + 1);
                } else {
                    expected.put(val, 1);
                }
            }

            assertEquals(expected.keySet(), actual.keys());
            for (final Entry<Integer, Integer> e : expected.entrySet()) {
                assertEquals(e.getValue().intValue(), actual.get(e.getKey()));
            }
        }
    }

}
