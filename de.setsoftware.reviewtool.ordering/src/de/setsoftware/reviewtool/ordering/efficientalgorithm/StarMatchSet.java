package de.setsoftware.reviewtool.ordering.efficientalgorithm;

import java.util.Collection;

/**
 * A match set with a distinguished center element that shall be put before the others.
 *
 * @param <T> Type of the stops.
 */
public class StarMatchSet<T> extends MatchSet<T> {

    private final T distinguishedPart;

    /**
     * Creates a new {@link StarMatchSet}.
     * @param distinguishedPart The center element.
     * @param set All matched elements (including the center).
     */
    public StarMatchSet(T distinguishedPart, Collection<T> set) {
        super(set);
        this.distinguishedPart = distinguishedPart;
    }

    public T getDistinguishedPart() {
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
