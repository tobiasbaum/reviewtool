package de.setsoftware.reviewtool.base;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implements the {@link IMultimap} interface using an ordinary {@link Map}.
 * @param <K> Type of the key.
 * @param <V> Type of the value.
 */
public final class Multimap<K, V> implements Serializable, IMultimap<K, V> {

    private static final long serialVersionUID = -2669248894482975071L;

    private final Map<K, List<V>> map = new LinkedHashMap<>();

    @Override
    public boolean isEmpty() {
        return this.map.isEmpty();
    }

    @Override
    public void put(final K key, final V value) {
        List<V> list = this.map.get(key);
        if (list == null) {
            list = new ArrayList<>();
            this.map.put(key, list);
        }
        list.add(value);
    }

    @Override
    public List<V> get(final K key) {
        final List<V> list = this.map.get(key);
        return list == null ? Collections.<V>emptyList() : Collections.unmodifiableList(list);
    }

    @Override
    public void putAll(final IMultimap<? extends K, ? extends V> other) {
        for (final Map.Entry<? extends K, ? extends List<? extends V>> entry : other.entrySet()) {
            for (final V value : entry.getValue()) {
                this.put(entry.getKey(), value);
            }
        }
    }

    @Override
    public Set<K> keySet() {
        return Collections.unmodifiableSet(this.map.keySet());
    }

    @Override
    public Set<Map.Entry<K, List<V>>> entrySet() {
        return Collections.unmodifiableSet(this.map.entrySet());
    }

    @Override
    public void removeKey(final K key) {
        this.map.remove(key);
    }

    @Override
    public void removeValue(final K key, final V value) {
        final List<V> list = this.map.get(key);
        if (list != null) {
            list.remove(value);
            if (list.isEmpty()) {
                this.removeKey(key);
            }
        }
    }

    @Override
    public String toString() {
        return this.map.toString();
    }

    @Override
    public int hashCode() {
        return this.map.hashCode();
    }

    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof Multimap)) {
            return false;
        }
        final Multimap<?, ?> m = (Multimap<?, ?>) o;
        return this.map.equals(m.map);
    }

    @Override
    public K keyWithMaxNumberOfValues() {
        int maxSize = 0;
        K maxKey = null;
        for (final Map.Entry<K, List<V>> e : this.map.entrySet()) {
            if (e.getValue().size() > maxSize) {
                maxSize = e.getValue().size();
                maxKey = e.getKey();
            }
        }
        return maxKey;
    }

    @Override
    public void sortValues() {
        for (final Map.Entry<K, List<V>> e : this.map.entrySet()) {
            @SuppressWarnings("unchecked")
            final List<Comparable<Object>> values = (List<Comparable<Object>>) e.getValue();
            Collections.sort(values);
        }
    }

    /**
     * Sorts all value lists with the given comparator.
     */
    public void sortValues(Comparator<? super V> comparator) {
        for (final Map.Entry<K, List<V>> e : this.map.entrySet()) {
            final List<V> values = e.getValue();
            Collections.sort(values, comparator);
        }
    }

    @Override
    public IMultimap<K, V> readOnlyView() {
        return new IMultimap<K, V>() {

            @Override
            public boolean isEmpty() {
                return Multimap.this.isEmpty();
            }

            @Override
            public void put(final K key, final V value) {
                throw new UnsupportedOperationException();
            }

            @Override
            public List<V> get(final K key) {
                return Multimap.this.get(key);
            }

            @Override
            public void putAll(final IMultimap<? extends K, ? extends V> other) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Set<K> keySet() {
                return Multimap.this.keySet();
            }

            @Override
            public Set<Map.Entry<K, List<V>>> entrySet() {
                return Multimap.this.entrySet();
            }

            @Override
            public void removeKey(final K key) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void removeValue(final K key, final V value) {
                throw new UnsupportedOperationException();
            }

            @Override
            public K keyWithMaxNumberOfValues() {
                return Multimap.this.keyWithMaxNumberOfValues();
            }

            @Override
            public void sortValues() {
                throw new UnsupportedOperationException();
            }

            @Override
            public IMultimap<K, V> readOnlyView() {
                return this;
            }
        };
    }

}
