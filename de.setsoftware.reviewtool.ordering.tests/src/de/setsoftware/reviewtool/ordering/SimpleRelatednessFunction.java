package de.setsoftware.reviewtool.ordering;

import java.util.HashMap;
import java.util.Map;

import de.setsoftware.reviewtool.ordering.relationtypes.RelationType;
import de.setsoftware.reviewtool.ordering.relationtypes.RelationTypeRelatednessFunction;

public class SimpleRelatednessFunction extends RelationTypeRelatednessFunction<String> {

    public SimpleRelatednessFunction(RelationType... typePreferences) {
        super(typePreferences);
    }

    private final Map<String, Double> values = new HashMap<>();

    @Override
    protected double determineRelatednessForType(RelationType type, String stop1, String stop2) {
        final Double v = this.values.get(this.key(type, stop1, stop2));
        return v == null ? 0.0 : v;
    }

    public void relatedBinary(RelationType type, String stop1, String stop2) {
        this.values.put(this.key(type, stop1, stop2), 1.0);
    }

    public void relatedGradual(RelationType type, String stop1, String stop2, double ratio) {
        this.values.put(this.key(type, stop1, stop2), ratio);
    }

    private String key(RelationType type, String stop1, String stop2) {
        if (stop1.compareTo(stop2) > 0) {
            return type + "," + stop2 + "," + stop1;
        } else {
            return type + "," + stop1 + "," + stop2;
        }
    }

}
