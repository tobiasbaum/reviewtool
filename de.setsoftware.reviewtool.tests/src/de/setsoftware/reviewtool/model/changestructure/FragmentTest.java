package de.setsoftware.reviewtool.model.changestructure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;

import org.junit.Test;

/**
 * Tests for {@link Fragment}.
 */
public class FragmentTest {

    private static FileInRevision file() {
        return new FileInRevision("file", new LocalRevision(), StubRepo.INSTANCE);
    }

    private static FileInRevision fileWithContent(final String content) {
        return new FileInRevision("file", new LocalRevision(), StubRepo.INSTANCE) {
            @Override
            public byte[] getContents() {
                try {
                    return content.getBytes("UTF-8");
                } catch (final UnsupportedEncodingException e) {
                    throw new AssertionError();
                }
            }
        };
    }

    private static PositionInText pos(int line, int col) {
        return new PositionInText(line, col);
    }

    @Test
    public void testCanBeMerged1() {
        final Fragment f1 = new Fragment(file(), pos(1, 1), pos(3, 0));
        final Fragment f2 = new Fragment(file(), pos(1, 1), pos(3, 0));
        assertTrue(f1.canBeMergedWith(f2));
    }

    @Test
    public void testCanBeMerged2() {
        final Fragment f1 = new Fragment(file(), pos(1, 1), pos(3, 0));
        final Fragment f2 = new Fragment(file(), pos(2, 1), pos(3, 0));
        assertTrue(f1.canBeMergedWith(f2));
    }

    @Test
    public void testCanBeMerged3() {
        final Fragment f1 = new Fragment(file(), pos(1, 1), pos(3, 0));
        final Fragment f2 = new Fragment(file(), pos(1, 3), pos(1, 5));
        assertTrue(f1.canBeMergedWith(f2));
    }

    @Test
    public void testCanBeMerged4() {
        final Fragment f1 = new Fragment(file(), pos(1, 1), pos(3, 0));
        final Fragment f2 = new Fragment(file(), pos(2, 1), pos(7, 0));
        assertTrue(f1.canBeMergedWith(f2));
    }

    @Test
    public void testCanBeMerged5() {
        final Fragment f1 = new Fragment(file(), pos(1, 1), pos(3, 0));
        final Fragment f2 = new Fragment(file(), pos(3, 1), pos(7, 0));
        assertTrue(f1.canBeMergedWith(f2));
        assertTrue(f2.canBeMergedWith(f1));
    }

    @Test
    public void testCanBeMerged6() {
        final Fragment f1 = new Fragment(file(), pos(1, 1), pos(3, 0));
        final Fragment f2 = new Fragment(file(), pos(4, 1), pos(7, 0));
        assertFalse(f1.canBeMergedWith(f2));
        assertFalse(f2.canBeMergedWith(f1));
    }

    @Test
    public void testCanBeMerged7() {
        final Fragment f1 = new Fragment(file(), pos(3, 1), pos(5, 0));
        final Fragment f2 = new Fragment(file(), pos(2, 1), pos(7, 0));
        assertTrue(f1.canBeMergedWith(f2));
        assertTrue(f2.canBeMergedWith(f1));
    }

    @Test
    public void testCanBeMerged8() {
        final Fragment f1 = new Fragment(file(), pos(1, 10), pos(1, 13));
        final Fragment f2 = new Fragment(file(), pos(1, 20), pos(1, 23));
        assertFalse(f1.canBeMergedWith(f2));
        assertFalse(f2.canBeMergedWith(f1));
    }

    @Test
    public void testCanBeMergedWithDeletionFragment1() {
        final Fragment f1 = new Fragment(file(), pos(1, 1), pos(1, 0));
        final Fragment f2 = new Fragment(file(), pos(1, 1), pos(1, 0));
        assertTrue(f1.canBeMergedWith(f2));
    }

    @Test
    public void testCanBeMergedWithDeletionFragment2() {
        final Fragment f1 = new Fragment(file(), pos(1, 1), pos(1, 0));
        final Fragment f2 = new Fragment(file(), pos(2, 1), pos(2, 0));
        assertFalse(f1.canBeMergedWith(f2));
        assertFalse(f2.canBeMergedWith(f1));
    }

    @Test
    public void testCanBeMergedWithDeletionFragment3() {
        final Fragment f1 = new Fragment(file(), pos(1, 1), pos(1, 0));
        final Fragment f2 = new Fragment(file(), pos(3, 1), pos(3, 0));
        assertFalse(f1.canBeMergedWith(f2));
        assertFalse(f2.canBeMergedWith(f1));
    }

    @Test
    public void testCanBeMergedWithDeletionFragment4() {
        final Fragment f1 = new Fragment(file(), pos(1, 1), pos(5, 0));
        final Fragment f2 = new Fragment(file(), pos(5, 1), pos(5, 0));
        assertTrue(f1.canBeMergedWith(f2));
        assertTrue(f2.canBeMergedWith(f1));
    }

