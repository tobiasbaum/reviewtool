package de.setsoftware.reviewtool.ordering;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.junit.Test;

import de.setsoftware.reviewtool.model.api.IRevisionedFile;
import de.setsoftware.reviewtool.model.changestructure.ChangestructureFactory;
import de.setsoftware.reviewtool.model.changestructure.Stop;
import de.setsoftware.reviewtool.model.changestructure.StubRepo;
import de.setsoftware.reviewtool.model.changestructure.Tour;
import de.setsoftware.reviewtool.model.changestructure.TourElement;
import de.setsoftware.reviewtool.ordering.efficientalgorithm.MatchSet;
import de.setsoftware.reviewtool.ordering.efficientalgorithm.PositionRequest;

/**
 * Test cases for {@link TourHierarchyBuilder}.
 */
public class TourHierarchyBuilderTest {

    private static IRevisionedFile file(String name, int revision) {
        return ChangestructureFactory.createFileInRevision(
                name, ChangestructureFactory.createRepoRevision(revision, StubRepo.INSTANCE));
    }

    private static Stop stop(String s) {
        return new Stop(
                ChangestructureFactory.createBinaryChange(file(s, 1), file(s, 3), false, true),
                file(s, 4));
    }

    private static Tour tour(String description, TourElement... elements) {
        return new Tour(description, Arrays.asList(elements));
    }

    private static TourHierarchyBuilder builder(Stop... stops) {
        return new TourHierarchyBuilder(Arrays.asList(stops));
    }

    private static OrderingInfo oi(final String description, final Stop... stops) {
        return new OrderingInfo() {
            @Override
            public boolean shallBeExplicit() {
                return true;
            }

            @Override
            public MatchSet<Stop> getMatchSet() {
                return new MatchSet<Stop>(Arrays.asList(stops));
            }

            @Override
            public Collection<? extends PositionRequest<Stop>> getPositionRequests() {
                return Collections.emptyList();
            }

            @Override
            public String getDescription() {
                return description;
            }
        };
    }

    @Test
    public void testEmpty() {
        final TourHierarchyBuilder builder = builder();
        assertEquals(Collections.emptyList(), builder.getTopmostElements());
    }

    @Test
    public void testNoGrouping() {
        final Stop a = stop("a");
        final Stop b = stop("b");
        final Stop c = stop("c");
        final Stop d = stop("d");

        final TourHierarchyBuilder builder = builder(a, b, c, d);
        assertEquals(Arrays.asList(a, b, c, d), builder.getTopmostElements());
    }

    @Test
    public void testSingleTourInMiddle() {
        final Stop a = stop("a");
        final Stop b = stop("b");
        final Stop c = stop("c");
        final Stop d = stop("d");

        final TourHierarchyBuilder builder = builder(a, b, c, d);
        builder.createSubtourIfPossible(oi("testtour", b, c));
        assertEquals(Arrays.asList(a, tour("testtour", b, c), d), builder.getTopmostElements());
    }

    @Test
    public void testSingleTourAtStart() {
        final Stop a = stop("a");
        final Stop b = stop("b");
        final Stop c = stop("c");
        final Stop d = stop("d");

        final TourHierarchyBuilder builder = builder(a, b, c, d);
        builder.createSubtourIfPossible(oi("testtour", a, b));
        assertEquals(Arrays.asList(tour("testtour", a, b), c, d), builder.getTopmostElements());
    }

    @Test
    public void testSingleTourAtEnd() {
        final Stop a = stop("a");
        final Stop b = stop("b");
        final Stop c = stop("c");
        final Stop d = stop("d");

        final TourHierarchyBuilder builder = builder(a, b, c, d);
        builder.createSubtourIfPossible(oi("testtour", c, d));
        assertEquals(Arrays.asList(a, b, tour("testtour", c, d)), builder.getTopmostElements());
    }

    @Test
    public void testTwoSubtours() {
        final Stop a = stop("a");
        final Stop b = stop("b");
        final Stop c = stop("c");
        final Stop d = stop("d");

        final TourHierarchyBuilder builder = builder(a, b, c, d);
        builder.createSubtourIfPossible(oi("t1", a, b));
        builder.createSubtourIfPossible(oi("t2", c, d));
        assertEquals(Arrays.asList(tour("t1", a, b), tour("t2", c, d)), builder.getTopmostElements());
    }

    @Test
    public void testNonMatchingTourIsNotCreated() {
        final Stop a = stop("a");
        final Stop b = stop("b");
        final Stop c = stop("c");
        final Stop d = stop("d");

        final TourHierarchyBuilder builder = builder(a, b, c, d);
        builder.createSubtourIfPossible(oi("t1", a, d));
        assertEquals(Arrays.asList(a, b, c, d), builder.getTopmostElements());
    }

    @Test
    public void testOverlappingTourCannotBeCreated() {
        final Stop a = stop("a");
        final Stop b = stop("b");
        final Stop c = stop("c");
        final Stop d = stop("d");

        final TourHierarchyBuilder builder = builder(a, b, c, d);
        builder.createSubtourIfPossible(oi("t1", b, c));
        builder.createSubtourIfPossible(oi("t2", c, d));
        assertEquals(Arrays.asList(a, tour("t1", b, c), d), builder.getTopmostElements());
    }

    @Test
    public void testOnlyTheFirstOfTwoMatchesCounts() {
        final Stop a = stop("a");
        final Stop b = stop("b");
        final Stop c = stop("c");
        final Stop d = stop("d");

        final TourHierarchyBuilder builder = builder(a, b, c, d);
        builder.createSubtourIfPossible(oi("t1", a, b));
        builder.createSubtourIfPossible(oi("t2", a, b));
        assertEquals(Arrays.asList(tour("t1", a, b), c, d), builder.getTopmostElements());
    }

    @Test
    public void testMatchWithSingleChildIsNotCreated() {
        final Stop a = stop("a");
        final Stop b = stop("b");
        final Stop c = stop("c");
        final Stop d = stop("d");

        final TourHierarchyBuilder builder = builder(a, b, c, d);
        builder.createSubtourIfPossible(oi("t1", a));
        assertEquals(Arrays.asList(a, b, c, d), builder.getTopmostElements());
    }

    @Test
    public void testMultipleNestings() {
        final Stop a = stop("a");
        final Stop b = stop("b");
        final Stop c = stop("c");
        final Stop d = stop("d");

        final TourHierarchyBuilder builder = builder(a, b, c, d);
        builder.createSubtourIfPossible(oi("t1", a, b, c));
        builder.createSubtourIfPossible(oi("t2", a, b));
        assertEquals(Arrays.asList(tour("t1", tour("t2", a, b), c), d), builder.getTopmostElements());
    }

}
