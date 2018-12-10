package de.setsoftware.reviewtool.ordering.efficientalgorithm;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A set of change parts/stops that have been matched by a grouping pattern.
 *
 * @param <T> Type of the stops.
 */
public abstract class MatchSet<T> {

    private final Set<T> parts;

    public MatchSet(Collection<T> set) {
        this.parts = new LinkedHashSet<>(set);
    }

    public Set<T> getChangeParts() {
        return this.parts;
    }

    @Override
    public String toString() {
        return this.parts.toString();
    }

    @Override
    public int hashCode() {
        return this.parts.hashCode();
    }

}
