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
    public static <T extends IPartiallyComparable<T>, U extends T> U getSomeMinimum(final Collection<U> elements) {
        U smallestSoFar = null;
        for (final U e : elements) {
            if (smallestSoFar == null) {
                smallestSoFar = e;
            } else if (e.le(smallestSoFar)) {
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
    public static <T extends IPartiallyComparable<T>, U extends T> List<U> topoSort(final Collection<U> toSort) {
        if (toSort.isEmpty()) {
            return Collections.emptyList();
        }

        final LinkedHashSet<U> remainingRevisions = new LinkedHashSet<>(toSort);
        final List<U> ret = new ArrayList<>();
        while (!remainingRevisions.isEmpty()) {
            final U minimum = getSomeMinimum(remainingRevisions);
            ret.add(minimum);
            remainingRevisions.remove(minimum);
        }
        return ret;
    }
}
