package de.setsoftware.reviewtool.ordering;

import java.util.Collection;

import de.setsoftware.reviewtool.model.changestructure.Stop;
import de.setsoftware.reviewtool.ordering.efficientalgorithm.MatchSet;
import de.setsoftware.reviewtool.ordering.efficientalgorithm.PositionRequest;

public interface OrderingInfo {

    public abstract MatchSet<Stop> getMatchSet();

    public abstract Collection<? extends PositionRequest<Stop>> getPositionRequests();

    public abstract boolean shallBeExplicit();

    public abstract String getDescription();

}
