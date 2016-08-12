package de.setsoftware.reviewtool.model.changestructure;

import java.util.List;

/**
 * Interface for strategies that can restructure tours.
 * Normally such a restructuring will result in a list of tours that is easier to understand or shorter.
 */
public interface ITourRestructuring {

    /**
     * Returns a description for this restructuring strategy.
     * This description is shown to the user, together with the restructuring result, when choosing
     * between several possible structures.
     */
    public abstract String getDescription();

    /**
     * Creates and returns a restructuring of the given tours.
     * When the restructuring would result in the same tours, or it is not applicable, null must be returned.
     * The implementing is allowed to change the given list in-place. The result is always taken from the
     * returned list.
     */
    public abstract List<? extends Tour> restructure(List<Tour> originalTours);

}
