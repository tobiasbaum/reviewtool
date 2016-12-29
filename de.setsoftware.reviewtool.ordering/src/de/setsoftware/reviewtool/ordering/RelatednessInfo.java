package de.setsoftware.reviewtool.ordering;

import java.util.Set;

import de.setsoftware.reviewtool.ordering.basealgorithm.PartialOrder;

public abstract class RelatednessInfo<S> {

    public abstract Set<? extends RelatednessReason> getReasonsFor(S stop);

    public abstract PartialOrder<S> getOrderFor(RelatednessReason reason);

}
