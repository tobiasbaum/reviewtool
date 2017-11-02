package de.setsoftware.reviewtool.ordering.efficientalgorithm;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Simple multiset.
 *
 * @param <T> Type of the items.
 */
public class CountingSet<T> {

    /**
     * Wrapper for the counter value.
     */
    private static final class Counter {
        private int count = 1;
    }

    private final Map<T, Counter> items;

    public CountingSet() {
        this.items = new LinkedHashMap<>();
    }

    public CountingSet(int initialCapacity) {
        this.items = new LinkedHashMap<>(initialCapacity);
    }

    /**
     * Adds all items from the given set to this one, increasing the respective counts by one.
     */
    public void addAll(Set<T> items) {
        for (final T item : items) {
            this.add(item);
        }
    }

    private void add(T item) {
        final Counter c = this.items.get(item);
        if (c == null) {
            this.items.put(item, new Counter());
        } else {
            c.count++;
        }
    }

    /**
     * Returns the count for the given item. Returns 0 if not contained.
     */
    public int get(T item) {
        final Counter c = this.items.get(item);
        return c == null ? 0 : c.count;
    }

    /**
     * Returns all items with a count larger than zero.
     */
    public Set<T> keys() {
        return this.items.keySet();
    }

}
