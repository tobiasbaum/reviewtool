package de.setsoftware.reviewtool.base;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Implements some useful algorithms on partially ordered sets.
 */
public final class PartialOrderAlgorithms {

    /**
     * Returns some minimal element of a partially ordered set.
     * If the underlying revisions are not totally ordered, it is unspecified which minimal element will be returned.
     * If the collection of revisions passed is empty, {@code null} is returned.
     * @param elements The collection of elements where to find some minimal element.
     */
    public static <T extends IPartiallyComparable<T>> T getSomeMinimum(final Collection<? extends T> elements) {
        T smallestSoFar = null;
        for (final T e : elements) {
            if (smallestSoFar == null) {
                smallestSoFar = e;
            } else if (e.le(smallestSoFar) && !smallestSoFar.le(e)) {
                smallestSoFar = e;
            }
        }
        return smallestSoFar;
    }

    /**
     * Performs a topological sort on a partially ordered collection.
     * @param toSort The collection to be sorted topologically.
     * @return The sorted set, represented as a list.
     */
    public static <T extends IPartiallyComparable<T>> List<T> topoSort(final Collection<? extends T> toSort) {
        if (toSort.isEmpty()) {
            return Collections.emptyList();
        }

        final LinkedHashSet<T> remainingRevisions = new LinkedHashSet<>(toSort);
        final List<T> ret = new ArrayList<>();
        while (!remainingRevisions.isEmpty()) {
            final T minimum = getSomeMinimum(remainingRevisions);
            ret.add(minimum);
            remainingRevisions.remove(minimum);
        }
        return ret;
    }
}
