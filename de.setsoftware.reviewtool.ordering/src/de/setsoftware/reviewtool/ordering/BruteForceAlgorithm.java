package de.setsoftware.reviewtool.ordering;

import java.util.Set;

import de.setsoftware.reviewtool.ordering.basealgorithm.PartialOrders;
import de.setsoftware.reviewtool.ordering.basealgorithm.RelatednessFunction;
import de.setsoftware.reviewtool.ordering.basealgorithm.Tour;
import de.setsoftware.reviewtool.ordering.basealgorithm.TourGoodnessOrder;

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
    public static<S, R extends Comparable<R>> Set<Tour<S>> determineBestTours(
            Set<S> stops, RelatednessFunction<S, R> relatednessFunction) {

        return PartialOrders.determineMinElements(
                new PermutationIterable<S>(stops),
                new TourGoodnessOrder<S, R>(relatednessFunction));
    }

}
