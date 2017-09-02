package de.setsoftware.reviewtool.model.changestructure;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.setsoftware.reviewtool.model.api.IFragment;
import de.setsoftware.reviewtool.model.api.IHunk;

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
        final IFragment src = new Fragment(file(), pos(1, 10), pos(1, 12));
        final IFragment tgt = new Fragment(file(), pos(1, 10), pos(1, 12));
        final IHunk hunk = new Hunk(src, tgt);
        assertEquals(0, hunk.getDelta().getColumnOffset());
    }

    @Test
    public void testGetNumberOfColumns2() {
        final IFragment src = new Fragment(file(), pos(1, 10), pos(1, 10));
        final IFragment tgt = new Fragment(file(), pos(1, 10), pos(1, 12));
        final IHunk hunk = new Hunk(src, tgt);
        assertEquals(2, hunk.getDelta().getColumnOffset());
    }

    @Test
    public void testGetNumberOfColumns3() {
        final IFragment src = new Fragment(file(), pos(1, 10), pos(1, 12));
        final IFragment tgt = new Fragment(file(), pos(1, 10), pos(1, 10));
        final IHunk hunk = new Hunk(src, tgt);
        assertEquals(-2, hunk.getDelta().getColumnOffset());
    }

    @Test
    public void testGetNumberOfColumns4() {
        final IFragment src = new Fragment(file(), pos(1, 8), pos(1, 9));
        final IFragment tgt = new Fragment(file(), pos(1, 12), pos(1, 17));
        final IHunk hunk = new Hunk(src, tgt);
        assertEquals(4, hunk.getDelta().getColumnOffset());
    }

    @Test
    public void testGetNumberOfColumns5() {
        final IFragment src = new Fragment(file(), pos(1, 8), pos(1, 9));
        final IFragment tgt = new Fragment(file(), pos(1, 8), pos(2, 1));
        final IHunk hunk = new Hunk(src, tgt);
        assertEquals(-8, hunk.getDelta().getColumnOffset());
    }

}
