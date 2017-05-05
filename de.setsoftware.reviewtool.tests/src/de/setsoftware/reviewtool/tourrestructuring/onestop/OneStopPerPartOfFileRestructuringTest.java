package de.setsoftware.reviewtool.tourrestructuring.onestop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import de.setsoftware.reviewtool.model.api.IPositionInText;
import de.setsoftware.reviewtool.model.api.IRepoRevision;
import de.setsoftware.reviewtool.model.api.IRepository;
import de.setsoftware.reviewtool.model.api.IRevision;
import de.setsoftware.reviewtool.model.api.IRevisionedFile;
import de.setsoftware.reviewtool.model.changestructure.AbstractRepository;
import de.setsoftware.reviewtool.model.changestructure.ChangestructureFactory;
import de.setsoftware.reviewtool.model.changestructure.Stop;
import de.setsoftware.reviewtool.model.changestructure.Tour;

/**
 * Tests for {@link OneStopPerPartOfFileRestructuring}.
 */
public class OneStopPerPartOfFileRestructuringTest {

    private static final IRepository REPO = new AbstractRepository() {
        @Override
        public String getId() {
            return "stub";
        }

        @Override
        public File getLocalRoot() {
            return null;
        }

        @Override
        public String toAbsolutePathInWc(String absolutePathInRepo) {
            return absolutePathInRepo;
        }

        @Override
        public String fromAbsolutePathInWc(String absolutePathInWc) {
            return absolutePathInWc;
        }

        @Override
        public IRevision getSmallestRevision(Collection<? extends IRevision> revisions) {
            return this.getSmallestOfComparableRevisions(revisions);
        }

        @Override
        public byte[] getFileContents(final String path, final IRepoRevision revision) {
            return new byte[0];
        }
    };

    private static IRevisionedFile fileInRevision(String file, int i) {
        return ChangestructureFactory.createFileInRevision(
                file,
                ChangestructureFactory.createRepoRevision(i),
                REPO);
    }

    private static Stop stop(final String file, int revision) {
        return new Stop(
                ChangestructureFactory.createBinaryChange(
                        fileInRevision(file, revision - 1), fileInRevision(file, revision), false, true),
                fileInRevision(file, 100));
    }

    private static Stop stop(final String file, int... revisions) {
        Stop s = stop(file, revisions[0]);
        for (int i = 1; i < revisions.length; i++) {
            s = s.merge(stop(file, revisions[i]));
        }
        return s;
    }

    private static Stop stopWithLines(final String file, int revision, int lineFrom, int lineTo) {
        final IPositionInText posFrom = ChangestructureFactory.createPositionInText(lineFrom, 1);
        final IPositionInText posTo = ChangestructureFactory.createPositionInText(lineTo + 1, 1);
        return new Stop(
                ChangestructureFactory.createTextualChangeHunk(
                        ChangestructureFactory.createFragment(fileInRevision(file, revision - 1), posFrom, posTo),
                        ChangestructureFactory.createFragment(fileInRevision(file, revision), posFrom, posTo),
                        false,
                        true),
                ChangestructureFactory.createFragment(fileInRevision(file, 100), posFrom, posTo));
    }

    private static Tour tour(String description, int revision, String... filesWithStops) {
        final List<Stop> stops = new ArrayList<>();
        for (final String file : filesWithStops) {
            stops.add(stop(file, revision));
        }
        return new Tour(description, stops);
    }

    private static Tour tour(String description, Stop... stops) {
        return new Tour(description, Arrays.asList(stops));
    }

    @Test
    public void testNoTours() {
        assertNull(new OneStopPerPartOfFileRestructuring().restructure(Collections.<Tour>emptyList()));
    }

    @Test
    public void testOneTour() {
        final List<Tour> tours = new ArrayList<>();
        tours.add(tour("desc", 1, "a", "b", "c"));
        assertNull(new OneStopPerPartOfFileRestructuring().restructure(tours));
    }

    @Test
    public void testNotMergeable() {
        final List<Tour> tours = new ArrayList<>();
        tours.add(tour("desc", 1, "a", "b", "c"));
        tours.add(tour("desc", 2, "d", "e"));
        assertNull(new OneStopPerPartOfFileRestructuring().restructure(tours));
    }

    @Test
    public void testOneTourIsSubsetOfOther() {
        final List<Tour> tours = new ArrayList<>();
        tours.add(tour("desc1", 1, "a", "b", "c"));
        tours.add(tour("desc2", 2, "a", "b"));
        final List<? extends Tour> actual = new OneStopPerPartOfFileRestructuring().restructure(tours);

        final List<Tour> expected = new ArrayList<>();
        expected.add(tour("desc1 + desc2", stop("a", 1, 2), stop("b", 1, 2), stop("c", 1)));
        assertEquals(expected, actual);
    }

    @Test
    public void testMergeOfStopsWhenToursAreNoSubsets() {
        final List<Tour> tours = new ArrayList<>();
        tours.add(tour("desc1", 1, "a", "b", "c"));
        tours.add(tour("desc2", 2, "c", "d"));
        final List<? extends Tour> actual = new OneStopPerPartOfFileRestructuring().restructure(tours);

        final List<Tour> expected = new ArrayList<>();
        expected.add(tour("desc1", stop("a", 1), stop("b", 1)));
        expected.add(tour("desc2 + desc1", stop("c", 1, 2), stop("d", 2)));
        assertEquals(expected, actual);
    }

    @Test
    public void testWithThreeTours() {
        final List<Tour> tours = new ArrayList<>();
        tours.add(tour("desc1", 1, "a", "b", "c", "d", "e"));
        tours.add(tour("desc2", 2, "b", "f"));
        tours.add(tour("desc3", 3, "b"));
        final List<? extends Tour> actual = new OneStopPerPartOfFileRestructuring().restructure(tours);

        final List<Tour> expected = new ArrayList<>();
        expected.add(tour("desc1", 1, "a", "c", "d", "e"));
        expected.add(tour("desc2 + desc3 + desc1", stop("b", 1, 2, 3), stop("f", 2)));
        assertEquals(expected, actual);
    }

    @Test
    public void testMergeTwoToursWithEqualStops() {
        final List<Tour> tours = new ArrayList<>();
        tours.add(tour("desc1", 1, "a"));
        tours.add(tour("desc2", 2, "a"));
        final List<? extends Tour> actual = new OneStopPerPartOfFileRestructuring().restructure(tours);

        final List<Tour> expected = new ArrayList<>();
        expected.add(tour("desc2 + desc1", stop("a", 1, 2)));
        assertEquals(expected, actual);
    }

    @Test
    public void testMergeTwoToursWithOverlappingStops() {
        final List<Tour> tours = new ArrayList<>();
        tours.add(tour("desc1", stopWithLines("a", 1, 1, 22)));
        tours.add(tour("desc2", stopWithLines("a", 2, 10, 10), stopWithLines("a", 2, 15, 15)));
        final List<? extends Tour> actual = new OneStopPerPartOfFileRestructuring().restructure(tours);

        final List<Tour> expected = new ArrayList<>();
        expected.add(tour("desc2 + desc1",
                stopWithLines("a", 1, 1, 22)
                .merge(stopWithLines("a", 2, 10, 10))
                .merge(stopWithLines("a", 2, 15, 15))));
        assertEquals(expected, actual);
    }

}
