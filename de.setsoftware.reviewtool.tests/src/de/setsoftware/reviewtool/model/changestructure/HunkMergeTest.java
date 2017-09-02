package de.setsoftware.reviewtool.model.changestructure;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import de.setsoftware.reviewtool.model.api.IFileDiff;
import de.setsoftware.reviewtool.model.api.IFragment;
import de.setsoftware.reviewtool.model.api.IHunk;
import de.setsoftware.reviewtool.model.api.IncompatibleFragmentException;

/**
 * Tests merging of hunks.
 */
public class HunkMergeTest {

    private static FileInRevision file(final int revision) {
        return new FileInRevision("file", new RepoRevision(revision, StubRepo.INSTANCE));
    }

    private static PositionInText pos(int line, int col) {
        return new PositionInText(line, col);
    }

    @Test
    public void testMergeSingleAdditionOnEmptyHunkList1() throws IncompatibleFragmentException {
        final IFileDiff list = new FileDiff(file(1));

        final IFragment sourceRev1 = new Fragment(file(1), pos(1, 1), pos(1, 1));
        final IFragment targetRev2 = new Fragment(file(2), pos(1, 1), pos(1, 1));
        final IFileDiff mergedList = list.merge(new Hunk(sourceRev1, targetRev2));

        final List<? extends IHunk> actualHunks = mergedList.getHunks();
        final List<IHunk> expectedHunks = new ArrayList<>();
        expectedHunks.add(new Hunk(sourceRev1, targetRev2));
        assertEquals(expectedHunks, actualHunks);
    }

    @Test
    public void testMergeSingleAdditionOnEmptyHunkList2() throws IncompatibleFragmentException {
        final IFileDiff list = new FileDiff(file(1));

        final IFragment sourceRev1 = new Fragment(file(1), pos(1, 1), pos(1, 1));
        final IFragment targetRev2 = new Fragment(file(2), pos(1, 1), pos(3, 1));
        final IFileDiff mergedList = list.merge(new Hunk(sourceRev1, targetRev2));

        final List<? extends IHunk> actualHunks = mergedList.getHunks();
        final List<IHunk> expectedHunks = new ArrayList<>();
        expectedHunks.add(new Hunk(
                sourceRev1,
                new Fragment(file(2), pos(1, 1), pos(3, 1), targetRev2)));
        assertEquals(expectedHunks, actualHunks);
    }

    @Test
    public void testMergeSingleAdditionOnEmptyHunkList3() throws IncompatibleFragmentException {
        final IFileDiff list = new FileDiff(file(1));

        final IFragment sourceRev1 = new Fragment(file(1), pos(5, 1), pos(5, 1));
        final IFragment targetRev2 = new Fragment(file(2), pos(5, 1), pos(7, 1));
        final IFileDiff mergedList = list.merge(new Hunk(sourceRev1, targetRev2));

        final List<? extends IHunk> actualHunks = mergedList.getHunks();
        final List<IHunk> expectedHunks = new ArrayList<>();
        expectedHunks.add(new Hunk(
                sourceRev1,
                new Fragment(file(2), pos(5, 1), pos(7, 1), targetRev2)));
        assertEquals(expectedHunks, actualHunks);
    }

    @Test
    public void testMergeSingleAdditionOnNonEmptyHunkList1() throws IncompatibleFragmentException {
        IFileDiff list = new FileDiff(file(1));

        final IFragment sourceRev1 = new Fragment(file(1), pos(1, 1), pos(1, 1));
        final IFragment targetRev2 = new Fragment(file(2), pos(1, 1), pos(2, 1));
        list = list.merge(new Hunk(sourceRev1, targetRev2));

        final IFragment sourceRev2 = new Fragment(file(2), pos(2, 1), pos(2, 1));
        final IFragment targetRev3 = new Fragment(file(3), pos(2, 1), pos(3, 1));
        final IFileDiff mergedList = list.merge(new Hunk(sourceRev2, targetRev3));

        final List<? extends IHunk> actualHunks = mergedList.getHunks();
        final List<IHunk> expectedHunks = new ArrayList<>();
        expectedHunks.add(new Hunk(
                sourceRev1,
                new Fragment(file(3), pos(1, 1), pos(2, 1), targetRev2)));
        expectedHunks.add(new Hunk(
                new Fragment(file(1), pos(1, 1), pos(1, 1), sourceRev2),
                new Fragment(file(3), pos(2, 1), pos(3, 1), targetRev3)));
        assertEquals(expectedHunks, actualHunks);
    }

