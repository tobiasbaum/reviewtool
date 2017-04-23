package de.setsoftware.reviewtool.model.changestructure;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/**
 * Tests merging of hunks.
 */
public class HunkMergeTest {

    private static FileInRevision file(final int revision) {
        return new FileInRevision("file", new RepoRevision(revision), StubRepo.INSTANCE);
    }

    private static PositionInText pos(int line, int col) {
        return new PositionInText(line, col);
    }

    @Test
    public void testMergeSingleAdditionOnEmptyHunkList1() throws IncompatibleFragmentException {
        final FileDiff list = new FileDiff(file(1));

        final Fragment sourceRev1 = new Fragment(file(1), pos(1, 1), pos(1, 0));
        final Fragment targetRev2 = new Fragment(file(2), pos(1, 1), pos(1, 0));
        final FileDiff mergedList = list.merge(new Hunk(sourceRev1, targetRev2));

        final List<Hunk> actualHunks = mergedList.getHunks();
        final List<Hunk> expectedHunks = new ArrayList<>();
        expectedHunks.add(new Hunk(sourceRev1, targetRev2));
        assertEquals(expectedHunks, actualHunks);
    }

    @Test
    public void testMergeSingleAdditionOnEmptyHunkList2() throws IncompatibleFragmentException {
        final FileDiff list = new FileDiff(file(1));

        final Fragment sourceRev1 = new Fragment(file(1), pos(1, 1), pos(1, 0));
        final Fragment targetRev2 = new Fragment(file(2), pos(1, 1), pos(3, 0));
        final FileDiff mergedList = list.merge(new Hunk(sourceRev1, targetRev2));

        final List<Hunk> actualHunks = mergedList.getHunks();
        final List<Hunk> expectedHunks = new ArrayList<>();
        expectedHunks.add(new Hunk(
                sourceRev1,
                new Fragment(file(2), pos(1, 1), pos(3, 0), targetRev2)));
        assertEquals(expectedHunks, actualHunks);
    }

    @Test
    public void testMergeSingleAdditionOnEmptyHunkList3() throws IncompatibleFragmentException {
        final FileDiff list = new FileDiff(file(1));

        final Fragment sourceRev1 = new Fragment(file(1), pos(5, 1), pos(5, 0));
        final Fragment targetRev2 = new Fragment(file(2), pos(5, 1), pos(7, 0));
        final FileDiff mergedList = list.merge(new Hunk(sourceRev1, targetRev2));

        final List<Hunk> actualHunks = mergedList.getHunks();
        final List<Hunk> expectedHunks = new ArrayList<>();
        expectedHunks.add(new Hunk(
                sourceRev1,
                new Fragment(file(2), pos(5, 1), pos(7, 0), targetRev2)));
        assertEquals(expectedHunks, actualHunks);
    }

    @Test
    public void testMergeSingleAdditionOnNonEmptyHunkList1() throws IncompatibleFragmentException {
        FileDiff list = new FileDiff(file(1));

        final Fragment sourceRev1 = new Fragment(file(1), pos(1, 1), pos(1, 0));
        final Fragment targetRev2 = new Fragment(file(2), pos(1, 1), pos(2, 0));
        list = list.merge(new Hunk(sourceRev1, targetRev2));

        final Fragment sourceRev2 = new Fragment(file(2), pos(2, 1), pos(2, 0));
        final Fragment targetRev3 = new Fragment(file(3), pos(2, 1), pos(3, 0));
        final FileDiff mergedList = list.merge(new Hunk(sourceRev2, targetRev3));

        final List<Hunk> actualHunks = mergedList.getHunks();
        final List<Hunk> expectedHunks = new ArrayList<>();
        expectedHunks.add(new Hunk(
                sourceRev1,
                new Fragment(file(3), pos(1, 1), pos(2, 0), targetRev2)));
        expectedHunks.add(new Hunk(
                new Fragment(file(1), pos(1, 1), pos(1, 0), sourceRev2),
                new Fragment(file(3), pos(2, 1), pos(3, 0), targetRev3)));
        assertEquals(expectedHunks, actualHunks);
    }

