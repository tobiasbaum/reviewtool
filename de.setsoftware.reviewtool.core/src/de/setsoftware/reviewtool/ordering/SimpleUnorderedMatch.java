package de.setsoftware.reviewtool.ordering;

import java.util.Collection;

import de.setsoftware.reviewtool.ordering.efficientalgorithm.MatchSet;
import de.setsoftware.reviewtool.ordering.efficientalgorithm.UnorderedMatchSet;

/**
 * A simple implementation of {@link OrderingInfo} that does not have any position requests.
 */
public class SimpleUnorderedMatch implements OrderingInfo {

    private final HierarchyExplicitness explicit;
    private final String description;
    private final MatchSet<ChangePart> matchSet;

    public SimpleUnorderedMatch(
            HierarchyExplicitness explicit, String description, Collection<ChangePart> changeParts) {
        this.explicit = explicit;
        this.description = description;
        this.matchSet = new UnorderedMatchSet<>(changeParts);
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
        if (!(o instanceof SimpleUnorderedMatch)) {
            return false;
        }
        final SimpleUnorderedMatch m = (SimpleUnorderedMatch) o;
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
