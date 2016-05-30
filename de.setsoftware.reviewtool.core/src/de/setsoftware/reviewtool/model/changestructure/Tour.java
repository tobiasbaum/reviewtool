package de.setsoftware.reviewtool.model.changestructure;

import java.util.ArrayList;
import java.util.List;

/**
 * A tour through a part of the changes. The consists of stops in a certain order, with
 * each stop belonging to some part of the change.
 * <p/>
 * The Tour+Stop metaphor is borrowed from the JTourBus tool.
 */
public class Tour {

    private final String description;
    private final List<Stop> stops = new ArrayList<>();

    public Tour(String description, List<Stop> list) {
        this.description = description;
        this.stops.addAll(list);
    }

    @Override
    public String toString() {
        return "Stop: " + this.description + ", " + this.stops;
    }

    public List<Stop> getStops() {
        return this.stops;
    }

    public String getDescription() {
        return this.description;
    }

}
