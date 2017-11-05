package de.setsoftware.reviewtool.ordering;

import java.util.Collection;

import de.setsoftware.reviewtool.ordering.efficientalgorithm.MatchSet;
import de.setsoftware.reviewtool.ordering.efficientalgorithm.PositionRequest;

/**
 * A match set and further data related to it that is needed to group and sort the stops
 * and arrange them hierarchically.
 */
public interface OrderingInfo {

    /**
     * The match set to group.
     */
    public abstract MatchSet<ChangePart> getMatchSet();

    /**
     * The position requests for the match set. Can be empty when no positioning is required.
     */
    public abstract Collection<? extends PositionRequest<ChangePart>> getPositionRequests();

    /**
     * Returns true iff this group shall be shown explicitly as a node in the tree view.
     */
    public abstract boolean shallBeExplicit();

    /**
     * The text for the tree node. Is ignored when not shown in the tree.
     */
    public abstract String getDescription();

}
