package de.setsoftware.reviewtool.ordering2.base;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class Tour {

    private final StopRelationGraph graph;
    private final List<Stop> permutation;

    public Tour(StopRelationGraph g, List<Stop> stops) {
        this.graph = g;
        this.permutation = stops;
    }

    public PartialCompareResult compareTo(Tour other, Set<? extends Pattern> patterns) {
        final MatchSet m1 = this.determineMatchSet(patterns);
        final MatchSet m2 = other.determineMatchSet(patterns);
        return m1.compareTo(m2);
    }

    public MatchSet determineMatchSet(Set<? extends Pattern> patterns) {
        final MatchSet ret = new MatchSet();
        this.determineMatchSetRec(ret, patterns, new HashSet<Tour>());
        return ret;
    }

    private MatchSet determineMatchSetRec(MatchSet ret, Set<? extends Pattern> patterns, Set<Tour> visited) {
        final Set<Tour> folds = new LinkedHashSet<>();
        for (final Pattern p : patterns) {
            for (final PatternMatch m : p.patternMatches(this.graph)) {
                if (m.isContainedInOrder(this.permutation)) {
                    ret.addContainedInOrder(m);
                    folds.add(this.fold(m));
                } else if (m.isContainedAdjacent(this.permutation)) {
                    ret.addContainedAdjacent(m);
                    folds.add(this.fold(m));
                }
            }
        }
        for (final Tour fold : folds) {
            if (visited.contains(fold)) {
                continue;
            }
            visited.add(fold);
            fold.determineMatchSetRec(ret, patterns, visited);
        }
        return ret;
    }

    private Tour fold(PatternMatch m) {
        final Collection<? extends Stop> matchedStops = m.getStops();
        final Stop compositeStop = new CompositeStop(matchedStops);
        final StopRelationGraph newGraph = this.graph.fold(matchedStops, compositeStop);

        final List<Stop> newPermutation = new ArrayList<>();
        boolean added = false;
        for (final Stop s : this.permutation) {
            if (matchedStops.contains(s)) {
                if (!added) {
                    newPermutation.add(compositeStop);
                    added = true;
                }
            } else {
                newPermutation.add(s);
            }
        }
        return new Tour(newGraph, newPermutation);
    }

    @Override
    public int hashCode() {
        return this.permutation.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Tour)) {
            return false;
        }
        return ((Tour) o).permutation.equals(this.permutation);
    }

    @Override
    public String toString() {
        return this.permutation.toString();
    }

}
