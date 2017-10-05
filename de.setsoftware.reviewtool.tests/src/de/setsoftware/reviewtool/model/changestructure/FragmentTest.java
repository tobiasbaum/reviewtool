package de.setsoftware.reviewtool.model.changestructure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;

import org.junit.Test;

import de.setsoftware.reviewtool.model.api.IFragment;

/**
 * Tests for {@link Fragment}.
 */
public class FragmentTest {

    private static FileInRevision file() {
        return new FileInRevision("file", new LocalRevision(StubRepo.INSTANCE));
    }

    private static FileInRevision fileWithContent(final String content) {
        return new FileInRevision("file", new LocalRevision(StubRepo.INSTANCE)) {
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
        final IFragment f1 = new Fragment(file(), pos(1, 1), pos(3, 1));
        final IFragment f2 = new Fragment(file(), pos(1, 1), pos(3, 1));
        assertTrue(f1.canBeMergedWith(f2));
    }

    @Test
    public void testCanBeMerged2() {
        final IFragment f1 = new Fragment(file(), pos(1, 1), pos(3, 1));
        final IFragment f2 = new Fragment(file(), pos(2, 1), pos(3, 1));
        assertTrue(f1.canBeMergedWith(f2));
    }

    @Test
    public void testCanBeMerged3() {
        final IFragment f1 = new Fragment(file(), pos(1, 1), pos(3, 1));
        final IFragment f2 = new Fragment(file(), pos(1, 3), pos(1, 6));
        assertTrue(f1.canBeMergedWith(f2));
    }

    @Test
    public void testCanBeMerged4() {
        final IFragment f1 = new Fragment(file(), pos(1, 1), pos(3, 1));
        final IFragment f2 = new Fragment(file(), pos(2, 1), pos(7, 1));
        assertTrue(f1.canBeMergedWith(f2));
    }

    @Test
    public void testCanBeMerged5() {
        final IFragment f1 = new Fragment(file(), pos(1, 1), pos(3, 1));
        final IFragment f2 = new Fragment(file(), pos(3, 1), pos(7, 1));
        assertTrue(f1.canBeMergedWith(f2));
        assertTrue(f2.canBeMergedWith(f1));
    }

    @Test
    public void testCanBeMerged6() {
        final IFragment f1 = new Fragment(file(), pos(1, 1), pos(3, 1));
        final IFragment f2 = new Fragment(file(), pos(4, 1), pos(7, 1));
        assertFalse(f1.canBeMergedWith(f2));
        assertFalse(f2.canBeMergedWith(f1));
    }

    @Test
    public void testCanBeMerged7() {
        final IFragment f1 = new Fragment(file(), pos(3, 1), pos(5, 1));
        final IFragment f2 = new Fragment(file(), pos(2, 1), pos(7, 1));
        assertTrue(f1.canBeMergedWith(f2));
        assertTrue(f2.canBeMergedWith(f1));
    }

    @Test
    public void testCanBeMerged8() {
        final IFragment f1 = new Fragment(file(), pos(1, 10), pos(1, 14));
        final IFragment f2 = new Fragment(file(), pos(1, 20), pos(1, 24));
        assertFalse(f1.canBeMergedWith(f2));
        assertFalse(f2.canBeMergedWith(f1));
    }

    @Test
    public void testCanBeMergedWithDeletionFragment1() {
        final IFragment f1 = new Fragment(file(), pos(1, 1), pos(1, 1));
        final IFragment f2 = new Fragment(file(), pos(1, 1), pos(1, 1));
        assertTrue(f1.canBeMergedWith(f2));
    }

    @Test
    public void testCanBeMergedWithDeletionFragment2() {
        final IFragment f1 = new Fragment(file(), pos(1, 1), pos(1, 1));
        final IFragment f2 = new Fragment(file(), pos(2, 1), pos(2, 1));
        assertFalse(f1.canBeMergedWith(f2));
        assertFalse(f2.canBeMergedWith(f1));
    }

    @Test
    public void testCanBeMergedWithDeletionFragment3() {
        final IFragment f1 = new Fragment(file(), pos(1, 1), pos(1, 1));
        final IFragment f2 = new Fragment(file(), pos(3, 1), pos(3, 1));
        assertFalse(f1.canBeMergedWith(f2));
        assertFalse(f2.canBeMergedWith(f1));
    }

    @Test
    public void testCanBeMergedWithDeletionFragment4() {
        final IFragment f1 = new Fragment(file(), pos(1, 1), pos(5, 1));
        final IFragment f2 = new Fragment(file(), pos(5, 1), pos(5, 1));
        assertTrue(f1.canBeMergedWith(f2));
        assertTrue(f2.canBeMergedWith(f1));
    }

    @Test
    public void testCanBeMergedWithDeletionFragment5() {
        final IFragment f1 = new Fragment(file(), pos(1, 1), pos(5, 1));
        final IFragment f2 = new Fragment(file(), pos(1, 1), pos(1, 1));
        assertTrue(f1.canBeMergedWith(f2));
        assertTrue(f2.canBeMergedWith(f1));
    }


    private static void testMergeSymmetric(IFragment f1, IFragment f2, IFragment expected) {
        assertEquals(expected, f1.merge(f2));
        assertEquals(expected, f2.merge(f1));
    }

    @Test
    public void testMerge1() {
        final IFragment f1 = new Fragment(file(), pos(1, 1), pos(3, 1));
        final IFragment f2 = new Fragment(file(), pos(1, 1), pos(3, 1));
        testMergeSymmetric(f1, f2, new Fragment(file(), pos(1, 1), pos(3, 1)));
    }

    @Test
    public void testMerge2() {
        final IFragment f1 = new Fragment(file(), pos(1, 1), pos(3, 1));
        final IFragment f2 = new Fragment(file(), pos(2, 1), pos(3, 1));
        testMergeSymmetric(f1, f2, new Fragment(file(), pos(1, 1), pos(3, 1), f1, f2));
    }

    @Test
    public void testMerge3() {
        final IFragment f1 = new Fragment(file(), pos(1, 1), pos(3, 1));
        final IFragment f2 = new Fragment(file(), pos(1, 3), pos(1, 6));
        testMergeSymmetric(f1, f2, new Fragment(file(), pos(1, 1), pos(3, 1), f1, f2));
    }

    @Test
    public void testMerge4() {
        final IFragment f1 = new Fragment(file(), pos(1, 1), pos(3, 1));
        final IFragment f2 = new Fragment(file(), pos(2, 1), pos(7, 1));
        testMergeSymmetric(f1, f2, new Fragment(file(), pos(1, 1), pos(7, 1), f1, f2));
    }

    @Test
    public void testMerge5() {
        final IFragment f1 = new Fragment(file(), pos(1, 1), pos(3, 1));
        final IFragment f2 = new Fragment(file(), pos(3, 1), pos(7, 1));
        testMergeSymmetric(f1, f2, new Fragment(file(), pos(1, 1), pos(7, 1), f1, f2));
    }

    @Test
    public void testMerge7() {
        final IFragment f1 = new Fragment(file(), pos(3, 1), pos(5, 1));
        final IFragment f2 = new Fragment(file(), pos(2, 1), pos(7, 1));
        testMergeSymmetric(f1, f2, new Fragment(file(), pos(2, 1), pos(7, 1), f1, f2));
    }

    @Test
    public void testMerge8() {
        final IFragment f1 = new Fragment(file(), pos(3, 1), pos(4, 7));
        final IFragment f2 = new Fragment(file(), pos(4, 3), pos(5, 1));
        testMergeSymmetric(f1, f2, new Fragment(file(), pos(3, 1), pos(5, 1), f1, f2));
    }

    @Test
    public void testMergeWithDeletionFragment1() {
        final IFragment f1 = new Fragment(file(), pos(1, 1), pos(1, 1));
        final IFragment f2 = new Fragment(file(), pos(1, 1), pos(1, 1));
        testMergeSymmetric(f1, f2, new Fragment(file(), pos(1, 1), pos(1, 1)));
    }

    @Test
    public void testMergeWithDeletionFragment4() {
        final IFragment f1 = new Fragment(file(), pos(1, 1), pos(5, 1));
        final IFragment f2 = new Fragment(file(), pos(5, 1), pos(5, 1));
        testMergeSymmetric(f1, f2, new Fragment(file(), pos(1, 1), pos(5, 1), f1, f2));
    }

    @Test
    public void testMergeWithDeletionFragment5() {
        final IFragment f1 = new Fragment(file(), pos(1, 1), pos(5, 1));
        final IFragment f2 = new Fragment(file(), pos(1, 1), pos(1, 1));
        testMergeSymmetric(f1, f2, new Fragment(file(), pos(1, 1), pos(5, 1), f1, f2));
    }

    @Test
    public void testIsNeighboring1() {
        final IFragment f1 = new Fragment(file(), pos(1, 1), pos(3, 1));
        final IFragment f2 = new Fragment(file(), pos(1, 1), pos(3, 1));
        assertFalse(f1.isNeighboring(f2));
    }

    @Test
    public void testIsNeighboring2() {
        final IFragment f1 = new Fragment(file(), pos(1, 1), pos(3, 1));
        final IFragment f2 = new Fragment(file(), pos(3, 1), pos(6, 1));
        assertTrue(f1.isNeighboring(f2));
    }

    @Test
    public void testIsNeighboring3() {
        final IFragment f1 = new Fragment(file(), pos(1, 1), pos(3, 1));
        final IFragment f2 = new Fragment(file(), pos(3, 1), pos(6, 1));
        assertTrue(f2.isNeighboring(f1));
    }

    @Test
    public void testIsNeighboring4() {
        final IFragment f1 = new Fragment(file(), pos(1, 1), pos(3, 1));
        final IFragment f2 = new Fragment(file(), pos(4, 1), pos(7, 1));
        assertFalse(f1.isNeighboring(f2));
    }

    @Test
    public void testIsNeighboring5() {
        final IFragment f1 = new Fragment(file(), pos(1, 1), pos(3, 1));
        final IFragment f2 = new Fragment(file(), pos(4, 1), pos(7, 1));
        assertFalse(f2.isNeighboring(f1));
    }

    @Test
    public void testIsNeighboring6() {
        final IFragment f1 = new Fragment(file(), pos(1, 1), pos(1, 4));
        final IFragment f2 = new Fragment(file(), pos(1, 4), pos(1, 7));
        assertTrue(f2.isNeighboring(f1));
    }

    @Test
    public void testIsNeighboring7() {
        final IFragment f1 = new Fragment(file(), pos(1, 1), pos(1, 4));
        final IFragment f2 = new Fragment(file(), pos(1, 4), pos(1, 7));
        assertTrue(f1.isNeighboring(f2));
    }

    @Test
    public void testIsNeighboring8() {
        final IFragment f1 = new Fragment(file(), pos(1, 1), pos(1, 4));
        final IFragment f2 = new Fragment(file(), pos(1, 3), pos(1, 6));
        assertFalse(f2.isNeighboring(f1));
    }

    @Test
    public void testIsNeighboring9() {
        final IFragment f1 = new Fragment(file(), pos(1, 1), pos(1, 4));
        final IFragment f2 = new Fragment(file(), pos(1, 3), pos(1, 6));
        assertFalse(f1.isNeighboring(f2));
    }

    @Test
    public void testIsNeighboring10() {
        final IFragment f1 = new Fragment(file(), pos(1, 1), pos(1, 4));
        final IFragment f2 = new Fragment(file(), pos(1, 5), pos(1, 8));
        assertFalse(f2.isNeighboring(f1));
    }

    @Test
    public void testIsNeighboring11() {
        final IFragment f1 = new Fragment(file(), pos(1, 1), pos(1, 4));
        final IFragment f2 = new Fragment(file(), pos(1, 5), pos(1, 8));
        assertFalse(f1.isNeighboring(f2));
    }

    @Test
    public void testGetNumberOfLines1() {
        final IFragment f1 = new Fragment(file(), pos(1, 1), pos(2, 1));
        assertEquals(1, f1.getNumberOfLines());
    }

    @Test
    public void testGetNumberOfLines2() {
        final IFragment f1 = new Fragment(file(), pos(1, 1), pos(3, 1));
        assertEquals(2, f1.getNumberOfLines());
    }

    @Test
    public void testGetNumberOfLines3() {
        final IFragment f1 = new Fragment(file(), pos(4, 1), pos(6, 1));
        assertEquals(2, f1.getNumberOfLines());
    }

    @Test
    public void testGetNumberOfLines4() {
        final IFragment f1 = new Fragment(file(), pos(4, 1), pos(4, 1));
        assertEquals(0, f1.getNumberOfLines());
    }

    @Test
    public void testGetNumberOfLines5() {
        final IFragment f1 = new Fragment(file(), pos(4, 1), pos(4, 2));
        assertEquals(0, f1.getNumberOfLines());
    }

    @Test
    public void testGetNumberOfLines6() {
        final IFragment f1 = new Fragment(file(), pos(4, 1), pos(5, 2));
        assertEquals(1, f1.getNumberOfLines());
    }

    @Test
    public void testGetNumberOfLines7() {
        final IFragment f1 = new Fragment(file(), pos(4, 1), pos(4, 4));
        assertEquals(0, f1.getNumberOfLines());
    }

    @Test
    public void testGetContentFullLines1() {
        final IFragment f1 = new Fragment(file(), pos(1, 1), pos(1, 1));
        assertEquals("", f1.getContentFullLines());
    }

    @Test
    public void testGetContentFullLines2() {
        final String content = "abcdef";
        final IFragment f1 = new Fragment(fileWithContent(content), pos(1, 2), pos(1, 4));
        assertEquals("abcdef\n", f1.getContentFullLines());
    }

    @Test
    public void testGetContentFullLines3() {
        final String content = "a\nb\nc\nd\ne\nf\n";
        final IFragment f1 = new Fragment(fileWithContent(content), pos(2, 1), pos(5, 1));
        assertEquals("b\nc\nd\n", f1.getContentFullLines());
    }

    @Test
    public void testGetContentFullLines4() {
        final String content = "a\nb\nc\nd\ne\nf\n";
        final IFragment f1 = new Fragment(fileWithContent(content), pos(2, 2), pos(5, 2));
        assertEquals("b\nc\nd\ne\n", f1.getContentFullLines());
    }

    @Test
    public void testGetContent1() {
        final String content = "x\nabcdefgh\n";
        final IFragment f1 = new Fragment(fileWithContent(content), pos(2, 1), pos(3, 1));
        assertEquals("abcdefgh\n", f1.getContent());
    }

    @Test
    public void testGetContent2() {
        final String content = "x\nabcdefgh\n";
        final IFragment f1 = new Fragment(fileWithContent(content), pos(2, 2), pos(3, 1));
        assertEquals("bcdefgh\n", f1.getContent());
    }

    @Test
    public void testGetContent3() {
        final String content = "x\nabcdefgh\n";
        final IFragment f1 = new Fragment(fileWithContent(content), pos(2, 1), pos(2, 3));
        assertEquals("ab", f1.getContent());
    }

    @Test
    public void testGetContent4() {
        final String content = "x\nabcdefgh\n";
        final IFragment f1 = new Fragment(fileWithContent(content), pos(2, 3), pos(2, 6));
        assertEquals("cde", f1.getContent());
    }

    @Test
    public void testGetContent5() {
        final String content = "x\nabcdefgh\nABCDEFGH\n";
        final IFragment f1 = new Fragment(fileWithContent(content), pos(2, 3), pos(3, 5));
        assertEquals("cdefgh\nABCD", f1.getContent());
    }
}
