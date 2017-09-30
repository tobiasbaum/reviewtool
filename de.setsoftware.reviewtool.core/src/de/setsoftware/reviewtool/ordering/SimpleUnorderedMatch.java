package de.setsoftware.reviewtool.ordering;

import java.util.Collection;
import java.util.Collections;

import de.setsoftware.reviewtool.model.changestructure.Stop;
import de.setsoftware.reviewtool.ordering.efficientalgorithm.MatchSet;
import de.setsoftware.reviewtool.ordering.efficientalgorithm.PositionRequest;

/**
 * A simple implementation of {@link OrderingInfo} that does not have any position requests.
 */
public class SimpleUnorderedMatch implements OrderingInfo {

    private final boolean explicit;
    private final String description;
    private final MatchSet<Stop> matchSet;

    public SimpleUnorderedMatch(boolean explicit, String description, Collection<Stop> stops) {
        this.explicit = explicit;
        this.description = description;
        this.matchSet = new MatchSet<>(stops);
    }

    @Override
    public MatchSet<Stop> getMatchSet() {
        return this.matchSet;
    }

    @Override
    public Collection<? extends PositionRequest<Stop>> getPositionRequests() {
        return Collections.emptyList();
    }

    @Override
    public boolean shallBeExplicit() {
        return this.explicit;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

}
