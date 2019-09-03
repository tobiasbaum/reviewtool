package de.setsoftware.reviewtool.ordering.efficientalgorithm;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * A match set with a distinguished center element that shall be put before the others.
 *
 * @param <T> Type of the stops.
 */
public class StarMatchSet<T> extends MatchSet<T> {

    private final Set<T> distinguishedPart;

    /**
     * Creates a new {@link StarMatchSet}.
     * @param distinguishedPart The center element(s).
     * @param set All matched elements (including the center).
     */
    public StarMatchSet(Set<T> distinguishedPart, Collection<T> set) {
        super(set);
        this.distinguishedPart = distinguishedPart;
    }

    /**
     * Creates a new {@link StarMatchSet}.
     * @param distinguishedPart The center element.
     * @param set All matched elements (including the center).
     */
    public StarMatchSet(T distinguishedPart, Collection<T> set) {
        this(Collections.singleton(distinguishedPart), set);
    }

    public Set<T> getDistinguishedPart() {
        return this.distinguishedPart;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof StarMatchSet<?>)) {
            return false;
        }
        final StarMatchSet<?> other = (StarMatchSet<?>) o;
        return this.distinguishedPart.equals(other.distinguishedPart)
            && this.getChangeParts().equals(other.getChangeParts());
    }

}