    @Test
    public void testMergeSingleAdditionOnNonEmptyHunkList2() throws IncompatibleFragmentException {
        IFileDiff list = new FileDiff(file(1));

        final IFragment sourceRev1 = new Fragment(file(1), pos(1, 1), pos(1, 1));
        final IFragment targetRev2 = new Fragment(file(2), pos(1, 1), pos(2, 1));
        list = list.merge(new Hunk(sourceRev1, targetRev2));

        final IFragment sourceRev2 = new Fragment(file(2), pos(1, 1), pos(1, 1));
        final IFragment targetRev3 = new Fragment(file(3), pos(1, 1), pos(2, 1));
        final IFileDiff mergedList = list.merge(new Hunk(sourceRev2, targetRev3));

        final List<? extends IHunk> actualHunks = mergedList.getHunks();
        final List<IHunk> expectedHunks = new ArrayList<>();
        expectedHunks.add(new Hunk(
                sourceRev2.setFile(file(1)),
                new Fragment(file(3), pos(1, 1), pos(2, 1), targetRev3)));
        expectedHunks.add(new Hunk(
                sourceRev1,
                new Fragment(file(3), pos(2, 1), pos(3, 1), targetRev2)));
        assertEquals(expectedHunks, actualHunks);
    }

    @Test
    public void testMergeSingleAdditionOnNonEmptyHunkList3() throws IncompatibleFragmentException {
        IFileDiff list = new FileDiff(file(1));

        final IFragment sourceRev1 = new Fragment(file(1), pos(1, 1), pos(1, 1));
        final IFragment targetRev2 = new Fragment(file(2), pos(1, 1), pos(2, 1));
        list = list.merge(new Hunk(sourceRev1, targetRev2));

        final IFragment sourceRev2 = new Fragment(file(2), pos(3, 1), pos(3, 1));
        final IFragment targetRev3 = new Fragment(file(3), pos(3, 1), pos(4, 1));
        final IFileDiff mergedList = list.merge(new Hunk(sourceRev2, targetRev3));

        final List<? extends IHunk> actualHunks = mergedList.getHunks();
        final List<IHunk> expectedHunks = new ArrayList<>();
        expectedHunks.add(new Hunk(
                sourceRev1,
                new Fragment(file(3), pos(1, 1), pos(2, 1), targetRev2)));
        expectedHunks.add(new Hunk(
                new Fragment(file(1), pos(2, 1), pos(2, 1), sourceRev2),
                new Fragment(file(3), pos(3, 1), pos(4, 1), targetRev3)));
        assertEquals(expectedHunks, actualHunks);
    }

    @Test
    public void testMergeSinglePartlyOverlappingChange1() throws IncompatibleFragmentException {
        IFileDiff list = new FileDiff(file(1));

        final IFragment sourceRev1 = new Fragment(file(1), pos(1, 1), pos(1, 1));
        final IFragment targetRev2 = new Fragment(file(2), pos(1, 1), pos(3, 1));
        list = list.merge(new Hunk(sourceRev1, targetRev2));

        final IFragment sourceRev2 = new Fragment(file(2), pos(2, 1), pos(3, 1));
        final IFragment targetRev3 = new Fragment(file(3), pos(2, 1), pos(4, 1));
        final IFileDiff mergedList = list.merge(new Hunk(sourceRev2, targetRev3));

        final List<? extends IHunk> actualHunks = mergedList.getHunks();
        final List<IHunk> expectedHunks = new ArrayList<>();
        expectedHunks.add(new Hunk(
                sourceRev1,
                new Fragment(file(3), pos(1, 1), pos(4, 1), targetRev2, targetRev3)));
        assertEquals(expectedHunks, actualHunks);
    }

