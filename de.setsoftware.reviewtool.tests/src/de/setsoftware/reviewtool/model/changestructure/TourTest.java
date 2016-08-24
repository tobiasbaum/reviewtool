package de.setsoftware.reviewtool.model.changestructure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public class TourTest {

    private static PositionInText pos(int line, int column) {
        return new PositionInText(line, column);
    }

    private static FileInRevision file(String name, int revision) {
        return new FileInRevision(name, new RepoRevision(revision), StubRepo.INSTANCE);
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
        final Fragment from1 = new Fragment(file("a.java", 1), pos(1, 1), pos(2, 0),  "abc");
        final Fragment to1 = new Fragment(file("a.java", 2), pos(1, 1), pos(2, 0),  "ABC");
        final Fragment current1 = new Fragment(file("a.java", 3), pos(1, 1), pos(2, 0),  "ABC");
        final Stop s1 = new Stop(from1, to1, current1, false, true);

        final Fragment from2 = new Fragment(file("b.java", 2), pos(1, 1), pos(2, 0),  "abc");
        final Fragment to2 = new Fragment(file("b.java", 3), pos(1, 1), pos(2, 0),  "ABC");
        final Fragment current2 = new Fragment(file("b.java", 3), pos(1, 1), pos(2, 0),  "ABC");
        final Stop s2 = new Stop(from2, to2, current2, false, true);

        final Tour t1 = new Tour("tA", Collections.singletonList(s1));
        final Tour t2 = new Tour("tB", Collections.singletonList(s2));
        final Tour merged = t1.mergeWith(t2);

        assertEquals("tA + tB", merged.getDescription());
        assertEquals(Arrays.asList(s1, s2), merged.getStops());
    }

    @Test
    public void testMergeWithDifferentPartsOfSameFileAndDifferentFile() {
        final Fragment from1 = new Fragment(file("a.java", 1), pos(1, 1), pos(2, 0),  "abc");
        final Fragment to1 = new Fragment(file("a.java", 2), pos(1, 1), pos(2, 0),  "ABC");
        final Fragment current1 = new Fragment(file("a.java", 3), pos(1, 1), pos(2, 0),  "ABC");
        final Stop s1 = new Stop(from1, to1, current1, false, true);

        final Fragment from2 = new Fragment(file("b.java", 2), pos(1, 1), pos(2, 0),  "abc");
        final Fragment to2 = new Fragment(file("b.java", 3), pos(1, 1), pos(2, 0),  "ABC");
        final Fragment current2 = new Fragment(file("b.java", 3), pos(1, 1), pos(2, 0),  "ABC");
        final Stop s2 = new Stop(from2, to2, current2, false, true);

        final Fragment from3 = new Fragment(file("a.java", 2), pos(4, 1), pos(5, 0),  "xyz");
        final Fragment to3 = new Fragment(file("a.java", 3), pos(4, 1), pos(5, 0),  "XYZ");
        final Fragment current3 = new Fragment(file("a.java", 3), pos(4, 1), pos(5, 0),  "XYZ");
        final Stop s3 = new Stop(from3, to3, current3, false, true);

        final Tour t1 = new Tour("tourA", Collections.singletonList(s1));
        final Tour t2 = new Tour("tourB", Arrays.asList(s2, s3));
        final Tour merged = t1.mergeWith(t2);

        assertEquals("tourA + tourB", merged.getDescription());
        assertEquals(Arrays.asList(s1, s3, s2), merged.getStops());
    }

    @Test
    public void testMergeOrderInFileByLine() {
        final Fragment from1 = new Fragment(file("a.java", 1), pos(1, 1), pos(2, 0),  "abc");
        final Fragment to1 = new Fragment(file("a.java", 2), pos(1, 1), pos(2, 0),  "ABC");
        final Fragment current1 = new Fragment(file("a.java", 3), pos(1, 1), pos(2, 0),  "ABC");
        final Stop s1 = new Stop(from1, to1, current1, false, true);

        final Fragment from2 = new Fragment(file("b.java", 2), pos(1, 1), pos(2, 0),  "abc");
        final Fragment to2 = new Fragment(file("b.java", 3), pos(1, 1), pos(2, 0),  "ABC");
        final Fragment current2 = new Fragment(file("b.java", 3), pos(1, 1), pos(2, 0),  "ABC");
        final Stop s2 = new Stop(from2, to2, current2, false, true);

        final Fragment from3 = new Fragment(file("a.java", 2), pos(4, 1), pos(5, 0),  "xyz");
        final Fragment to3 = new Fragment(file("a.java", 3), pos(4, 1), pos(5, 0),  "XYZ");
        final Fragment current3 = new Fragment(file("a.java", 3), pos(4, 1), pos(5, 0),  "XYZ");
        final Stop s3 = new Stop(from3, to3, current3, false, true);

        final Tour t1 = new Tour("tourA", Collections.singletonList(s1));
        final Tour t2 = new Tour("tourB", Arrays.asList(s2, s3));
        final Tour merged = t2.mergeWith(t1);

        assertEquals("tourB + tourA", merged.getDescription());
        assertEquals(Arrays.asList(s2, s1, s3), merged.getStops());
    }

    @Test
    public void testMergeOfAdjacentLines() {
        final Fragment from1 = new Fragment(file("a.java", 1), pos(1, 1), pos(2, 0),  "abc");
        final Fragment to1 = new Fragment(file("a.java", 2), pos(1, 1), pos(2, 0),  "ABC");
        final Fragment current1 = new Fragment(file("a.java", 3), pos(1, 1), pos(2, 0),  "ABC");
        final Stop s1 = new Stop(from1, to1, current1, false, true);

        final Fragment from2 = new Fragment(file("a.java", 2), pos(2, 1), pos(3, 0),  "xyz");
        final Fragment to2 = new Fragment(file("a.java", 3), pos(2, 1), pos(3, 0),  "XYZ");
        final Fragment current2 = new Fragment(file("a.java", 3), pos(2, 1), pos(3, 0),  "XYZ");
        final Stop s2 = new Stop(from2, to2, current2, false, true);

        final Tour t1 = new Tour("tourA", Collections.singletonList(s1));
        final Tour t2 = new Tour("tourB", Collections.singletonList(s2));
        final Tour merged = t1.mergeWith(t2);

        final Stop mergedStop = s1.merge(s2);

        assertEquals("tourA + tourB", merged.getDescription());
        assertEquals(Arrays.asList(mergedStop), merged.getStops());
    }

    @Test
    public void testMergeOfBinaryChanges() {
        final Stop s1 = new Stop(file("a.java", 1), file("a.java", 2), file("a.java", 3), false, true);
        final Stop s2 = new Stop(file("a.java", 2), file("a.java", 3), file("a.java", 3), false, true);

        final Tour t1 = new Tour("tourA", Collections.singletonList(s1));
        final Tour t2 = new Tour("tourB", Collections.singletonList(s2));
        final Tour merged = t1.mergeWith(t2);

        final Stop mergedStop = s1.merge(s2);

        assertEquals("tourA + tourB", merged.getDescription());
        assertEquals(Arrays.asList(mergedStop), merged.getStops());
    }

    @Test
    public void testCannotMergeNeighboringStopsWithDifferentRelevance() {
        final Fragment from1 = new Fragment(file("a.java", 1), pos(1, 1), pos(2, 0),  "abc");
        final Fragment to1 = new Fragment(file("a.java", 2), pos(1, 1), pos(2, 0),  "ABC");
        final Fragment current1 = new Fragment(file("a.java", 3), pos(1, 1), pos(2, 0),  "ABC");
        final Stop s1 = new Stop(from1, to1, current1, false);

        final Fragment from2 = new Fragment(file("a.java", 2), pos(2, 1), pos(3, 0),  "xyz");
        final Fragment to2 = new Fragment(file("a.java", 3), pos(2, 1), pos(3, 0),  "XYZ");
        final Fragment current2 = new Fragment(file("a.java", 3), pos(2, 1), pos(3, 0),  "XYZ");
        final Stop s2 = new Stop(from2, to2, current2, true);

        assertFalse(s1.canBeMergedWith(s2));
        assertFalse(s2.canBeMergedWith(s1));
    }

    @Test
    public void testCanMergeOverlappingStopsWithDifferentRelevance() {
        final Fragment from1 = new Fragment(file("a.java", 1), pos(1, 1), pos(3, 0),  "abc\nrrr");
        final Fragment to1 = new Fragment(file("a.java", 2), pos(1, 1), pos(3, 0),  "ABC\nrrr");
        final Fragment current1 = new Fragment(file("a.java", 3), pos(1, 1), pos(3, 0),  "ABC\nrrr");
        final Stop s1 = new Stop(from1, to1, current1, false);

        final Fragment from2 = new Fragment(file("a.java", 2), pos(2, 1), pos(4, 0),  "rrr\nxyz");
        final Fragment to2 = new Fragment(file("a.java", 3), pos(2, 1), pos(4, 0),  "rrr\nXYZ");
        final Fragment current2 = new Fragment(file("a.java", 3), pos(2, 1), pos(4, 0),  "rrr\nXYZ");
        final Stop s2 = new Stop(from2, to2, current2, true);

        assertTrue(s1.canBeMergedWith(s2));
        assertTrue(s2.canBeMergedWith(s1));
    }

}
