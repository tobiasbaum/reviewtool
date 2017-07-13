package de.setsoftware.reviewtool.ordering2.efficientalgorithm;

import java.util.LinkedHashSet;
import java.util.Set;

public class MatchSet<T> {

    private final Set<T> parts;

    public MatchSet(Set<T> set) {
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

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MatchSet<?>)) {
            return false;
        }
        final MatchSet<?> other = (MatchSet<?>) o;
        return this.parts.equals(other.parts);
    }

}