    @Test
    public void testMergeSinglePartlyOverlappingChange2() throws IncompatibleFragmentException {
        IFileDiff list = new FileDiff(file(1));

        final IFragment targetRev2 = new Fragment(file(2), pos(1, 1), pos(3, 1));
        list = list.merge(new Hunk(
                new Fragment(file(1), pos(1, 1), pos(1, 1)),
                targetRev2));

        final IFragment targetRev3 = new Fragment(file(3), pos(1, 1), pos(3, 1));
        final IFileDiff mergedList = list.merge(new Hunk(
                new Fragment(file(2), pos(1, 1), pos(2, 1)),
                targetRev3));

        final List<? extends IHunk> actualHunks = mergedList.getHunks();
        final List<IHunk> expectedHunks = new ArrayList<>();
        expectedHunks.add(new Hunk(
                new Fragment(file(1), pos(1, 1), pos(1, 1)),
                new Fragment(file(3), pos(1, 1), pos(4, 1), targetRev2, targetRev3)));
        assertEquals(expectedHunks, actualHunks);
    }

    @Test
    public void testMergeSingleFullyOverlappingChange() throws IncompatibleFragmentException {
        IFileDiff list = new FileDiff(file(1));

        final IFragment sourceRev1 = new Fragment(file(1), pos(2, 1), pos(2, 1));
        final IFragment targetRev2 = new Fragment(file(2), pos(2, 1), pos(5, 1));
        list = list.merge(new Hunk(sourceRev1, targetRev2));

        final IFragment sourceRev2 = new Fragment(file(2), pos(1, 1), pos(6, 1));
        final IFragment targetRev3 = new Fragment(file(3), pos(1, 1), pos(4, 1));
        final IFileDiff mergedList = list.merge(new Hunk(sourceRev2, targetRev3));

        final List<? extends IHunk> actualHunks = mergedList.getHunks();
        final List<IHunk> expectedHunks = new ArrayList<>();
        expectedHunks.add(new Hunk(
                new Fragment(file(1), pos(1, 1), pos(3, 1), sourceRev1, sourceRev2),
                new Fragment(file(3), pos(1, 1), pos(4, 1), targetRev2, targetRev3)));
        assertEquals(expectedHunks, actualHunks);
    }

    @Test
    public void testMergeSingleMultiplyOverlappingChange1() throws IncompatibleFragmentException {
        IFileDiff list = new FileDiff(file(1));

        final IFragment sourceRev1 = new Fragment(file(1), pos(1, 1), pos(1, 1));
        final IFragment targetRev2 = new Fragment(file(2), pos(1, 1), pos(3, 1));
        list = list.merge(new Hunk(sourceRev1, targetRev2));

        final IFragment sourceRev2 = new Fragment(file(2), pos(4, 1), pos(4, 1));
        final IFragment targetRev3 = new Fragment(file(3), pos(4, 1), pos(6, 1));
        list = list.merge(new Hunk(sourceRev2, targetRev3));

        final IFragment sourceRev3 = new Fragment(file(3), pos(2, 1), pos(5, 1));
        final IFragment targetRev4 = new Fragment(file(4), pos(2, 1), pos(3, 1));
        final IFileDiff mergedList = list.merge(new Hunk(sourceRev3, targetRev4));

        final List<? extends IHunk> actualHunks = mergedList.getHunks();
        final List<IHunk> expectedHunks = new ArrayList<>();
        expectedHunks.add(new Hunk(
                new Fragment(file(1), pos(1, 1), pos(2, 1), sourceRev1, sourceRev2, sourceRev3),
                new Fragment(file(4), pos(1, 1), pos(4, 1), targetRev2, targetRev3, targetRev4)));
        assertEquals(expectedHunks, actualHunks);
    }


