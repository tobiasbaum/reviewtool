package de.setsoftware.reviewtool.model.changestructure;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Tests for {@link Hunk}.
 */
public class HunkTest {

    private static FileInRevision file() {
        return new FileInRevision("file", new LocalRevision(), StubRepo.INSTANCE);
    }

    private static PositionInText pos(int line, int col) {
        return new PositionInText(line, col);
    }

    @Test
    public void testGetNumberOfColumns1() {
        final Fragment src = new Fragment(file(), pos(1, 10), pos(1, 12));
        final Fragment tgt = new Fragment(file(), pos(1, 10), pos(1, 12));
        final Hunk hunk = new Hunk(src, tgt);
        assertEquals(0, hunk.getDelta().getColumnOffset());
    }

    @Test
    public void testGetNumberOfColumns2() {
        final Fragment src = new Fragment(file(), pos(1, 10), pos(1, 10));
        final Fragment tgt = new Fragment(file(), pos(1, 10), pos(1, 12));
        final Hunk hunk = new Hunk(src, tgt);
        assertEquals(2, hunk.getDelta().getColumnOffset());
    }

    @Test
    public void testGetNumberOfColumns3() {
        final Fragment src = new Fragment(file(), pos(1, 10), pos(1, 12));
        final Fragment tgt = new Fragment(file(), pos(1, 10), pos(1, 10));
        final Hunk hunk = new Hunk(src, tgt);
        assertEquals(-2, hunk.getDelta().getColumnOffset());
    }

    @Test
    public void testGetNumberOfColumns4() {
        final Fragment src = new Fragment(file(), pos(1, 8), pos(1, 9));
        final Fragment tgt = new Fragment(file(), pos(1, 12), pos(1, 17));
        final Hunk hunk = new Hunk(src, tgt);
        assertEquals(4, hunk.getDelta().getColumnOffset());
    }

    @Test
    public void testGetNumberOfColumns5() {
        final Fragment src = new Fragment(file(), pos(1, 8), pos(1, 9));
        final Fragment tgt = new Fragment(file(), pos(1, 8), pos(2, 1));
        final Hunk hunk = new Hunk(src, tgt);
        assertEquals(-8, hunk.getDelta().getColumnOffset());
    }

}
