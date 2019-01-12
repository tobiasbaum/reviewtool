package de.setsoftware.reviewtool.ordering;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import de.setsoftware.reviewtool.ordering.efficientalgorithm.MatchSet;
import de.setsoftware.reviewtool.ordering.efficientalgorithm.StarMatchSet;
import de.setsoftware.reviewtool.ordering.efficientalgorithm.UnorderedMatchSet;

/**
 * A simple implementation of {@link OrderingInfo}.
 */
public class OrderingInfoImpl implements OrderingInfo {

    private final HierarchyExplicitness explicit;
    private final String description;
    private final MatchSet<ChangePart> matchSet;

    private OrderingInfoImpl(
            HierarchyExplicitness explicit, String description, MatchSet<ChangePart> matchSet) {
        this.explicit = explicit;
        this.description = description;
        this.matchSet = matchSet;
    }

    /**
     * Creates an {@link OrderingInfo} without position requests (only grouping).
     */
    public static OrderingInfo unordered(
            HierarchyExplicitness explicit,
            String description,
            Collection<ChangePart> changeParts) {
        return new OrderingInfoImpl(explicit, description, new UnorderedMatchSet<>(changeParts));
    }

    /**
     * Creates an {@link OrderingInfo} that puts the center of a star pattern first.
     */
    public static OrderingInfo star(
            HierarchyExplicitness explicit,
            String description,
            ChangePart center,
            Collection<ChangePart> otherChangeParts) {
        final Set<ChangePart> combined = new HashSet<>(otherChangeParts);
        combined.add(center);
        return new OrderingInfoImpl(explicit, description, new StarMatchSet<>(center, combined));
    }

    @Override
    public MatchSet<ChangePart> getMatchSet() {
        return this.matchSet;
    }

    @Override
    public HierarchyExplicitness getExplicitness() {
        return this.explicit;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public int hashCode() {
        return this.matchSet.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof OrderingInfoImpl)) {
            return false;
        }
        final OrderingInfoImpl m = (OrderingInfoImpl) o;
        if (this.explicit != m.explicit) {
            return false;
        }
        if (this.explicit != HierarchyExplicitness.NONE && !this.description.equals(m.description)) {
            return false;
        }
        return this.matchSet.equals(m.matchSet);
    }

    @Override
    public String toString() {
        return this.explicit + "," + this.description + "," + this.matchSet;
    }

}