    @Test
    public void testMergeSingleAdditionOnNonEmptyHunkList2() throws IncompatibleFragmentException {
        FileDiff list = new FileDiff(file(1));

        final Fragment sourceRev1 = new Fragment(file(1), pos(1, 1), pos(1, 0));
        final Fragment targetRev2 = new Fragment(file(2), pos(1, 1), pos(2, 0));
        list = list.merge(new Hunk(sourceRev1, targetRev2));

        final Fragment sourceRev2 = new Fragment(file(2), pos(1, 1), pos(1, 0));
        final Fragment targetRev3 = new Fragment(file(3), pos(1, 1), pos(2, 0));
        final FileDiff mergedList = list.merge(new Hunk(sourceRev2, targetRev3));

        final List<Hunk> actualHunks = mergedList.getHunks();
        final List<Hunk> expectedHunks = new ArrayList<>();
        expectedHunks.add(new Hunk(
                sourceRev2.setFile(file(1)),
                new Fragment(file(3), pos(1, 1), pos(2, 0), targetRev3)));
        expectedHunks.add(new Hunk(
                sourceRev1,
                new Fragment(file(3), pos(2, 1), pos(3, 0), targetRev2)));
        assertEquals(expectedHunks, actualHunks);
    }

    @Test
    public void testMergeSingleAdditionOnNonEmptyHunkList3() throws IncompatibleFragmentException {
        FileDiff list = new FileDiff(file(1));

        final Fragment sourceRev1 = new Fragment(file(1), pos(1, 1), pos(1, 0));
        final Fragment targetRev2 = new Fragment(file(2), pos(1, 1), pos(2, 0));
        list = list.merge(new Hunk(sourceRev1, targetRev2));

        final Fragment sourceRev2 = new Fragment(file(2), pos(3, 1), pos(3, 0));
        final Fragment targetRev3 = new Fragment(file(3), pos(3, 1), pos(4, 0));
        final FileDiff mergedList = list.merge(new Hunk(sourceRev2, targetRev3));

        final List<Hunk> actualHunks = mergedList.getHunks();
        final List<Hunk> expectedHunks = new ArrayList<>();
        expectedHunks.add(new Hunk(
                sourceRev1,
                new Fragment(file(3), pos(1, 1), pos(2, 0), targetRev2)));
        expectedHunks.add(new Hunk(
                new Fragment(file(1), pos(2, 1), pos(2, 0), sourceRev2),
                new Fragment(file(3), pos(3, 1), pos(4, 0), targetRev3)));
        assertEquals(expectedHunks, actualHunks);
    }

    @Test
    public void testMergeSinglePartlyOverlappingChange1() throws IncompatibleFragmentException {
        FileDiff list = new FileDiff(file(1));

        final Fragment sourceRev1 = new Fragment(file(1), pos(1, 1), pos(1, 0));
        final Fragment targetRev2 = new Fragment(file(2), pos(1, 1), pos(3, 0));
        list = list.merge(new Hunk(sourceRev1, targetRev2));

        final Fragment sourceRev2 = new Fragment(file(2), pos(2, 1), pos(3, 0));
        final Fragment targetRev3 = new Fragment(file(3), pos(2, 1), pos(4, 0));
        final FileDiff mergedList = list.merge(new Hunk(sourceRev2, targetRev3));

        final List<Hunk> actualHunks = mergedList.getHunks();
        final List<Hunk> expectedHunks = new ArrayList<>();
        expectedHunks.add(new Hunk(
                sourceRev1,
                new Fragment(file(3), pos(1, 1), pos(4, 0), targetRev2, targetRev3)));
        assertEquals(expectedHunks, actualHunks);
    }

