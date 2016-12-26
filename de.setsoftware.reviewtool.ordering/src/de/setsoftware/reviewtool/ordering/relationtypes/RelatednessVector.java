package de.setsoftware.reviewtool.ordering.relationtypes;

import java.util.Arrays;

public class RelatednessVector implements Comparable<RelatednessVector> {

    private final double[] values;

    public RelatednessVector(double[] values) {
        this.values = values;
    }

    @Override
    public int compareTo(RelatednessVector o) {
        assert o.values.length == this.values.length;
        for (int i = 0; i < this.values.length; i++) {
            final double v1 = this.values[i];
            final double v2 = o.values[i];
            //for the single double values, 1.0 is better than 0.0, but for relatedness values, smaller is better
            //  therefore the return values here are swapped compared to a standard implementation
            if (v1 < v2) {
                return 1;
            } else if (v1 > v2) {
                return -1;
            }
        }
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof RelatednessVector)) {
            return false;
        }
        final RelatednessVector v = (RelatednessVector) o;
        return this.compareTo(v) == 0;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.values);
    }

    @Override
    public String toString() {
        return Arrays.toString(this.values);
    }

}
