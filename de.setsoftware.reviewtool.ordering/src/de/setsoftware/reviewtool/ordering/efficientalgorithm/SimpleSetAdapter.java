package de.setsoftware.reviewtool.ordering.efficientalgorithm;

import java.util.Set;

/**
 * Adapter from java.util.Sets to SimpleSets.
 *
 * @param <T> Type of the items.
 */
public class SimpleSetAdapter<T> implements SimpleSet<T> {

    private final Set<T> wrapped;

    public SimpleSetAdapter(Set<T> wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public boolean contains(T item) {
        return this.wrapped.contains(item);
    }

    @Override
    public Set<T> toSet() {
        return this.wrapped;
    }

}