    @Test
    public void testMergeSinglePartlyOverlappingChange2() throws IncompatibleFragmentException {
        FileDiff list = new FileDiff(file(1));

        final Fragment targetRev2 = new Fragment(file(2), pos(1, 1), pos(3, 0));
        list = list.merge(new Hunk(
                new Fragment(file(1), pos(1, 1), pos(1, 0)),
                targetRev2));

        final Fragment targetRev3 = new Fragment(file(3), pos(1, 1), pos(3, 0));
        final FileDiff mergedList = list.merge(new Hunk(
                new Fragment(file(2), pos(1, 1), pos(2, 0)),
                targetRev3));

        final List<Hunk> actualHunks = mergedList.getHunks();
        final List<Hunk> expectedHunks = new ArrayList<>();
        expectedHunks.add(new Hunk(
                new Fragment(file(1), pos(1, 1), pos(1, 0)),
                new Fragment(file(3), pos(1, 1), pos(4, 0), targetRev2, targetRev3)));
        assertEquals(expectedHunks, actualHunks);
    }

    @Test
    public void testMergeSingleFullyOverlappingChange() throws IncompatibleFragmentException {
        FileDiff list = new FileDiff(file(1));

        final Fragment sourceRev1 = new Fragment(file(1), pos(2, 1), pos(2, 0));
        final Fragment targetRev2 = new Fragment(file(2), pos(2, 1), pos(5, 0));
        list = list.merge(new Hunk(sourceRev1, targetRev2));

        final Fragment sourceRev2 = new Fragment(file(2), pos(1, 1), pos(6, 0));
        final Fragment targetRev3 = new Fragment(file(3), pos(1, 1), pos(4, 0));
        final FileDiff mergedList = list.merge(new Hunk(sourceRev2, targetRev3));

        final List<Hunk> actualHunks = mergedList.getHunks();
        final List<Hunk> expectedHunks = new ArrayList<>();
        expectedHunks.add(new Hunk(
                new Fragment(file(1), pos(1, 1), pos(3, 0), sourceRev1, sourceRev2),
                new Fragment(file(3), pos(1, 1), pos(4, 0), targetRev2, targetRev3)));
        assertEquals(expectedHunks, actualHunks);
    }

    @Test
    public void testMergeSingleMultiplyOverlappingChange1() throws IncompatibleFragmentException {
        FileDiff list = new FileDiff(file(1));

        final Fragment sourceRev1 = new Fragment(file(1), pos(1, 1), pos(1, 0));
        final Fragment targetRev2 = new Fragment(file(2), pos(1, 1), pos(3, 0));
        list = list.merge(new Hunk(sourceRev1, targetRev2));

        final Fragment sourceRev2 = new Fragment(file(2), pos(4, 1), pos(4, 0));
        final Fragment targetRev3 = new Fragment(file(3), pos(4, 1), pos(6, 0));
        list = list.merge(new Hunk(sourceRev2, targetRev3));

        final Fragment sourceRev3 = new Fragment(file(3), pos(2, 1), pos(5, 0));
        final Fragment targetRev4 = new Fragment(file(4), pos(2, 1), pos(3, 0));
        final FileDiff mergedList = list.merge(new Hunk(sourceRev3, targetRev4));

        final List<Hunk> actualHunks = mergedList.getHunks();
        final List<Hunk> expectedHunks = new ArrayList<>();
        expectedHunks.add(new Hunk(
                new Fragment(file(1), pos(1, 1), pos(2, 0), sourceRev1, sourceRev2, sourceRev3),
                new Fragment(file(4), pos(1, 1), pos(4, 0), targetRev2, targetRev3, targetRev4)));
        assertEquals(expectedHunks, actualHunks);
    }


