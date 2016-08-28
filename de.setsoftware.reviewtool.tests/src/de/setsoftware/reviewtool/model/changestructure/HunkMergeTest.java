package de.setsoftware.reviewtool.model.changestructure;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/**
 * Tests merging of hunks.
 */
public class HunkMergeTest {

    private static FileInRevision file() {
        return new FileInRevision("file", new LocalRevision(), StubRepo.INSTANCE);
    }

    private static PositionInText pos(int line, int col) {
        return new PositionInText(line, col);
    }

    @Test
    public void testMergeSingleAdditionOnEmptyHunkList1() throws IncompatibleFragmentException {
        final FileDiff list = new FileDiff();
        final Hunk hunk = new Hunk(new Fragment(file(), pos(1, 1), pos(1, 0), ""),
                new Fragment(file(), pos(1, 1), pos(1, 0), ""));
        final FileDiff mergedList = list.merge(hunk);
        final List<Hunk> actualHunks = mergedList.getHunks();
        final List<Hunk> expectedHunks = new ArrayList<>();
        expectedHunks.add(hunk);
        assertEquals(expectedHunks, actualHunks);
    }

    @Test
    public void testMergeSingleAdditionOnEmptyHunkList12() throws IncompatibleFragmentException {
        final FileDiff list = new FileDiff();
        final Hunk hunk = new Hunk(new Fragment(file(), pos(1, 1), pos(1, 0), ""),
                new Fragment(file(), pos(1, 1), pos(3, 0), "Hallo\nWelt"));
        final FileDiff mergedList = list.merge(hunk);
        final List<Hunk> actualHunks = mergedList.getHunks();
        final List<Hunk> expectedHunks = new ArrayList<>();
        expectedHunks.add(hunk);
        assertEquals(expectedHunks, actualHunks);
    }

    @Test
    public void testMergeSingleAdditionOnEmptyHunkList13() throws IncompatibleFragmentException {
        final FileDiff list = new FileDiff();
        final Hunk hunk = new Hunk(new Fragment(file(), pos(5, 1), pos(5, 0), ""),
                new Fragment(file(), pos(5, 1), pos(7, 0), "Hallo\nWelt\n"));
        final FileDiff mergedList = list.merge(hunk);
        final List<Hunk> actualHunks = mergedList.getHunks();
        final List<Hunk> expectedHunks = new ArrayList<>();
        expectedHunks.add(hunk);
        assertEquals(expectedHunks, actualHunks);
    }

    @Test
    public void testMergeSingleAdditionOnNonEmptyHunkList1() throws IncompatibleFragmentException {
        FileDiff list = new FileDiff();
        list = list.merge(new Hunk(new Fragment(file(), pos(1, 1), pos(1, 0), ""),
                new Fragment(file(), pos(1, 1), pos(2, 0), "A\n")));
        final FileDiff mergedList = list.merge(new Hunk(new Fragment(file(), pos(2, 1), pos(2, 0), ""),
                new Fragment(file(), pos(2, 1), pos(3, 0), "B\n")));
        final List<Hunk> actualHunks = mergedList.getHunks();
        final List<Hunk> expectedHunks = new ArrayList<>();
        expectedHunks.add(new Hunk(new Fragment(file(), pos(1, 1), pos(1, 0), ""),
                new Fragment(file(), pos(1, 1), pos(2, 0), "A\n")));
        expectedHunks.add(new Hunk(new Fragment(file(), pos(1, 1), pos(1, 0), ""),
                new Fragment(file(), pos(2, 1), pos(3, 0), "B\n")));
        assertEquals(expectedHunks, actualHunks);
    }

    @Test
    public void testMergeSingleAdditionOnNonEmptyHunkList2() throws IncompatibleFragmentException {
        FileDiff list = new FileDiff();
        list = list.merge(new Hunk(new Fragment(file(), pos(1, 1), pos(1, 0), ""),
                new Fragment(file(), pos(1, 1), pos(2, 0), "A\n")));
        final FileDiff mergedList = list.merge(new Hunk(new Fragment(file(), pos(1, 1), pos(1, 0), ""),
                new Fragment(file(), pos(1, 1), pos(2, 0), "B\n")));
        final List<Hunk> actualHunks = mergedList.getHunks();
        final List<Hunk> expectedHunks = new ArrayList<>();
        expectedHunks.add(new Hunk(new Fragment(file(), pos(1, 1), pos(1, 0), ""),
                new Fragment(file(), pos(1, 1), pos(2, 0), "B\n")));
        expectedHunks.add(new Hunk(new Fragment(file(), pos(1, 1), pos(1, 0), ""),
                new Fragment(file(), pos(2, 1), pos(3, 0), "A\n")));
        assertEquals(expectedHunks, actualHunks);
    }

