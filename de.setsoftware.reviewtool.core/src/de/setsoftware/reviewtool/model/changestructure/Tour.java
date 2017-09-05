package de.setsoftware.reviewtool.model.changestructure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.setsoftware.reviewtool.telemetry.TelemetryEventBuilder;
import de.setsoftware.reviewtool.telemetry.TelemetryParamSource;

/**
 * A tour through a part of the changes. The consists of stops in a certain order, with
 * each stop belonging to some part of the change. Tours can be structured into sub-tours,
 * forming a hierarchy (composite).
 * <p/>
 * The Tour+Stop metaphor is borrowed from the JTourBus tool.
 * <p/>
 * A tour is immutable.
 */
public class Tour extends TourElement {

    private final String description;
    private final List<Stop> stops = new ArrayList<>();

    public Tour(String description, List<? extends Stop> list) {
        this.description = description;
        this.stops.addAll(list);
    }

    @Override
    public String toString() {
        return "Tour: " + this.description + ", " + this.stops;
    }

    @Override
    public int hashCode() {
        return this.description.hashCode() + this.stops.size();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Tour)) {
            return false;
        }
        final Tour t = (Tour) o;
        return this.description.equals(t.description)
            && this.stops.equals(t.stops);
    }

    public List<Stop> getStops() {
        return Collections.unmodifiableList(this.stops);
    }

    public List<? extends TourElement> getChildren() {
        return Collections.unmodifiableList(this.stops);
    }

    public String getDescription() {
        return this.description;
    }

    /**
     * Returns a list with all stops after the given stop.
     * If the given stop is not contained in this, all stops are returned.
     */
    public List<Stop> getStopsAfter(Stop currentStop) {
        final int index = this.stops.indexOf(currentStop);
        return this.stops.subList(index + 1, this.stops.size());
    }

    /**
     * Returns a list with all stops before the given stop.
     * If the given stop is not contained in this, the empty list.
     */
    public List<Stop> getStopsBefore(Stop currentStop) {
        final int index = this.stops.indexOf(currentStop);
        return index <= 0 ? Collections.<Stop>emptyList() : this.stops.subList(0, index - 1);
    }

    public int getNumberOfStops(boolean onlyRelevant) {
        return this.filterRelevance(onlyRelevant).size();
    }

    /**
     * Returns the count of all fragments in the contained stops.
     */
    public int getNumberOfFragments(boolean onlyRelevant) {
        int ret = 0;
        for (final Stop s : this.filterRelevance(onlyRelevant)) {
            ret += s.getNumberOfFragments();
        }
        return ret;
    }

    /**
     * Returns the total count of all added lines (left-hand side of a fragment).
     * A change is counted as both remove and add.
     */
    public int getNumberOfAddedLines(boolean onlyRelevant) {
        int ret = 0;
        for (final Stop s : this.filterRelevance(onlyRelevant)) {
            ret += s.getNumberOfAddedLines();
        }
        return ret;
    }

    /**
     * Returns the total count of all removed lines (left-hand side of a fragment).
     * A change is counted as both remove and add.
     */
    public int getNumberOfRemovedLines(boolean onlyRelevant) {
        int ret = 0;
        for (final Stop s : this.filterRelevance(onlyRelevant)) {
            ret += s.getNumberOfRemovedLines();
        }
        return ret;
    }

    private List<Stop> filterRelevance(boolean onlyRelevant) {
        if (onlyRelevant) {
            final List<Stop> ret = new ArrayList<>();
            for (final Stop s : this.stops) {
                if (!s.isIrrelevantForReview()) {
                    ret.add(s);
                }
            }
            return ret;
        } else {
            return this.stops;
        }
    }

    /**
     * Determines some statistics on the size of the given tours and stores them as params
     * in the given @{link {@link TelemetryEventBuilder}.
     */
    public static TelemetryParamSource determineSize(final List<? extends Tour> tours) {
        return new TelemetryParamSource() {
            @Override
            public void addParams(TelemetryEventBuilder event) {
                if (tours == null) {
                    event.param("cntTours", "-1");
                    return;
                }

                int numberOfStops = 0;
                int numberOfFragments = 0;
                int numberOfAddedLines = 0;
                int numberOfRemovedLines = 0;
                int numberOfStopsRel = 0;
                int numberOfFragmentsRel = 0;
                int numberOfAddedLinesRel = 0;
                int numberOfRemovedLinesRel = 0;
                for (final Tour t : tours) {
                    numberOfStops += t.getNumberOfStops(false);
                    numberOfFragments += t.getNumberOfFragments(false);
                    numberOfAddedLines += t.getNumberOfAddedLines(false);
                    numberOfRemovedLines += t.getNumberOfRemovedLines(false);
                    numberOfStopsRel += t.getNumberOfStops(true);
                    numberOfFragmentsRel += t.getNumberOfFragments(true);
                    numberOfAddedLinesRel += t.getNumberOfAddedLines(true);
                    numberOfRemovedLinesRel += t.getNumberOfRemovedLines(true);
                }
                event.param("cntTours", tours.size());
                event.param("cntStops", numberOfStops);
                event.param("cntFragments", numberOfFragments);
                event.param("cntAddedLines", numberOfAddedLines);
                event.param("cntRemovedLines", numberOfRemovedLines);
                event.param("cntStopsRel", numberOfStopsRel);
                event.param("cntFragmentsRel", numberOfFragmentsRel);
                event.param("cntAddedLinesRel", numberOfAddedLinesRel);
                event.param("cntRemovedLinesRel", numberOfRemovedLinesRel);
            }
        };
    }

    Tour findParentFor(TourElement element) {
        for (final TourElement child : this.stops) {
            if (child.equals(element)) {
                return this;
            } else if (child instanceof Tour) {
                final Tour recursiveResult = ((Tour) child).findParentFor(element);
                if (recursiveResult != null) {
                    return recursiveResult;
                }
            }
        }
        return null;
    }

}
