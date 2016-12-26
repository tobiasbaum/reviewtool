package de.setsoftware.reviewtool.ordering.relationtypes;

import de.setsoftware.reviewtool.ordering.basealgorithm.RelatednessFunction;

public abstract class RelationTypeRelatednessFunction<S> implements RelatednessFunction<S, RelatednessVector> {

    private final RelationType[] typePreferences;

    public RelationTypeRelatednessFunction(RelationType[] typePreferences) {
        this.typePreferences = typePreferences;
    }

    @Override
    public RelatednessVector determineRelatedness(S stop1, S stop2) {
        final double[] values = new double[this.typePreferences.length];
        for (int i = 0; i < this.typePreferences.length; i++) {
            values[i] = this.determineRelatednessForType(this.typePreferences[i], stop1, stop2);
        }
        return new RelatednessVector(values);
    }

    protected abstract double determineRelatednessForType(RelationType type, S stop1, S stop2);

}
