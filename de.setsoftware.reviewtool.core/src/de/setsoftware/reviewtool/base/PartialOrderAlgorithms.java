package de.setsoftware.reviewtool.base;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
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
     * Returns some maximal element of a partially ordered set.
     * If the underlying revisions are not totally ordered, it is unspecified which maximal element will be returned.
     * If the collection of revisions passed is empty, {@code null} is returned.
     * @param elements The collection of elements where to find some maximal element.
     */
    public static <T extends IPartiallyComparable<T>, U extends T> U getSomeMaximum(final Collection<U> elements) {
        U smallestSoFar = null;
        for (final U e : elements) {
            if (smallestSoFar == null) {
                smallestSoFar = e;
            } else if (smallestSoFar.le(e)) {
                smallestSoFar = e;
            }
        }
        return smallestSoFar;
    }

    /**
     * Returns all minimal elements of a partially ordered set.
     * If the collection of revisions passed is empty, an empty list is returned.
     * @param elements A partially ordered collection of elements where to find all minimal elements.
     */
    public static <T extends IPartiallyComparable<T>, U extends T> List<U> getAllMinimalElements(
            final Collection<U> elements) {

        final List<U> result = new ArrayList<>();
        final Deque<U> input = new ArrayDeque<>(elements);
        while (!input.isEmpty()) {
            final U minimum = input.removeFirst();
            result.add(minimum);

            final Iterator<U> it = input.iterator();
            while (it.hasNext()) {
                final U element = it.next();
                if (minimum.le(element)) {
                    it.remove();
                }
            }
        }

        return result;
    }

    /**
     * Returns all maximal elements of a partially ordered set.
     * If the collection of revisions passed is empty, an empty list is returned.
     * @param elements A partially ordered collection of elements where to find all maximal elements.
     */
    public static <T extends IPartiallyComparable<T>, U extends T> List<U> getAllMaximalElements(
            final Collection<U> elements) {

        final List<U> result = new ArrayList<>();
        final Deque<U> input = new ArrayDeque<>(elements);
        while (!input.isEmpty()) {
            final U maximum = input.removeLast();
            result.add(maximum);

            final Iterator<U> it = input.iterator();
            while (it.hasNext()) {
                final U element = it.next();
                if (element.le(maximum)) {
                    it.remove();
                }
            }
        }

        return result;
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

        final LinkedHashSet<U> remainingElements = new LinkedHashSet<>(toSort);
        final List<U> ret = new ArrayList<>();
        while (!remainingElements.isEmpty()) {
            final U minimum = getSomeMinimum(remainingElements);
            ret.add(minimum);
            remainingElements.remove(minimum);
        }
        return ret;
    }
}
