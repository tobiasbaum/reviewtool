package de.setsoftware.reviewtool.ordering;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import de.setsoftware.reviewtool.model.api.IClassification;
import de.setsoftware.reviewtool.model.api.IFragment;
import de.setsoftware.reviewtool.model.changestructure.IStopOrdering;
import de.setsoftware.reviewtool.model.changestructure.Stop;
import de.setsoftware.reviewtool.model.changestructure.TourElement;
import de.setsoftware.reviewtool.ordering.efficientalgorithm.MatchSet;
import de.setsoftware.reviewtool.ordering.efficientalgorithm.TourCalculator;
import de.setsoftware.reviewtool.ordering.efficientalgorithm.TourCalculatorControl;

/**
 * Facade for sorting and hierarchical grouping of stops.
 */
public class StopOrdering implements IStopOrdering {

    private final List<RelationMatcher> relationTypes;

    public StopOrdering(List<RelationMatcher> relationTypes) {
        this.relationTypes = new ArrayList<>(relationTypes);
    }

    /**
     * Determines a good ordering of the given stops, groups them into sub-tours if applicable
     * and returns the result.
     */
    @Override
    public List<? extends TourElement> groupAndSort(
            List<Stop> stops, TourCalculatorControl isCanceled, Set<? extends IClassification> irrelevantCategories)
        throws InterruptedException {

        final List<ChangePart> changeParts = ChangePart.groupToMinimumGranularity(stops, irrelevantCategories);

        TourCalculator.checkInterruption(isCanceled);

        final List<OrderingInfo> orderingInfos = new ArrayList<>();
        for (final RelationMatcher m : this.relationTypes) {
            orderingInfos.addAll(m.determineMatches(changeParts));
            TourCalculator.checkInterruption(isCanceled);
        }

        final TourCalculator<ChangePart> calculator = TourCalculator.calculateFor(
                changeParts,
                getMatchSets(orderingInfos),
                this.nameAndLineComparator(),
                isCanceled);
        final List<ChangePart> sorted = calculator.getTour();

        final TourHierarchyBuilder hierarchyBuilder = new TourHierarchyBuilder(sorted);
        for (final OrderingInfo o : orderingInfos) {
            hierarchyBuilder.createSubtourIfPossible(o);
        }
        TourCalculator.checkInterruption(isCanceled);
        return hierarchyBuilder.getTopmostElements();
    }

    private Comparator<ChangePart> nameAndLineComparator() {
        return new Comparator<ChangePart>() {
            @Override
            public int compare(ChangePart o1, ChangePart o2) {
                final Stop s1 = o1.getStops().get(0);
                final Stop s2 = o2.getStops().get(0);
                final int cmp =
                        s1.getOriginalMostRecentFile().getPath().compareTo(s2.getOriginalMostRecentFile().getPath());
                if (cmp != 0) {
                    return cmp;
                }
                return Integer.compare(this.getLine(s1), this.getLine(s2));
            }

            private int getLine(Stop s) {
                final IFragment fragment = s.getOriginalMostRecentFragment();
                return fragment == null ? -1 : fragment.getFrom().getLine();
            }
        };
    }

    private static List<MatchSet<ChangePart>> getMatchSets(List<OrderingInfo> orderingInfos) {
        final List<MatchSet<ChangePart>> ret = new ArrayList<>();
        for (final OrderingInfo o : orderingInfos) {
            ret.add(o.getMatchSet());
        }
        return ret;
    }

}
