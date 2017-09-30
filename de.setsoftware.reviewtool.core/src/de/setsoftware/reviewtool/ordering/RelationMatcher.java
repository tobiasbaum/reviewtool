package de.setsoftware.reviewtool.ordering;

import java.util.Collection;
import java.util.List;

import de.setsoftware.reviewtool.model.changestructure.Stop;

/**
 * Determines matches for a relation type (or some other kind of match) for the grouping and ordering
 * algorithm.
 */
public interface RelationMatcher {

    public abstract Collection<? extends OrderingInfo> determineMatches(List<Stop> stops);

}
