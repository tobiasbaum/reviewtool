package de.setsoftware.reviewtool.base;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A collection of observers/listeners, referenced by weak references.
 *
 * @param <T> Type of the listener.
 */
public class WeakListeners<T> implements Iterable<T> {

    private final List<WeakReference<T>> listeners = new ArrayList<>();

    /**
     * Returns a snapshot of the currently registered iterators.
     * Also performs housekeeping of references that have been garbage collected.
     */
    public List<T> getListeners() {
        final List<T> ret = new ArrayList<>();
        final Iterator<WeakReference<T>> iter = this.listeners.iterator();
        while (iter.hasNext()) {
            final T s = iter.next().get();
            if (s != null) {
                ret.add(s);
            } else {
                iter.remove();
            }
        }
        return ret;
    }

    public void add(T listener) {
        this.listeners.add(new WeakReference<>(listener));
    }

    /**
     * Removes the given listener.
     */
    public void remove(T toRemove) {
        final Iterator<WeakReference<T>> iter = this.listeners.iterator();
        while (iter.hasNext()) {
            final T s = iter.next().get();
            if (toRemove == s) {
                iter.remove();
            }
        }
    }

    @Override
    public Iterator<T> iterator() {
        return this.getListeners().iterator();
    }
}
