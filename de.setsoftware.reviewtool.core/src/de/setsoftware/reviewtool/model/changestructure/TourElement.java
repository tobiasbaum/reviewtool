package de.setsoftware.reviewtool.model.changestructure;

import java.util.List;

/**
 * Super type for elements that can be contained in a review tour.
 */
public abstract class TourElement {

    protected abstract void fillStopsInto(List<Stop> buffer);

}
