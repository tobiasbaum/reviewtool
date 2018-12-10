package de.setsoftware.reviewtool.ordering.efficientalgorithm;

import java.util.Collection;

/**
 * A match set without a distinguished element or any other preference for ordering.
 *
 * @param <T> Type of the stops.
 */
public class UnorderedMatchSet<T> extends MatchSet<T> {

    public UnorderedMatchSet(Collection<T> set) {
        super(set);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof UnorderedMatchSet<?>)) {
            return false;
        }
        final MatchSet<?> other = (UnorderedMatchSet<?>) o;
        return this.getChangeParts().equals(other.getChangeParts());
    }

}
