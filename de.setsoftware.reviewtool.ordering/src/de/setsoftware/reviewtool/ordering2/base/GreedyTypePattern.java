package de.setsoftware.reviewtool.ordering2.base;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class GreedyTypePattern implements Pattern {

    private final RelationType relationType;

    public GreedyTypePattern(RelationType relationType) {
        this.relationType = relationType;
    }

    @Override
    public Set<PatternMatch> patternMatches(StopRelationGraph graph) {

        final List<Relation> matchingRelations = new ArrayList<Relation>(
                graph.getRelationsWithType(this.relationType));
        Collections.sort(matchingRelations, new Comparator<Relation>() {
            @Override
            public int compare(Relation o1, Relation o2) {
                final int cmpTargetStop = o1.getTargetStop().compareTo(o2.getTargetStop());
                if (cmpTargetStop != 0) {
                    return cmpTargetStop;
                }
                final int cmpLabel = o1.getLabel().compareTo(o2.getLabel());
                if (cmpLabel != 0) {
                    return cmpLabel;
                }
                return o1.getSourceStop().compareTo(o2.getSourceStop());
            }
        });

        if (matchingRelations.isEmpty()) {
            return Collections.emptySet();
        }

        final Set<PatternMatch> ret = new LinkedHashSet<>();
        final List<Relation> curGroup = new ArrayList<>();
        curGroup.add(matchingRelations.get(0));
        for (final Relation r : matchingRelations.subList(1, matchingRelations.size())) {
            if (!(curGroup.get(0).getLabel().equals(r.getLabel())
                && curGroup.get(0).getTargetStop().equals(r.getTargetStop()))) {
                ret.add(this.createMatch(curGroup));
                curGroup.clear();
            }
            curGroup.add(r);
        }
        ret.add(this.createMatch(curGroup));
        return ret;
    }

    private PatternMatch createMatch(List<Relation> curGroup) {
        final List<Stop> stops = new ArrayList<>();
        stops.add(curGroup.get(0).getTargetStop());
        for (final Relation r : curGroup) {
            stops.add(r.getSourceStop());
        }
        final PatternMatchWithLabel ret = new PatternMatchWithLabel(
                this,
                curGroup.get(0).getLabel(),
                stops);
        ret.fixStopAtPosition(0);
        return ret;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof GreedyTypePattern)) {
            return false;
        }
        return ((GreedyTypePattern) o).relationType.equals(this.relationType);
    }

    @Override
    public int hashCode() {
        return this.relationType.hashCode();
    }

    @Override
    public String toString() {
        return "GP:" + this.relationType;
    }
}