    @Test
    public void testCanBeMergedWithDeletionFragment5() {
        final Fragment f1 = new Fragment(file(), pos(1, 1), pos(5, 0));
        final Fragment f2 = new Fragment(file(), pos(1, 1), pos(1, 0));
        assertTrue(f1.canBeMergedWith(f2));
        assertTrue(f2.canBeMergedWith(f1));
    }


    private static void testMergeSymmetric(Fragment f1, Fragment f2, Fragment expected) {
        assertEquals(expected, f1.merge(f2));
        assertEquals(expected, f2.merge(f1));
    }

    @Test
    public void testMerge1() {
        final Fragment f1 = new Fragment(file(), pos(1, 1), pos(3, 0));
        final Fragment f2 = new Fragment(file(), pos(1, 1), pos(3, 0));
        testMergeSymmetric(f1, f2, new Fragment(file(), pos(1, 1), pos(3, 0)));
    }

    @Test
    public void testMerge2() {
        final Fragment f1 = new Fragment(file(), pos(1, 1), pos(3, 0));
        final Fragment f2 = new Fragment(file(), pos(2, 1), pos(3, 0));
        testMergeSymmetric(f1, f2, new Fragment(file(), pos(1, 1), pos(3, 0), f1, f2));
    }

    @Test
    public void testMerge3() {
        final Fragment f1 = new Fragment(file(), pos(1, 1), pos(3, 0));
        final Fragment f2 = new Fragment(file(), pos(1, 3), pos(1, 5));
        testMergeSymmetric(f1, f2, new Fragment(file(), pos(1, 1), pos(3, 0), f1, f2));
    }

    @Test
    public void testMerge4() {
        final Fragment f1 = new Fragment(file(), pos(1, 1), pos(3, 0));
        final Fragment f2 = new Fragment(file(), pos(2, 1), pos(7, 0));
        testMergeSymmetric(f1, f2, new Fragment(file(), pos(1, 1), pos(7, 0), f1, f2));
    }

    @Test
    public void testMerge5() {
        final Fragment f1 = new Fragment(file(), pos(1, 1), pos(3, 0));
        final Fragment f2 = new Fragment(file(), pos(3, 1), pos(7, 0));
        testMergeSymmetric(f1, f2, new Fragment(file(), pos(1, 1), pos(7, 0), f1, f2));
    }

    @Test
    public void testMerge7() {
        final Fragment f1 = new Fragment(file(), pos(3, 1), pos(5, 0));
        final Fragment f2 = new Fragment(file(), pos(2, 1), pos(7, 0));
        testMergeSymmetric(f1, f2, new Fragment(file(), pos(2, 1), pos(7, 0), f1, f2));
    }

    @Test
    public void testMerge8() {
        final Fragment f1 = new Fragment(file(), pos(3, 1), pos(4, 6));
        final Fragment f2 = new Fragment(file(), pos(4, 3), pos(5, 0));
        testMergeSymmetric(f1, f2, new Fragment(file(), pos(3, 1), pos(5, 0), f1, f2));
    }

    @Test
    public void testMergeWithDeletionFragment1() {
        final Fragment f1 = new Fragment(file(), pos(1, 1), pos(1, 0));
        final Fragment f2 = new Fragment(file(), pos(1, 1), pos(1, 0));
        testMergeSymmetric(f1, f2, new Fragment(file(), pos(1, 1), pos(1, 0)));
    }

    @Test
    public void testMergeWithDeletionFragment4() {
        final Fragment f1 = new Fragment(file(), pos(1, 1), pos(5, 0));
        final Fragment f2 = new Fragment(file(), pos(5, 1), pos(5, 0));
        testMergeSymmetric(f1, f2, new Fragment(file(), pos(1, 1), pos(5, 0), f1, f2));
    }

    @Test
    public void testMergeWithDeletionFragment5() {
        final Fragment f1 = new Fragment(file(), pos(1, 1), pos(5, 0));
        final Fragment f2 = new Fragment(file(), pos(1, 1), pos(1, 0));
        testMergeSymmetric(f1, f2, new Fragment(file(), pos(1, 1), pos(5, 0), f1, f2));
    }

    @Test
    public void testIsNeighboring1() {
        final Fragment f1 = new Fragment(file(), pos(1, 1), pos(3, 0));
        final Fragment f2 = new Fragment(file(), pos(1, 1), pos(3, 0));
        assertFalse(f1.isNeighboring(f2));
    }

    @Test
    public void testIsNeighboring2() {
        final Fragment f1 = new Fragment(file(), pos(1, 1), pos(3, 0));
        final Fragment f2 = new Fragment(file(), pos(3, 1), pos(6, 0));
        assertTrue(f1.isNeighboring(f2));
    }

    @Test
    public void testIsNeighboring3() {
        final Fragment f1 = new Fragment(file(), pos(1, 1), pos(3, 0));
        final Fragment f2 = new Fragment(file(), pos(3, 1), pos(6, 0));
        assertTrue(f2.isNeighboring(f1));
    }

