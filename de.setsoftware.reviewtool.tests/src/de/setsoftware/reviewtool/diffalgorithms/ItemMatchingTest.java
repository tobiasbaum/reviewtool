package de.setsoftware.reviewtool.diffalgorithms;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ItemMatchingTest {

    private static FullFileView<String> testfile() {
        return new FullFileView<>(new String[100]);
    }

    private static void match(ItemMatching<String> m, int line1, int line2) {
        m.match(testfile(), line1, testfile(), line2);
    }

    @Test
    public void testRemoveIncompatibleMatchingsWithEmptyMatching() {
        final ItemMatching<String> m = new ItemMatching<>(testfile(), testfile());

        m.removeIncompatibleMatchings();

        assertEquals("{}", m.toString());
    }

    @Test
    public void testRemoveIncompatibleMatchingsWithoutNeedForRemoves() {
        final ItemMatching<String> m = new ItemMatching<>(testfile(), testfile());
        match(m, 1, 1);
        match(m, 2, 2);
        match(m, 3, 3);

        m.removeIncompatibleMatchings();

        assertEquals("{1=1, 2=2, 3=3}", m.toString());
    }

    @Test
    public void testRemoveIncompatibleMatchingsWithSimpleConflict() {
        final ItemMatching<String> m = new ItemMatching<>(testfile(), testfile());
        match(m, 1, 2);
        match(m, 2, 1);
        match(m, 3, 3);

        m.removeIncompatibleMatchings();

        assertEquals("{2=1, 3=3}", m.toString());
    }

    @Test
    public void testRemoveIncompatibleMatchingsWithMultipleConflict() {
        final ItemMatching<String> m = new ItemMatching<>(testfile(), testfile());
        match(m, 1, 4);
        match(m, 2, 2);
        match(m, 3, 3);
        match(m, 4, 1);

        m.removeIncompatibleMatchings();

        assertEquals("{2=2, 3=3}", m.toString());
    }

}
