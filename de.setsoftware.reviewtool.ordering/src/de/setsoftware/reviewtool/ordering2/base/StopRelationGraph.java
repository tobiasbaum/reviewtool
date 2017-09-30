package de.setsoftware.reviewtool.ordering2.base;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class StopRelationGraph {

    private final Set<Stop> allStops = new LinkedHashSet<>();
    private final Set<Relation> allRelations = new LinkedHashSet<>();

    public void addStop(Stop stop) {
        this.allStops.add(stop);
    }

    public void addRelation(Relation rel) {
        this.allRelations.add(rel);
    }

    public Collection<? extends Relation> getRelationsWithType(RelationType relationType) {
        final List<Relation> ret = new ArrayList<>();
        for (final Relation r : this.allRelations) {
            if (r.getType().equals(relationType)) {
                ret.add(r);
            }
        }
        return ret;
    }

    public StopRelationGraph fold(Collection<? extends Stop> matchedStops, Stop compositeStop) {
        final StopRelationGraph ret = new StopRelationGraph();

        ret.allStops.addAll(this.allStops);
        ret.allStops.removeAll(matchedStops);
        ret.allStops.add(compositeStop);

        for (final Relation r : this.allRelations) {
            if (matchedStops.contains(r.getSourceStop())) {
                if (!matchedStops.contains(r.getTargetStop())) {
                    ret.addRelation(new Relation(r.getType(), r.getLabel(), compositeStop, r.getTargetStop()));
                }
            } else if (matchedStops.contains(r.getTargetStop())) {
                ret.addRelation(new Relation(r.getType(), r.getLabel(), r.getSourceStop(), compositeStop));
            } else {
                ret.addRelation(r);
            }
        }
        return ret;
    }

    public Set<Stop> getStops() {
        return this.allStops;
    }

}
