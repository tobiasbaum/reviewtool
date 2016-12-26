package de.setsoftware.reviewtool.ordering.basealgorithm;

public interface RelatednessFunction<S, R> {

    public abstract R determineRelatedness(S stop1, S stop2);

}
