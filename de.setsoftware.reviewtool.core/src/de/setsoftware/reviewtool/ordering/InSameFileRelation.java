package de.setsoftware.reviewtool.ordering;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

import de.setsoftware.reviewtool.base.Multimap;
import de.setsoftware.reviewtool.model.api.IRevisionedFile;
import de.setsoftware.reviewtool.model.changestructure.Stop;

/**
 * Groups stops that belong to the same file.
 */
class InSameFileRelation implements RelationMatcher {

    @Override
    public Collection<? extends OrderingInfo> determineMatches(List<Stop> stops) {
        final Multimap<IRevisionedFile, Stop> grouping = new Multimap<>();
        for (final Stop s : stops) {
            grouping.put(s.getMostRecentFile(), s);
        }

        final List<OrderingInfo> ret = new ArrayList<>();
        for (final Entry<IRevisionedFile, List<Stop>> e : grouping.entrySet()) {
            ret.add(new SimpleUnorderedMatch(true, e.getKey().toLocalPath().lastSegment(), e.getValue()));
        }
        return ret;
    }

}
