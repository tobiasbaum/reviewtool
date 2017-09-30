package de.setsoftware.reviewtool.ordering2;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import de.setsoftware.reviewtool.ordering2.base.PartialCompareResult;
import de.setsoftware.reviewtool.ordering2.base.Pattern;
import de.setsoftware.reviewtool.ordering2.base.Stop;
import de.setsoftware.reviewtool.ordering2.base.StopRelationGraph;
import de.setsoftware.reviewtool.ordering2.base.Tour;

/**
 * Naive brute-force algorithm that enumerates all permutations of the stops and compares them
 * with regard to the tour goodness order.
 */
public class BruteForceAlgorithm {

    /**
     * Determines the best tours for a given set of stops.
     * @param stops The stops to order.
     * @return The set of tours that have an optimal ordering.
     */
    public static Set<Tour> determineBestTours(StopRelationGraph graph, Set<? extends Pattern> patterns) {

        return determineExtremeElements(
                graph,
                new PermutationIterable<Stop>(graph.getStops()),
                patterns,
                PartialCompareResult.GREATER);
    }

    /**
     * Determines the worst tours for a given set of stops.
     * @param stops The stops to order.
     * @return The set of tours that have an optimal ordering.
     */
    public static Set<Tour> determineWorstTours(StopRelationGraph graph, Set<? extends Pattern> patterns) {

        return determineExtremeElements(
                graph,
                new PermutationIterable<Stop>(graph.getStops()),
                patterns,
                PartialCompareResult.LESS);
    }

    private static Set<Tour> determineExtremeElements(
            StopRelationGraph g,
            Iterable<List<Stop>> allElements,
            Set<? extends Pattern> patterns,
            PartialCompareResult expectedDirection) {
        final Set<Tour> minElements = new LinkedHashSet<>();
        for (final List<Stop> cur : allElements) {
            final Tour curTour = new Tour(g, cur);
            final Iterator<Tour> minIter = minElements.iterator();
            boolean add = true;
            while (minIter.hasNext()) {
                final Tour curMin = minIter.next();

                final PartialCompareResult cmp = curMin.compareTo(curTour, patterns);
                if (cmp == expectedDirection) {
                    add = false;
                    break;
                } else if (cmp == PartialCompareResult.EQUAL) {
                    break;
                } else if (cmp != PartialCompareResult.INCOMPARABLE) {
                    minIter.remove();
                }
            }
            if (add) {
                minElements.add(curTour);
            }
        }
        return minElements;
    }

}
