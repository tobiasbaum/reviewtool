package de.setsoftware.reviewtool.model.changestructure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FragmentTest {

    private static FileInRevision file() {
        return new FileInRevision("file", new LocalRevision(), StubRepo.INSTANCE);
    }

    private static PositionInText pos(int line, int col) {
        return new PositionInText(line, col);
    }

    @Test
    public void testCanBeMerged1() {
        final Fragment f1 = new Fragment(file(), pos(1, 1), pos(3, 0), "z1\nz2\n");
        final Fragment f2 = new Fragment(file(), pos(1, 1), pos(3, 0), "z1\nz2\n");
        assertTrue(f1.canBeMergedWith(f2));
    }

    @Test
    public void testCanBeMerged2() {
        final Fragment f1 = new Fragment(file(), pos(1, 1), pos(3, 0), "z1\nz2\n");
        final Fragment f2 = new Fragment(file(), pos(2, 1), pos(3, 0), "z2\n");
        assertTrue(f1.canBeMergedWith(f2));
    }

    @Test
    public void testCanBeMerged3() {
        final Fragment f1 = new Fragment(file(), pos(1, 1), pos(3, 0), "z123456789\nz2\n");
        final Fragment f2 = new Fragment(file(), pos(1, 3), pos(1, 5), "23");
        assertTrue(f1.canBeMergedWith(f2));
    }

    @Test
    public void testCanBeMerged4() {
        final Fragment f1 = new Fragment(file(), pos(1, 1), pos(3, 0), "z1\nz2\n");
        final Fragment f2 = new Fragment(file(), pos(2, 1), pos(7, 0), "z2\nz3\nz4\nz5\nz6");
        assertTrue(f1.canBeMergedWith(f2));
    }

    @Test
    public void testCanBeMerged5() {
        final Fragment f1 = new Fragment(file(), pos(1, 1), pos(3, 0), "z1\nz2\n");
        final Fragment f2 = new Fragment(file(), pos(3, 1), pos(7, 0), "z3\nz4\nz5\nz6");
        assertTrue(f1.canBeMergedWith(f2));
        assertTrue(f2.canBeMergedWith(f1));
    }

    @Test
    public void testCanBeMerged6() {
        final Fragment f1 = new Fragment(file(), pos(1, 1), pos(3, 0), "z1\nz2\n");
        final Fragment f2 = new Fragment(file(), pos(4, 1), pos(7, 0), "z4\nz5\nz6");
        assertFalse(f1.canBeMergedWith(f2));
        assertFalse(f2.canBeMergedWith(f1));
    }

    @Test
    public void testCanBeMerged7() {
        final Fragment f1 = new Fragment(file(), pos(3, 1), pos(5, 0), "z3\nz4\n");
        final Fragment f2 = new Fragment(file(), pos(2, 1), pos(7, 0), "z2\nz3\nz4\nz5\nz6");
        assertTrue(f1.canBeMergedWith(f2));
        assertTrue(f2.canBeMergedWith(f1));
    }

    @Test
    public void testCanBeMerged8() {
        final Fragment f1 = new Fragment(file(), pos(1, 10), pos(1, 13), "ab");
        final Fragment f2 = new Fragment(file(), pos(1, 20), pos(1, 23), "xy");
        assertFalse(f1.canBeMergedWith(f2));
        assertFalse(f2.canBeMergedWith(f1));
    }

    @Test
    public void testCanBeMergedWithDeletionFragment1() {
        final Fragment f1 = new Fragment(file(), pos(1, 1), pos(1, 0), "");
        final Fragment f2 = new Fragment(file(), pos(1, 1), pos(1, 0), "");
        assertTrue(f1.canBeMergedWith(f2));
    }

    @Test
    public void testCanBeMergedWithDeletionFragment2() {
        final Fragment f1 = new Fragment(file(), pos(1, 1), pos(1, 0), "");
        final Fragment f2 = new Fragment(file(), pos(2, 1), pos(2, 0), "");
        assertFalse(f1.canBeMergedWith(f2));
        assertFalse(f2.canBeMergedWith(f1));
    }

    @Test
    public void testCanBeMergedWithDeletionFragment3() {
        final Fragment f1 = new Fragment(file(), pos(1, 1), pos(1, 0), "");
        final Fragment f2 = new Fragment(file(), pos(3, 1), pos(3, 0), "");
        assertFalse(f1.canBeMergedWith(f2));
        assertFalse(f2.canBeMergedWith(f1));
    }

    @Test
    public void testCanBeMergedWithDeletionFragment4() {
        final Fragment f1 = new Fragment(file(), pos(1, 1), pos(5, 0), "A\nB\nC\nD\n");
        final Fragment f2 = new Fragment(file(), pos(5, 1), pos(5, 0), "");
        assertTrue(f1.canBeMergedWith(f2));
        assertTrue(f2.canBeMergedWith(f1));
    }

    @Test
    public void testCanBeMergedWithDeletionFragment5() {
        final Fragment f1 = new Fragment(file(), pos(1, 1), pos(5, 0), "A\nB\nC\nD\n");
        final Fragment f2 = new Fragment(file(), pos(1, 1), pos(1, 0), "");
        assertTrue(f1.canBeMergedWith(f2));
        assertTrue(f2.canBeMergedWith(f1));
    }


    private static void testMergeSymmetric(Fragment f1, Fragment f2, Fragment expected) {
        assertEquals(expected, f1.merge(f2));
        assertEquals(expected, f2.merge(f1));
    }

    @Test
    public void testMerge1() {
        final Fragment f1 = new Fragment(file(), pos(1, 1), pos(3, 0), "z1\nz2\n");
        final Fragment f2 = new Fragment(file(), pos(1, 1), pos(3, 0), "z1\nz2\n");
        testMergeSymmetric(f1, f2, new Fragment(file(), pos(1, 1), pos(3, 0), "z1\nz2\n"));
    }

    @Test
    public void testMerge2() {
        final Fragment f1 = new Fragment(file(), pos(1, 1), pos(3, 0), "z1\nz2\n");
        final Fragment f2 = new Fragment(file(), pos(2, 1), pos(3, 0), "z2\n");
        testMergeSymmetric(f1, f2, new Fragment(file(), pos(1, 1), pos(3, 0), "z1\nz2\n"));
    }

    @Test
    public void testMerge3() {
        final Fragment f1 = new Fragment(file(), pos(1, 1), pos(3, 0), "z123456789\nz2\n");
        final Fragment f2 = new Fragment(file(), pos(1, 3), pos(1, 5), "23");
        testMergeSymmetric(f1, f2, new Fragment(file(), pos(1, 1), pos(3, 0), "z123456789\nz2\n"));
    }

    @Test
    public void testMerge4() {
        final Fragment f1 = new Fragment(file(), pos(1, 1), pos(3, 0), "z1\nz2\n");
        final Fragment f2 = new Fragment(file(), pos(2, 1), pos(7, 0), "z2\nz3\nz4\nz5\nz6");
        testMergeSymmetric(f1, f2, new Fragment(file(), pos(1, 1), pos(7, 0), "z1\nz2\nz3\nz4\nz5\nz6"));
    }

    @Test
    public void testMerge5() {
        final Fragment f1 = new Fragment(file(), pos(1, 1), pos(3, 0), "z1\nz2\n");
        final Fragment f2 = new Fragment(file(), pos(3, 1), pos(7, 0), "z3\nz4\nz5\nz6");
        testMergeSymmetric(f1, f2, new Fragment(file(), pos(1, 1), pos(7, 0), "z1\nz2\nz3\nz4\nz5\nz6"));
    }

    @Test
    public void testMerge7() {
        final Fragment f1 = new Fragment(file(), pos(3, 1), pos(5, 0), "z3\nz4\n");
        final Fragment f2 = new Fragment(file(), pos(2, 1), pos(7, 0), "z2\nz3\nz4\nz5\nz6");
        testMergeSymmetric(f1, f2, new Fragment(file(), pos(2, 1), pos(7, 0), "z2\nz3\nz4\nz5\nz6"));
    }

    @Test
    public void testMerge8() {
        final Fragment f1 = new Fragment(file(), pos(3, 1), pos(4, 6), "z3\nabcde");
        final Fragment f2 = new Fragment(file(), pos(4, 3), pos(5, 0), "cdefgh\n");
        testMergeSymmetric(f1, f2, new Fragment(file(), pos(3, 1), pos(5, 0), "z3\nabcdefgh\n"));
    }

    @Test
    public void testMergeWithDeletionFragment1() {
        final Fragment f1 = new Fragment(file(), pos(1, 1), pos(1, 0), "");
        final Fragment f2 = new Fragment(file(), pos(1, 1), pos(1, 0), "");
        testMergeSymmetric(f1, f2, new Fragment(file(), pos(1, 1), pos(1, 0), ""));
    }

    @Test
    public void testMergeWithDeletionFragment4() {
        final Fragment f1 = new Fragment(file(), pos(1, 1), pos(5, 0), "A\nB\nC\nD\n");
        final Fragment f2 = new Fragment(file(), pos(5, 1), pos(5, 0), "");
        testMergeSymmetric(f1, f2, new Fragment(file(), pos(1, 1), pos(5, 0), "A\nB\nC\nD\n"));
    }

    @Test
    public void testMergeWithDeletionFragment5() {
        final Fragment f1 = new Fragment(file(), pos(1, 1), pos(5, 0), "A\nB\nC\nD\n");
        final Fragment f2 = new Fragment(file(), pos(1, 1), pos(1, 0), "");
        testMergeSymmetric(f1, f2, new Fragment(file(), pos(1, 1), pos(5, 0), "A\nB\nC\nD\n"));
    }
}