    @Test
    public void testMergeSingleMultiplyOverlappingChange2() throws IncompatibleFragmentException {
        FileDiff list = new FileDiff(file(1));

        final Fragment sourceRev1 = new Fragment(file(1), pos(1, 1), pos(1, 0));
        final Fragment targetRev2 = new Fragment(file(2), pos(1, 1), pos(3, 0));
        list = list.merge(new Hunk(sourceRev1, targetRev2));

        final Fragment sourceRev2 = new Fragment(file(2), pos(4, 1), pos(4, 0));
        final Fragment targetRev3 = new Fragment(file(3), pos(4, 1), pos(6, 0));
        list = list.merge(new Hunk(sourceRev2, targetRev3));

        final Fragment sourceRev3 = new Fragment(file(3), pos(7, 1), pos(7, 0));
        final Fragment targetRev4 = new Fragment(file(4), pos(7, 1), pos(9, 0));
        list = list.merge(new Hunk(sourceRev3, targetRev4));

        final Fragment sourceRev4 = new Fragment(file(4), pos(2, 1), pos(8, 0));
        final Fragment targetRev5 = new Fragment(file(5), pos(2, 1), pos(5, 0));
        final FileDiff mergedList = list.merge(new Hunk(sourceRev4, targetRev5));

        final List<Hunk> actualHunks = mergedList.getHunks();
        final List<Hunk> expectedHunks = new ArrayList<>();
        expectedHunks.add(new Hunk(
                new Fragment(file(1), pos(1, 1), pos(3, 0), sourceRev1, sourceRev2, sourceRev3, sourceRev4),
                new Fragment(file(5), pos(1, 1), pos(6, 0), targetRev2, targetRev3, targetRev4, targetRev5)));
        assertEquals(expectedHunks, actualHunks);
    }

    @Test
    public void testMergeAdditionAndDeletion() throws IncompatibleFragmentException {
        FileDiff list = new FileDiff(file(1));

        final Fragment targetRev2 = new Fragment(file(2), pos(1, 1), pos(4, 0));
        list = list.merge(new Hunk(
                new Fragment(file(1), pos(1, 1), pos(1, 0)),
                targetRev2));

        final Fragment targetRev3 = new Fragment(file(3), pos(1, 1), pos(4, 0));
        list = list.merge(new Hunk(
                new Fragment(file(2), pos(1, 1), pos(3, 0)),
                targetRev3));

        final Fragment targetRev4 = new Fragment(file(4), pos(3, 1), pos(3, 0));
        final FileDiff mergedList = list.merge(new Hunk(
                new Fragment(file(3), pos(3, 1), pos(5, 0)),
                targetRev4));

        final List<Hunk> actualHunks = mergedList.getHunks();
        final List<Hunk> expectedHunks = new ArrayList<>();
        expectedHunks.add(new Hunk(
                new Fragment(file(1), pos(1, 1), pos(1, 0)),
                new Fragment(file(4), pos(1, 1), pos(3, 0), targetRev2, targetRev3, targetRev4)));
        assertEquals(expectedHunks, actualHunks);
    }

    @Test
    public void testMergeChangeAndDeletion() throws IncompatibleFragmentException {
        FileDiff list = new FileDiff(file(1));

        final Fragment sourceRev1 = new Fragment(file(1), pos(1, 1), pos(3, 0));
        final Fragment targetRev2 = new Fragment(file(2), pos(1, 1), pos(4, 0));
        list = list.merge(new Hunk(sourceRev1, targetRev2));

        final Fragment sourceRev2 = new Fragment(file(2), pos(3, 1), pos(5, 0));
        final Fragment targetRev3 = new Fragment(file(3), pos(3, 1), pos(3, 0));
        final FileDiff mergedList = list.merge(new Hunk(sourceRev2, targetRev3));

        final List<Hunk> actualHunks = mergedList.getHunks();
        final List<Hunk> expectedHunks = new ArrayList<>();
        expectedHunks.add(new Hunk(
                new Fragment(file(1), pos(1, 1), pos(4, 0), sourceRev1, sourceRev2),
                new Fragment(file(3), pos(1, 1), pos(3, 0), targetRev2, targetRev3)));
        assertEquals(expectedHunks, actualHunks);
    }
}
