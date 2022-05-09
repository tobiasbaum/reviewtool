package de.setsoftware.reviewtool.ordering;

import de.setsoftware.reviewtool.ordering.efficientalgorithm.MatchSet;

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
     * Returns the setting for determining whether this group shall be shown explicitly as a node in the tree view.
     */
    public abstract HierarchyExplicitness getExplicitness();

    /**
     * The text for the tree node. Is ignored when not shown in the tree.
     */
    public abstract String getDescription();

}
