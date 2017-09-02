package de.setsoftware.reviewtool.model.changestructure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import de.setsoftware.reviewtool.model.api.IFragment;

/**
 * Tests for {@link Tour}.
 */
public class TourTest {

    private static PositionInText pos(int line, int column) {
        return new PositionInText(line, column);
    }

    private static FileInRevision file(String name, int revision) {
        return new FileInRevision(name, new RepoRevision(revision, StubRepo.INSTANCE));
    }

    @Test
    public void testMergeOfEmptyTours() {
        final Tour t1 = new Tour("t1", Collections.<Stop>emptyList());
        final Tour t2 = new Tour("t2", Collections.<Stop>emptyList());
        final Tour merged = t1.mergeWith(t2);

        assertEquals("t1 + t2", merged.getDescription());
        assertEquals(Collections.emptyList(), merged.getStops());
    }

    @Test
    public void testMergeWithDifferentFiles() {
        final IFragment from1 = new Fragment(file("a.java", 1), pos(1, 1), pos(2, 1));
        final IFragment to1 = new Fragment(file("a.java", 2), pos(1, 1), pos(2, 1));
        final Fragment current1 = new Fragment(file("a.java", 3), pos(1, 1), pos(2, 1));
        final Stop s1 = new Stop(new TextualChangeHunk(from1, to1, false, true), current1);

        final IFragment from2 = new Fragment(file("b.java", 2), pos(1, 1), pos(2, 1));
        final IFragment to2 = new Fragment(file("b.java", 3), pos(1, 1), pos(2, 1));
        final Fragment current2 = new Fragment(file("b.java", 3), pos(1, 1), pos(2, 1));
        final Stop s2 = new Stop(new TextualChangeHunk(from2, to2, false, true), current2);

        final Tour t1 = new Tour("tA", Collections.singletonList(s1));
        final Tour t2 = new Tour("tB", Collections.singletonList(s2));
        final Tour merged = t1.mergeWith(t2);

        assertEquals("tA + tB", merged.getDescription());
        assertEquals(Arrays.asList(s1, s2), merged.getStops());
    }

    @Test
    public void testMergeWithDifferentPartsOfSameFileAndDifferentFile() {
        final IFragment from1 = new Fragment(file("a.java", 1), pos(1, 1), pos(2, 1));
        final IFragment to1 = new Fragment(file("a.java", 2), pos(1, 1), pos(2, 1));
        final Fragment current1 = new Fragment(file("a.java", 3), pos(1, 1), pos(2, 1));
        final Stop s1 = new Stop(new TextualChangeHunk(from1, to1, false, true), current1);

        final IFragment from2 = new Fragment(file("b.java", 2), pos(1, 1), pos(2, 1));
        final IFragment to2 = new Fragment(file("b.java", 3), pos(1, 1), pos(2, 1));
        final Fragment current2 = new Fragment(file("b.java", 3), pos(1, 1), pos(2, 1));
        final Stop s2 = new Stop(new TextualChangeHunk(from2, to2, false, true), current2);

        final IFragment from3 = new Fragment(file("a.java", 2), pos(4, 1), pos(5, 1));
        final IFragment to3 = new Fragment(file("a.java", 3), pos(4, 1), pos(5, 1));
        final Fragment current3 = new Fragment(file("a.java", 3), pos(4, 1), pos(5, 1));
        final Stop s3 = new Stop(new TextualChangeHunk(from3, to3, false, true), current3);

        final Tour t1 = new Tour("tourA", Collections.singletonList(s1));
        final Tour t2 = new Tour("tourB", Arrays.asList(s2, s3));
        final Tour merged = t1.mergeWith(t2);

        assertEquals("tourA + tourB", merged.getDescription());
        assertEquals(Arrays.asList(s1, s3, s2), merged.getStops());
    }

    @Test
    public void testMergeOrderInFileByLine() {
        final IFragment from1 = new Fragment(file("a.java", 1), pos(1, 1), pos(2, 1));
        final IFragment to1 = new Fragment(file("a.java", 2), pos(1, 1), pos(2, 1));
        final Fragment current1 = new Fragment(file("a.java", 3), pos(1, 1), pos(2, 1));
        final Stop s1 = new Stop(new TextualChangeHunk(from1, to1, false, true), current1);

        final IFragment from2 = new Fragment(file("b.java", 2), pos(1, 1), pos(2, 1));
        final IFragment to2 = new Fragment(file("b.java", 3), pos(1, 1), pos(2, 1));
        final Fragment current2 = new Fragment(file("b.java", 3), pos(1, 1), pos(2, 1));
        final Stop s2 = new Stop(new TextualChangeHunk(from2, to2, false, true), current2);

        final IFragment from3 = new Fragment(file("a.java", 2), pos(4, 1), pos(5, 1));
        final IFragment to3 = new Fragment(file("a.java", 3), pos(4, 1), pos(5, 1));
        final Fragment current3 = new Fragment(file("a.java", 3), pos(4, 1), pos(5, 1));
        final Stop s3 = new Stop(new TextualChangeHunk(from3, to3, false, true), current3);

        final Tour t1 = new Tour("tourA", Collections.singletonList(s1));
        final Tour t2 = new Tour("tourB", Arrays.asList(s2, s3));
        final Tour merged = t2.mergeWith(t1);

        assertEquals("tourB + tourA", merged.getDescription());
        assertEquals(Arrays.asList(s2, s1, s3), merged.getStops());
    }

