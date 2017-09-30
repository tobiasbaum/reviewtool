package de.setsoftware.reviewtool.ordering2.base;

import java.util.Collection;
import java.util.List;

public interface PatternMatch {

    public abstract boolean isContainedInOrder(List<Stop> permutation);

    public abstract boolean isContainedAdjacent(List<Stop> permutation);

    public abstract Collection<? extends Stop> getStops();

}
