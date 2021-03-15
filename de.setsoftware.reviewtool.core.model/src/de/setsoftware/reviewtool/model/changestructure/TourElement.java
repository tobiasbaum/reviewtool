package de.setsoftware.reviewtool.model.changestructure;

import java.util.List;

import de.setsoftware.reviewtool.model.api.IClassification;

/**
 * Super type for elements that can be contained in a review tour.
 */
public abstract class TourElement {

    protected abstract void fillStopsInto(List<Stop> buffer);

    public abstract IClassification[] getClassification();

    /**
     * Returns a string with the classifications of this element.
     * Adds a line break to the end when there are classifications.
     */
    public final String getClassificationFormatted() {
        final IClassification[] cl = this.getClassification();
        if (cl.length == 0) {
            return "";
        }
        final StringBuilder ret = new StringBuilder(cl[0].getName());
        for (int i = 1; i < cl.length; i++) {
            ret.append(", ").append(cl[i].getName());
        }
        ret.append('\n');
        return ret.toString();
    }
}
