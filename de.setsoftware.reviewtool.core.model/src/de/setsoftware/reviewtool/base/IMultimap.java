package de.setsoftware.reviewtool.base;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A simple multimap with a list of values for each key.
 * @param <K> Type of the key.
 * @param <V> Type of the value.
 */
public interface IMultimap<K, V> {

    /**
     * Returns {@code true} iff the multimap is empty, else {@code false}.
     */
    public abstract boolean isEmpty();

    /**
     * Add the given key value pair. If there already is an entry
     * for the given key, add the value to the end of this key's list.
     */
    public abstract void put(K key, V value);

    /**
     * Get the values for the given key. If there are no values, the
     * empty list is returned.
     */
    public abstract List<V> get(K key);

    /**
     * Adds all entries from the given map to this map.
     */
    public abstract void putAll(IMultimap<? extends K, ? extends V> other);

    /**
     * Returns all keys of this multimap.
     */
    public abstract Set<K> keySet();

    /**
     * Returns all entries of this multimap.
     */
    public abstract Set<Map.Entry<K, List<V>>> entrySet();

    /**
     * Removes the entry for the given key (including all values).
     */
    public abstract void removeKey(K key);

    /**
     * Removes the value from the entries for the given key.
     */
    public abstract void removeValue(K key, V value);

    /**
     * Returns one of the keys which have a maximal number of assigned values.
     */
    public abstract K keyWithMaxNumberOfValues();

    /**
     * Sorts all value lists.
     * @pre The values are comparable.
     */
    public abstract void sortValues();

    /**
     * Returns a read-only view of this multimap where all modifying operations throw a
     * {@link UnsupportedOperationException}.
     */
    public IMultimap<K, V> readOnlyView();
}