    @Test
    public void testIsNeighboring4() {
        final Fragment f1 = new Fragment(file(), pos(1, 1), pos(3, 0));
        final Fragment f2 = new Fragment(file(), pos(4, 1), pos(7, 0));
        assertFalse(f1.isNeighboring(f2));
    }

    @Test
    public void testIsNeighboring5() {
        final Fragment f1 = new Fragment(file(), pos(1, 1), pos(3, 0));
        final Fragment f2 = new Fragment(file(), pos(4, 1), pos(7, 0));
        assertFalse(f2.isNeighboring(f1));
    }

    @Test
    public void testIsNeighboring6() {
        final Fragment f1 = new Fragment(file(), pos(1, 1), pos(1, 3));
        final Fragment f2 = new Fragment(file(), pos(1, 4), pos(1, 6));
        assertTrue(f2.isNeighboring(f1));
    }

    @Test
    public void testIsNeighboring7() {
        final Fragment f1 = new Fragment(file(), pos(1, 1), pos(1, 3));
        final Fragment f2 = new Fragment(file(), pos(1, 4), pos(1, 6));
        assertTrue(f1.isNeighboring(f2));
    }

    @Test
    public void testIsNeighboring8() {
        final Fragment f1 = new Fragment(file(), pos(1, 1), pos(1, 3));
        final Fragment f2 = new Fragment(file(), pos(1, 3), pos(1, 5));
        assertFalse(f2.isNeighboring(f1));
    }

    @Test
    public void testIsNeighboring9() {
        final Fragment f1 = new Fragment(file(), pos(1, 1), pos(1, 3));
        final Fragment f2 = new Fragment(file(), pos(1, 3), pos(1, 5));
        assertFalse(f1.isNeighboring(f2));
    }

    @Test
    public void testIsNeighboring10() {
        final Fragment f1 = new Fragment(file(), pos(1, 1), pos(1, 3));
        final Fragment f2 = new Fragment(file(), pos(1, 5), pos(1, 7));
        assertFalse(f2.isNeighboring(f1));
    }

    @Test
    public void testIsNeighboring11() {
        final Fragment f1 = new Fragment(file(), pos(1, 1), pos(1, 3));
        final Fragment f2 = new Fragment(file(), pos(1, 5), pos(1, 7));
        assertFalse(f1.isNeighboring(f2));
    }

    @Test
    public void testGetNumberOfLines1() {
        final Fragment f1 = new Fragment(file(), pos(1, 1), pos(2, 0));
        assertEquals(1, f1.getNumberOfLines());
    }

    @Test
    public void testGetNumberOfLines2() {
        final Fragment f1 = new Fragment(file(), pos(1, 1), pos(3, 0));
        assertEquals(2, f1.getNumberOfLines());
    }

    @Test
    public void testGetNumberOfLines3() {
        final Fragment f1 = new Fragment(file(), pos(4, 1), pos(6, 0));
        assertEquals(2, f1.getNumberOfLines());
    }

    @Test
    public void testGetNumberOfLines4() {
        final Fragment f1 = new Fragment(file(), pos(4, 1), pos(4, 0));
        assertEquals(0, f1.getNumberOfLines());
    }

    @Test
    public void testGetNumberOfLines5() {
        final Fragment f1 = new Fragment(file(), pos(4, 1), pos(4, 1));
        assertEquals(0, f1.getNumberOfLines());
    }

    @Test
    public void testGetNumberOfLines6() {
        final Fragment f1 = new Fragment(file(), pos(4, 1), pos(5, 1));
        assertEquals(1, f1.getNumberOfLines());
    }

    @Test
    public void testGetNumberOfLines7() {
        final Fragment f1 = new Fragment(file(), pos(4, 1), pos(4, 3));
        assertEquals(0, f1.getNumberOfLines());
    }

    @Test
    public void testGetContent1() {
        final Fragment f1 = new Fragment(file(), pos(1, 1), pos(1, 0));
        assertEquals("", f1.getContentFullLines());
    }

    @Test
    public void testGetContent2() {
        final String content = "abcdef";
        final Fragment f1 = new Fragment(fileWithContent(content), pos(1, 2), pos(1, 3));
        assertEquals("abcdef\n", f1.getContentFullLines());
    }

    @Test
    public void testGetContent3() {
        final String content = "a\nb\nc\nd\ne\nf\n";
        final Fragment f1 = new Fragment(fileWithContent(content), pos(2, 1), pos(5, 0));
        assertEquals("b\nc\nd\n", f1.getContentFullLines());
    }

    @Test
    public void testGetContent4() {
        final String content = "a\nb\nc\nd\ne\nf\n";
        final Fragment f1 = new Fragment(fileWithContent(content), pos(2, 2), pos(5, 1));
        assertEquals("b\nc\nd\ne\n", f1.getContentFullLines());
    }
}
