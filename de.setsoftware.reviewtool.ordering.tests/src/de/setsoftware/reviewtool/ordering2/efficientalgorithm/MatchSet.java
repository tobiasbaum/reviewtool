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

}