    @Test
    public void testMergeSingleMultiplyOverlappingChange2() throws IncompatibleFragmentException {
        IFileDiff list = new FileDiff(file(1));

        final IFragment sourceRev1 = new Fragment(file(1), pos(1, 1), pos(1, 1));
        final IFragment targetRev2 = new Fragment(file(2), pos(1, 1), pos(3, 1));
        list = list.merge(new Hunk(sourceRev1, targetRev2));

        final IFragment sourceRev2 = new Fragment(file(2), pos(4, 1), pos(4, 1));
        final IFragment targetRev3 = new Fragment(file(3), pos(4, 1), pos(6, 1));
        list = list.merge(new Hunk(sourceRev2, targetRev3));

        final IFragment sourceRev3 = new Fragment(file(3), pos(7, 1), pos(7, 1));
        final IFragment targetRev4 = new Fragment(file(4), pos(7, 1), pos(9, 1));
        list = list.merge(new Hunk(sourceRev3, targetRev4));

        final IFragment sourceRev4 = new Fragment(file(4), pos(2, 1), pos(8, 1));
        final IFragment targetRev5 = new Fragment(file(5), pos(2, 1), pos(5, 1));
        final IFileDiff mergedList = list.merge(new Hunk(sourceRev4, targetRev5));

        final List<? extends IHunk> actualHunks = mergedList.getHunks();
        final List<IHunk> expectedHunks = new ArrayList<>();
        expectedHunks.add(new Hunk(
                new Fragment(file(1), pos(1, 1), pos(3, 1), sourceRev1, sourceRev2, sourceRev3, sourceRev4),
                new Fragment(file(5), pos(1, 1), pos(6, 1), targetRev2, targetRev3, targetRev4, targetRev5)));
        assertEquals(expectedHunks, actualHunks);
    }

    @Test
    public void testMergeAdditionAndDeletion() throws IncompatibleFragmentException {
        IFileDiff list = new FileDiff(file(1));

        final IFragment targetRev2 = new Fragment(file(2), pos(1, 1), pos(4, 1));
        list = list.merge(new Hunk(
                new Fragment(file(1), pos(1, 1), pos(1, 1)),
                targetRev2));

        final IFragment targetRev3 = new Fragment(file(3), pos(1, 1), pos(4, 1));
        list = list.merge(new Hunk(
                new Fragment(file(2), pos(1, 1), pos(3, 1)),
                targetRev3));

        final IFragment targetRev4 = new Fragment(file(4), pos(3, 1), pos(3, 1));
        final IFileDiff mergedList = list.merge(new Hunk(
                new Fragment(file(3), pos(3, 1), pos(5, 1)),
                targetRev4));

        final List<? extends IHunk> actualHunks = mergedList.getHunks();
        final List<IHunk> expectedHunks = new ArrayList<>();
        expectedHunks.add(new Hunk(
                new Fragment(file(1), pos(1, 1), pos(1, 1)),
                new Fragment(file(4), pos(1, 1), pos(3, 1), targetRev2, targetRev3, targetRev4)));
        assertEquals(expectedHunks, actualHunks);
    }

    @Test
    public void testMergeChangeAndDeletion() throws IncompatibleFragmentException {
        IFileDiff list = new FileDiff(file(1));

        final IFragment sourceRev1 = new Fragment(file(1), pos(1, 1), pos(3, 1));
        final IFragment targetRev2 = new Fragment(file(2), pos(1, 1), pos(4, 1));
        list = list.merge(new Hunk(sourceRev1, targetRev2));

        final IFragment sourceRev2 = new Fragment(file(2), pos(3, 1), pos(5, 1));
        final IFragment targetRev3 = new Fragment(file(3), pos(3, 1), pos(3, 1));
        final IFileDiff mergedList = list.merge(new Hunk(sourceRev2, targetRev3));

        final List<? extends IHunk> actualHunks = mergedList.getHunks();
        final List<IHunk> expectedHunks = new ArrayList<>();
        expectedHunks.add(new Hunk(
                new Fragment(file(1), pos(1, 1), pos(4, 1), sourceRev1, sourceRev2),
                new Fragment(file(3), pos(1, 1), pos(3, 1), targetRev2, targetRev3)));
        assertEquals(expectedHunks, actualHunks);
    }
}
