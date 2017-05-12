package de.setsoftware.reviewtool.ordering2.efficientalgorithm;

import java.util.List;
import java.util.Set;

/**
 * Interface for a data structure/algorithm that can be used to determine an order of items that conforms to
 * a set of bundling conditions.
 *
 * @param <T> The type of the items.
 */
public interface BundleCombination<T> {

    /**
     * Returns a {@link BundleCombination} that satisfies all bundling conditions of the current one plus
     * the condition given as a parameter. If the parameter is in conflict to the existing bundlings, null
     * is returned.
     */
    public abstract BundleCombination<T> bundle(Set<T> bundle);

    /**
     * Returns a list that contains all of the items in an order that satisfies all bundlings.
     */
    public abstract List<T> getPossibleOrder();

}
