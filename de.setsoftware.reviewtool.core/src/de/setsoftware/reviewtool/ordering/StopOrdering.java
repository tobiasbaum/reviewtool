package de.setsoftware.reviewtool.ordering;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.setsoftware.reviewtool.model.changestructure.IStopOrdering;
import de.setsoftware.reviewtool.model.changestructure.Stop;
import de.setsoftware.reviewtool.model.changestructure.TourElement;
import de.setsoftware.reviewtool.ordering.efficientalgorithm.CancelCallback;
import de.setsoftware.reviewtool.ordering.efficientalgorithm.MatchSet;
import de.setsoftware.reviewtool.ordering.efficientalgorithm.PositionRequest;
import de.setsoftware.reviewtool.ordering.efficientalgorithm.TourCalculator;

/**
 * Facade for sorting and hierarchical grouping of stops.
 */
public class StopOrdering implements IStopOrdering {

    /**
     * Determines a good ordering of the given stops, groups them into sub-tours if applicable
     * and returns the result.
     */
    @Override
    public List<? extends TourElement> groupAndSort(List<Stop> stops, CancelCallback isCanceled)
        throws InterruptedException {

        final List<OrderingInfo> orderingInfos = new ArrayList<>();
        for (final RelationMatcher m : getRelationMatchers()) {
            orderingInfos.addAll(m.determineMatches(stops));
            TourCalculator.checkInterruption(isCanceled);
        }

        final TourCalculator<Stop> calculator = TourCalculator.calculateFor(
                stops, getMatchSets(orderingInfos), getPositionRequests(orderingInfos), isCanceled);
        final List<Stop> sorted = calculator.getTour();

        final TourHierarchyBuilder hierarchyBuilder = new TourHierarchyBuilder(sorted);
        for (final OrderingInfo o : orderingInfos) {
            if (o.shallBeExplicit()) {
                hierarchyBuilder.createSubtourIfPossible(o);
            }
        }
        TourCalculator.checkInterruption(isCanceled);
        return hierarchyBuilder.getTopmostElements();
    }

    private static List<? extends RelationMatcher> getRelationMatchers() {
        //TODO make loading of relation matchers more dynamic
        return Arrays.asList(
                new InSameFileRelation(),
                new TokenSimilarityRelation());
    }

    private static List<MatchSet<Stop>> getMatchSets(List<OrderingInfo> orderingInfos) {
        final List<MatchSet<Stop>> ret = new ArrayList<>();
        for (final OrderingInfo o : orderingInfos) {
            ret.add(o.getMatchSet());
        }
        return ret;
    }

    private static List<PositionRequest<Stop>> getPositionRequests(List<OrderingInfo> orderingInfos) {
        final List<PositionRequest<Stop>> ret = new ArrayList<>();
        for (final OrderingInfo o : orderingInfos) {
            ret.addAll(o.getPositionRequests());
        }
        return ret;
    }

}
