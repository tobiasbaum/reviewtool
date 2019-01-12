package de.setsoftware.reviewtool.ordering;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

import de.setsoftware.reviewtool.base.Multimap;
import de.setsoftware.reviewtool.base.Pair;
import de.setsoftware.reviewtool.model.api.IRevisionedFile;
import de.setsoftware.reviewtool.model.api.IWorkingCopy;
import de.setsoftware.reviewtool.model.changestructure.Stop;
import de.setsoftware.reviewtool.ordering.efficientalgorithm.TourCalculatorControl;

/**
 * Groups stops that belong to the same file.
 */
public class InSameFileRelation implements RelationMatcher {

    private final HierarchyExplicitness explicitness;

    public InSameFileRelation(HierarchyExplicitness explicitness) {
        this.explicitness = explicitness;
    }

    @Override
    public Collection<? extends OrderingInfo> determineMatches(
            List<ChangePart> changeParts, TourCalculatorControl control) {
        final Multimap<Pair<IWorkingCopy, IRevisionedFile>, ChangePart> grouping = new Multimap<>();
        for (final ChangePart c : changeParts) {
            final Stop stop = c.getStops().get(0);
            grouping.put(Pair.create(stop.getWorkingCopy(), stop.getMostRecentFile()), c);
        }

        final List<OrderingInfo> ret = new ArrayList<>();
        for (final Entry<Pair<IWorkingCopy, IRevisionedFile>, List<ChangePart>> e : grouping.entrySet()) {
            final Pair<IWorkingCopy, IRevisionedFile> p = e.getKey();
            ret.add(OrderingInfoImpl.unordered(
                    this.explicitness,
                    p.getSecond().toLocalPath(p.getFirst()).lastSegment(), e.getValue()));
        }
        return ret;
    }

}
