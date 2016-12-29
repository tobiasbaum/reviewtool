package de.setsoftware.reviewtool.ordering.basealgorithm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;

/**
 * Implementation of the published partial order comparing the goodness of a tour ordering for code review.
 * Less is better (think ranking in sports)!
 *
 * @param<S> Type of stops.
 * @param<R> Type of relation values.
 */
public class TourGoodnessOrder<S, R extends Comparable<R>> implements PartialOrder<Tour<S>> {

    private final RelatednessFunction<S, R> relatednessFunction;

    public TourGoodnessOrder(RelatednessFunction<S, R> relatednessFunction) {
        this.relatednessFunction = relatednessFunction;
    }

    @Override
    public boolean isLessOrEquals(Tour<S> value1, Tour<S> value2) {
        assert new HashSet<>(value1.getStops()).size() == value2.getStops().size();
        assert new HashSet<>(value1.getStops()).equals(new HashSet<>(value2.getStops()));

        final List<R> possibleRelatednesses = this.determinePossibleRelatednessValuesSorted(value1.getStops());

        final Multiset<R> neighboringStopRelatednessCounts1 = this.determineNeighboringStopRelatednessCounts(value1);
        final Multiset<R> neighboringStopRelatednessCounts2 = this.determineNeighboringStopRelatednessCounts(value2);

        final Integer[] countVector1 = this.toCountVector(neighboringStopRelatednessCounts1, possibleRelatednesses);
        final Integer[] countVector2 = this.toCountVector(neighboringStopRelatednessCounts2, possibleRelatednesses);
        if (Arrays.equals(countVector1, countVector2)) {
            //when the scores are equal but the tours are different, we want to regard them as incomparable instead of equal
            //  therefore this special case
            return value1.equals(value2);
        } else {
            return new LexicographicOrder<>(new InvertedOrder<>(new NaturalOrder<Integer>())).isLessOrEquals(
                    countVector1, countVector2);
        }
    }

    private Multiset<R> determineNeighboringStopRelatednessCounts(Tour<S> tour) {
        final Multiset<R> ret = new Multiset<>();
        final List<S> stops = tour.getStops();
        for (int i = 0; i < stops.size() - 1; i++) {
            ret.add(this.relatednessFunction.determineRelatedness(stops.get(i), stops.get(i + 1)));
        }
        return ret;
    }

    private Integer[] toCountVector(Multiset<R> neighboringStopRelatednessCounts, List<R> possibleRelatednesses) {
        final Integer[] ret = new Integer[possibleRelatednesses.size()];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = neighboringStopRelatednessCounts.get(possibleRelatednesses.get(i));
        }
        return ret;
    }

    private List<R> determinePossibleRelatednessValuesSorted(List<S> stops) {
        final TreeSet<R> ret = new TreeSet<>();
        for (int i = 0; i < stops.size(); i++) {
            for (int j = i + 1; j < stops.size(); j++) {
                ret.add(this.relatednessFunction.determineRelatedness(stops.get(i), stops.get(j)));
            }
        }
        return new ArrayList<>(ret);
    }

}