    @Test
    public void testMergeSingleAdditionOnNonEmptyHunkList3() throws IncompatibleFragmentException {
        FileDiff list = new FileDiff();
        list = list.merge(new Hunk(new Fragment(file(), pos(1, 1), pos(1, 0), ""),
                new Fragment(file(), pos(1, 1), pos(2, 0), "A\n")));
        final FileDiff mergedList = list.merge(new Hunk(new Fragment(file(), pos(3, 1), pos(3, 0), ""),
                new Fragment(file(), pos(3, 1), pos(4, 0), "B\n")));
        final List<Hunk> actualHunks = mergedList.getHunks();
        final List<Hunk> expectedHunks = new ArrayList<>();
        expectedHunks.add(new Hunk(new Fragment(file(), pos(1, 1), pos(1, 0), ""),
                new Fragment(file(), pos(1, 1), pos(2, 0), "A\n")));
        expectedHunks.add(new Hunk(new Fragment(file(), pos(2, 1), pos(2, 0), ""),
                new Fragment(file(), pos(3, 1), pos(4, 0), "B\n")));
        assertEquals(expectedHunks, actualHunks);
    }

    @Test
    public void testMergeSinglePartlyOverlappingChange1() throws IncompatibleFragmentException {
        FileDiff list = new FileDiff();
        list = list.merge(new Hunk(new Fragment(file(), pos(1, 1), pos(1, 0), ""),
                new Fragment(file(), pos(1, 1), pos(3, 0), "A\nB\n")));
        final FileDiff mergedList = list.merge(new Hunk(new Fragment(file(), pos(2, 1), pos(3, 0), "B\n"),
                new Fragment(file(), pos(2, 1), pos(4, 0), "C\nD\n")));
        final List<Hunk> actualHunks = mergedList.getHunks();
        final List<Hunk> expectedHunks = new ArrayList<>();
        expectedHunks.add(new Hunk(new Fragment(file(), pos(1, 1), pos(1, 0), ""),
                new Fragment(file(), pos(1, 1), pos(4, 0), "A\nC\nD\n")));
        assertEquals(expectedHunks, actualHunks);
    }

    @Test
    public void testMergeSinglePartlyOverlappingChange2() throws IncompatibleFragmentException {
        FileDiff list = new FileDiff();
        list = list.merge(new Hunk(new Fragment(file(), pos(1, 1), pos(1, 0), ""),
                new Fragment(file(), pos(1, 1), pos(3, 0), "A\nB\n")));
        final FileDiff mergedList = list.merge(new Hunk(new Fragment(file(), pos(1, 1), pos(2, 0), "A\n"),
                new Fragment(file(), pos(1, 1), pos(3, 0), "C\nD\n")));
        final List<Hunk> actualHunks = mergedList.getHunks();
        final List<Hunk> expectedHunks = new ArrayList<>();
        expectedHunks.add(new Hunk(new Fragment(file(), pos(1, 1), pos(1, 0), ""),
                new Fragment(file(), pos(1, 1), pos(4, 0), "C\nD\nB\n")));
        assertEquals(expectedHunks, actualHunks);
    }

    @Test
    public void testMergeSingleFullyOverlappingChange() throws IncompatibleFragmentException {
        FileDiff list = new FileDiff();
        list = list.merge(new Hunk(new Fragment(file(), pos(2, 1), pos(2, 0), ""),
                new Fragment(file(), pos(2, 1), pos(5, 0), "B\nC\nD\n")));
        final FileDiff mergedList = list.merge(new Hunk(new Fragment(file(), pos(1, 1), pos(6, 0), "A\nB\nC\nD\nE\n"),
                new Fragment(file(), pos(1, 1), pos(4, 0), "B\nC\nD\n")));
        final List<Hunk> actualHunks = mergedList.getHunks();
        final List<Hunk> expectedHunks = new ArrayList<>();
        expectedHunks.add(new Hunk(new Fragment(file(), pos(1, 1), pos(3, 0), "A\nE\n"),
                new Fragment(file(), pos(1, 1), pos(4, 0), "B\nC\nD\n")));
        assertEquals(expectedHunks, actualHunks);
    }

