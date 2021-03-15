package de.setsoftware.reviewtool.base;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * A simple multiset.
 * @param <T> The item type.
 */
public class Multiset<T> {

    /**
     * Mutable counter value.
     */
    private static final class Counter {
        private int cnt;
    }

    private final Map<T, Counter> items = new LinkedHashMap<>();

    /**
     * Add an item to the multiset.
     */
    public void add(T item) {
        Counter c = this.items.get(item);
        if (c == null) {
            c = new Counter();
            this.items.put(item, c);
        }
        c.cnt++;
    }

    /**
     * Returns true iff the given is contained at least once in the map.
     */
    public boolean contains(T item) {
        final Counter c = this.items.get(item);
        return c != null;
    }

    /**
     * Returns the count for the given item.
     */
    public int get(Object item) {
        final Counter c = this.items.get(item);
        return c == null ? 0 : c.cnt;
    }

    /**
     * Removes one occurrence of the given item from this multiset.
     */
    public void remove(T item) {
        final Counter c = this.items.get(item);
        if (c == null) {
            return;
        }
        c.cnt--;
        if (c.cnt <= 0) {
            this.items.remove(item);
        }
    }

    /**
     * Returns only the keys, without the counts.
     */
    public Set<T> keySet() {
        return this.items.keySet();
    }

    /**
     * Returns the item which occurs most often. Returns the first such if
     * there are several with the same count.
     */
    public T getMostCommonItem() {
        T mostCommon = null;
        for (final T g : this.keySet()) {
            if (mostCommon == null || this.get(g) > this.get(mostCommon)) {
                mostCommon = g;
            }
        }
        return mostCommon;
    }

    /**
     * Returns true iff there is no item in the set.
     */
    public boolean isEmpty() {
        return this.items.isEmpty();
    }

}
