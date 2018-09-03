package de.setsoftware.reviewtool.ordering;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import de.setsoftware.reviewtool.base.ComparableWrapper;
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

    private static IRevisionedFile file(final String name, final int revision) {
        return ChangestructureFactory.createFileInRevision(
                name, ChangestructureFactory.createRepoRevision(ComparableWrapper.wrap(revision), StubRepo.INSTANCE));
    }

    private static Stop stop(final String s) {
        return new Stop(
                ChangestructureFactory.createBinaryChange(null, file(s, 1), file(s, 3)),
                file(s, 4));
    }

    private static Tour tour(final String description, final TourElement... elements) {
        return new Tour(description, Arrays.asList(elements));
    }

    private static TourHierarchyBuilder builder(final Stop... stops) {
        return new TourHierarchyBuilder(wrap(stops));
    }

    private static ChangePart cp(final Stop... stops) {
        return new ChangePart(Arrays.asList(stops));
    }

    private static List<ChangePart> wrap(final Stop... stops) {
        final List<ChangePart> changeParts = new ArrayList<>();
        for (final Stop s : stops) {
            changeParts.add(cp(s));
        }
        return changeParts;
    }

    private static OrderingInfo oi(final String description, final Stop... stops) {
        return oi(description, wrap(stops));
    }

    private static OrderingInfo oi(final String description, final List<ChangePart> changeParts) {
        return oi(description, changeParts, HierarchyExplicitness.ONLY_NONTRIVIAL);
    }

    private static OrderingInfo oi(
            final String description, final List<ChangePart> changeParts, final HierarchyExplicitness explicitness) {
        return new OrderingInfo() {
            @Override
            public HierarchyExplicitness getExplicitness() {
                return explicitness;
            }

            @Override
            public MatchSet<ChangePart> getMatchSet() {
                return new MatchSet<>(changeParts);
            }

            @Override
            public Collection<? extends PositionRequest<ChangePart>> getPositionRequests() {
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

    @Test
    public void testLargerTour() {
        final Stop a = stop("a");
        final Stop b = stop("b");
        final Stop c = stop("c");
        final Stop d = stop("d");
        final Stop e = stop("e");
        final Stop f = stop("f");
        final Stop g = stop("g");
        final Stop h = stop("h");
        final Stop i = stop("i");
        final Stop j = stop("j");
        final Stop k = stop("k");
        final Stop l = stop("l");
        final Stop m = stop("m");
        final Stop n = stop("n");

        final TourHierarchyBuilder builder = builder(a, b, c, d, e, f, g, h, i, j, k, l, m, n);
        builder.createSubtourIfPossible(oi("t3", i, j, k));
        builder.createSubtourIfPossible(oi("t1", a, b, c));
        builder.createSubtourIfPossible(oi("t4", l, m, n));
        builder.createSubtourIfPossible(oi("t2", e, f, g, h));
        assertEquals(
                Arrays.asList(tour("t1", a, b, c), d, tour("t2", e, f, g, h), tour("t3", i, j, k), tour("t4", l, m, n)),
                builder.getTopmostElements());
    }

    @Test
    public void testWithChangePartsConsistingOfMultipleStops() {
        final Stop a = stop("a");
        final Stop b = stop("b");
        final Stop c = stop("c");
        final Stop d = stop("d");
        final Stop e = stop("e");
        final Stop f = stop("f");
        final Stop g = stop("g");

        final TourHierarchyBuilder builder = builder(a, b, c, d, e, f, g);
        builder.createSubtourIfPossible(oi("t1", Arrays.asList(cp(a, b), cp(c, d))));
        builder.createSubtourIfPossible(oi("t2", Arrays.asList(cp(e), cp(f, g))));
        assertEquals(
                Arrays.asList(tour("t1", a, b, c, d), tour("t2", e, f, g)),
                builder.getTopmostElements());
    }

    @Test
    public void testGroupWithOneChangePartButTwoStops() {
        final Stop a = stop("a");
        final Stop b = stop("b");
        final Stop c = stop("c");

        final TourHierarchyBuilder builder = builder(a, b, c);
        builder.createSubtourIfPossible(oi("t1", Arrays.asList(cp(b, c))));
        assertEquals(
                Arrays.asList(a, tour("t1", b, c)),
                builder.getTopmostElements());
    }

    @Test
    public void testExplicitSingle() {
        final Stop a = stop("a");
        final TourHierarchyBuilder builder = builder(a);
        builder.createSubtourIfPossible(oi("t1", wrap(a), HierarchyExplicitness.ALWAYS));
        assertEquals(
                Arrays.asList(tour("t1", a)),
                builder.getTopmostElements());
    }

    @Test
    public void testExplicitSingles() {
        final Stop a = stop("a");
        final Stop b = stop("b");
        final TourHierarchyBuilder builder = builder(a, b);
        builder.createSubtourIfPossible(oi("t1", wrap(a), HierarchyExplicitness.ALWAYS));
        builder.createSubtourIfPossible(oi("t2", wrap(b), HierarchyExplicitness.ALWAYS));
        assertEquals(
                Arrays.asList(tour("t1", a), tour("t2", b)),
                builder.getTopmostElements());
    }

    @Test
    public void testNestedExplicitSingles() {
        final Stop a = stop("a");
        final Stop b = stop("b");
        final TourHierarchyBuilder builder = builder(a, b);
        builder.createSubtourIfPossible(oi("t1", wrap(a), HierarchyExplicitness.ALWAYS));
        builder.createSubtourIfPossible(oi("t2", wrap(a, b), HierarchyExplicitness.ALWAYS));
        builder.createSubtourIfPossible(oi("t3", wrap(a, b), HierarchyExplicitness.ALWAYS));
        assertEquals(
                Arrays.asList(tour("t3", tour("t2", tour("t1", a), b))),
                builder.getTopmostElements());
    }
}