    @Test
    public void testMergeSingleMultiplyOverlappingChange1() throws IncompatibleFragmentException {
        FileDiff list = new FileDiff();
        list = list.merge(new Hunk(new Fragment(file(), pos(1, 1), pos(1, 0), ""),
                new Fragment(file(), pos(1, 1), pos(3, 0), "A\nB\n")));
        list = list.merge(new Hunk(new Fragment(file(), pos(4, 1), pos(4, 0), ""),
                new Fragment(file(), pos(4, 1), pos(6, 0), "C\nD\n")));
        final FileDiff mergedList = list.merge(new Hunk(new Fragment(file(), pos(2, 1), pos(5, 0), "B\nX\nC\n"),
                new Fragment(file(), pos(2, 1), pos(3, 0), "Y\n")));
        final List<Hunk> actualHunks = mergedList.getHunks();
        final List<Hunk> expectedHunks = new ArrayList<>();
        expectedHunks.add(new Hunk(new Fragment(file(), pos(1, 1), pos(2, 0), "X\n"),
                new Fragment(file(), pos(1, 1), pos(4, 0), "A\nY\nD\n")));
        assertEquals(expectedHunks, actualHunks);
    }


    @Test
    public void testMergeSingleMultiplyOverlappingChange2() throws IncompatibleFragmentException {
        FileDiff list = new FileDiff();
        list = list.merge(new Hunk(new Fragment(file(), pos(1, 1), pos(1, 0), ""),
                new Fragment(file(), pos(1, 1), pos(3, 0), "A\nB\n")));
        list = list.merge(new Hunk(new Fragment(file(), pos(4, 1), pos(4, 0), ""),
                new Fragment(file(), pos(4, 1), pos(6, 0), "C\nD\n")));
        list = list.merge(new Hunk(new Fragment(file(), pos(7, 1), pos(7, 0), ""),
                new Fragment(file(), pos(7, 1), pos(9, 0), "E\nF\n")));
        final FileDiff mergedList =
                list.merge(new Hunk(new Fragment(file(), pos(2, 1), pos(8, 0), "B\nX\nC\nD\nY\nE\n"),
                        new Fragment(file(), pos(2, 1), pos(5, 0), "X\nY\nZ\n")));
        final List<Hunk> actualHunks = mergedList.getHunks();
        final List<Hunk> expectedHunks = new ArrayList<>();
        expectedHunks.add(new Hunk(new Fragment(file(), pos(1, 1), pos(3, 0), "X\nY\n"),
                new Fragment(file(), pos(1, 1), pos(6, 0), "A\nX\nY\nZ\nF\n")));
        assertEquals(expectedHunks, actualHunks);
    }

    @Test
    public void testMergeAdditionAndDeletion() throws IncompatibleFragmentException {
        FileDiff list = new FileDiff();
        list = list.merge(new Hunk(new Fragment(file(), pos(1, 1), pos(1, 0), ""),
                new Fragment(file(), pos(1, 1), pos(4, 0), "A\nB\nD\n")));
        list = list.merge(new Hunk(new Fragment(file(), pos(1, 1), pos(3, 0), "A\nB\n"),
                new Fragment(file(), pos(1, 1), pos(4, 0), "A'\nB'\nC\n")));
        final FileDiff mergedList = list.merge(new Hunk(new Fragment(file(), pos(3, 1), pos(5, 0), "C\nD\n"),
                new Fragment(file(), pos(3, 1), pos(3, 0), "")));
        final List<Hunk> actualHunks = mergedList.getHunks();
        final List<Hunk> expectedHunks = new ArrayList<>();
        expectedHunks.add(new Hunk(new Fragment(file(), pos(1, 1), pos(1, 0), ""),
                new Fragment(file(), pos(1, 1), pos(3, 0), "A'\nB'\n")));
        assertEquals(expectedHunks, actualHunks);
    }

    @Test
    public void testMergeChangeAndDeletion() throws IncompatibleFragmentException {
        FileDiff list = new FileDiff();
        list = list.merge(new Hunk(new Fragment(file(), pos(1, 1), pos(3, 0), "A\nB\n"),
                new Fragment(file(), pos(1, 1), pos(4, 0), "A'\nB'\nC\n")));
        final FileDiff mergedList = list.merge(new Hunk(new Fragment(file(), pos(3, 1), pos(5, 0), "C\nD\n"),
                new Fragment(file(), pos(3, 1), pos(3, 0), "")));
        final List<Hunk> actualHunks = mergedList.getHunks();
        final List<Hunk> expectedHunks = new ArrayList<>();
        expectedHunks.add(new Hunk(new Fragment(file(), pos(1, 1), pos(4, 0), "A\nB\nD\n"),
                new Fragment(file(), pos(1, 1), pos(3, 0), "A'\nB'\n")));
        assertEquals(expectedHunks, actualHunks);
    }
}
