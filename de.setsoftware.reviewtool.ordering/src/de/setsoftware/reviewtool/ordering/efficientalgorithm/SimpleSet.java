package de.setsoftware.reviewtool.ordering.efficientalgorithm;

import java.util.Set;

/**
 * A very simple interface for sets, used for high-speed implementations for certain use cases.
 *
 * @param <T> Type of the items.
 */
public interface SimpleSet<T> {

    public abstract boolean contains(T item);

    public abstract Set<T> toSet();

}