    @Test
    public void testMergeOfAdjacentLines() {
        final IFragment from1 = new Fragment(file("a.java", 1), pos(1, 1), pos(2, 1));
        final IFragment to1 = new Fragment(file("a.java", 2), pos(1, 1), pos(2, 1));
        final Fragment current1 = new Fragment(file("a.java", 3), pos(1, 1), pos(2, 1));
        final Stop s1 = new Stop(new TextualChangeHunk(from1, to1, false, true), current1);

        final IFragment from2 = new Fragment(file("a.java", 2), pos(2, 1), pos(3, 1));
        final IFragment to2 = new Fragment(file("a.java", 3), pos(2, 1), pos(3, 1));
        final Fragment current2 = new Fragment(file("a.java", 3), pos(2, 1), pos(3, 1));
        final Stop s2 = new Stop(new TextualChangeHunk(from2, to2, false, true), current2);

        final Tour t1 = new Tour("tourA", Collections.singletonList(s1));
        final Tour t2 = new Tour("tourB", Collections.singletonList(s2));
        final Tour merged = t1.mergeWith(t2);

        final Stop mergedStop = s1.merge(s2);

        assertEquals("tourA + tourB", merged.getDescription());
        assertEquals(Arrays.asList(mergedStop), merged.getStops());
    }

    @Test
    public void testMergeOfBinaryChanges() {
        final Stop s1 = new Stop(new BinaryChange(file("a.java", 1), file("a.java", 2), false, true),
                file("a.java", 3));
        final Stop s2 = new Stop(new BinaryChange(file("a.java", 2), file("a.java", 3), false, true),
                file("a.java", 3));

        final Tour t1 = new Tour("tourA", Collections.singletonList(s1));
        final Tour t2 = new Tour("tourB", Collections.singletonList(s2));
        final Tour merged = t1.mergeWith(t2);

        final Stop mergedStop = s1.merge(s2);

        assertEquals("tourA + tourB", merged.getDescription());
        assertEquals(Arrays.asList(mergedStop), merged.getStops());
    }

    @Test
    public void testCannotMergeNeighboringStopsWithDifferentRelevance() {
        final IFragment from1 = new Fragment(file("a.java", 1), pos(1, 1), pos(2, 1));
        final IFragment to1 = new Fragment(file("a.java", 2), pos(1, 1), pos(2, 1));
        final Fragment current1 = new Fragment(file("a.java", 3), pos(1, 1), pos(2, 1));
        final Stop s1 = new Stop(new TextualChangeHunk(from1, to1, false, true), current1);

        final IFragment from2 = new Fragment(file("a.java", 2), pos(2, 1), pos(3, 1));
        final IFragment to2 = new Fragment(file("a.java", 3), pos(2, 1), pos(3, 1));
        final Fragment current2 = new Fragment(file("a.java", 3), pos(2, 1), pos(3, 1));
        final Stop s2 = new Stop(new TextualChangeHunk(from2, to2, true, true), current2);

        assertFalse(s1.canBeMergedWith(s2));
        assertFalse(s2.canBeMergedWith(s1));
    }

    @Test
    public void testCanMergeOverlappingStopsWithDifferentRelevance() {
        final IFragment from1 = new Fragment(file("a.java", 1), pos(1, 1), pos(3, 1));
        final IFragment to1 = new Fragment(file("a.java", 2), pos(1, 1), pos(3, 1));
        final Fragment current1 = new Fragment(file("a.java", 3), pos(1, 1), pos(3, 1));
        final Stop s1 = new Stop(new TextualChangeHunk(from1, to1, false, true), current1);

        final IFragment from2 = new Fragment(file("a.java", 2), pos(2, 1), pos(4, 1));
        final IFragment to2 = new Fragment(file("a.java", 3), pos(2, 1), pos(4, 1));
        final Fragment current2 = new Fragment(file("a.java", 3), pos(2, 1), pos(4, 1));
        final Stop s2 = new Stop(new TextualChangeHunk(from2, to2, true, true), current2);

        assertTrue(s1.canBeMergedWith(s2));
        assertTrue(s2.canBeMergedWith(s1));
    }

}
