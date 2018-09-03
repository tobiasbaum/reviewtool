package de.setsoftware.reviewtool.model.changestructure;

import java.util.List;

import de.setsoftware.reviewtool.model.api.IClassification;

/**
 * Super type for elements that can be contained in a review tour.
 */
public abstract class TourElement {

    protected abstract void fillStopsInto(List<Stop> buffer);

    public abstract IClassification[] getClassification();

}
