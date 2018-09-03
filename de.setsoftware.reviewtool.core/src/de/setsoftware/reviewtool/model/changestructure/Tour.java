package de.setsoftware.reviewtool.model.changestructure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import de.setsoftware.reviewtool.model.api.IClassification;
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
    private final List<TourElement> children = new ArrayList<>();

    public Tour(String description, List<? extends TourElement> list) {
        this.description = description;
        this.children.addAll(list);
    }

    @Override
    public String toString() {
        return "Tour: " + this.description + ", " + this.children;
    }

    @Override
    public int hashCode() {
        return this.description.hashCode() + this.children.size();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Tour)) {
            return false;
        }
        final Tour t = (Tour) o;
        return this.description.equals(t.description)
            && this.children.equals(t.children);
    }

    /**
     * Returns all stops (=leaves) in this tour hierarchy.
     */
    public List<Stop> getStops() {
        final List<Stop> buffer = new ArrayList<>();
        this.fillStopsInto(buffer);
        return Collections.unmodifiableList(buffer);
    }

    @Override
    protected void fillStopsInto(List<Stop> buffer) {
        for (final TourElement e : this.children) {
            e.fillStopsInto(buffer);
        }
    }

    public List<? extends TourElement> getChildren() {
        return Collections.unmodifiableList(this.children);
    }

    public String getDescription() {
        return this.description;
    }

    /**
     * Returns a list with all stops after the given stop.
     * If the given stop is not contained in this, all stops are returned.
     */
    public List<Stop> getStopsAfter(Stop currentStop) {
        final List<Stop> allStops = this.getStops();
        final int index = allStops.indexOf(currentStop);
        return allStops.subList(index + 1, allStops.size());
    }

    /**
     * Returns a list with all stops before the given stop.
     * If the given stop is not contained in this, the empty list.
     */
    public List<Stop> getStopsBefore(Stop currentStop) {
        final List<Stop> allStops = this.getStops();
        final int index = allStops.indexOf(currentStop);
        return index <= 0 ? Collections.<Stop>emptyList() : allStops.subList(0, index - 1);
    }

    public int getNumberOfStops(boolean onlyRelevant, Set<? extends IClassification> irrelevantCategories) {
        return this.filterRelevance(onlyRelevant, irrelevantCategories).size();
    }

    /**
     * Returns the count of all fragments in the contained stops.
     */
    public int getNumberOfFragments(boolean onlyRelevant, Set<? extends IClassification> irrelevantCategories) {
        int ret = 0;
        for (final Stop s : this.filterRelevance(onlyRelevant, irrelevantCategories)) {
            ret += s.getNumberOfFragments();
        }
        return ret;
    }

    /**
     * Returns the total count of all added lines (left-hand side of a fragment).
     * A change is counted as both remove and add.
     */
    public int getNumberOfAddedLines(boolean onlyRelevant, Set<? extends IClassification> irrelevantCategories) {
        int ret = 0;
        for (final Stop s : this.filterRelevance(onlyRelevant, irrelevantCategories)) {
            ret += s.getNumberOfAddedLines();
        }
        return ret;
    }

    /**
     * Returns the total count of all removed lines (left-hand side of a fragment).
     * A change is counted as both remove and add.
     */
    public int getNumberOfRemovedLines(boolean onlyRelevant, Set<? extends IClassification> irrelevantCategories) {
        int ret = 0;
        for (final Stop s : this.filterRelevance(onlyRelevant, irrelevantCategories)) {
            ret += s.getNumberOfRemovedLines();
        }
        return ret;
    }

    private List<Stop> filterRelevance(boolean onlyRelevant, Set<? extends IClassification> irrelevantCategories) {
        if (onlyRelevant) {
            final List<Stop> ret = new ArrayList<>();
            for (final Stop s : this.getStops()) {
                if (!s.isIrrelevantForReview(irrelevantCategories)) {
                    ret.add(s);
                }
            }
            return ret;
        } else {
            return this.getStops();
        }
    }

    /**
     * Determines some statistics on the size of the given tours and stores them as params
     * in the given @{link {@link TelemetryEventBuilder}.
     */
    public static TelemetryParamSource determineSize(
            final List<? extends Tour> tours, Set<? extends IClassification> irrelevantCategories) {
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
                    numberOfStops += t.getNumberOfStops(false, irrelevantCategories);
                    numberOfFragments += t.getNumberOfFragments(false, irrelevantCategories);
                    numberOfAddedLines += t.getNumberOfAddedLines(false, irrelevantCategories);
                    numberOfRemovedLines += t.getNumberOfRemovedLines(false, irrelevantCategories);
                    numberOfStopsRel += t.getNumberOfStops(true, irrelevantCategories);
                    numberOfFragmentsRel += t.getNumberOfFragments(true, irrelevantCategories);
                    numberOfAddedLinesRel += t.getNumberOfAddedLines(true, irrelevantCategories);
                    numberOfRemovedLinesRel += t.getNumberOfRemovedLines(true, irrelevantCategories);
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
        for (final TourElement child : this.children) {
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

    /**
     * Returns the intersection of the classification of all children.
     */
    @Override
    public IClassification[] getClassification() {
        if (this.children.isEmpty()) {
            return Classification.NONE;
        }
        final Iterator<TourElement> iter = this.children.iterator();
        final Set<IClassification> ret = new LinkedHashSet<>(Arrays.asList(iter.next().getClassification()));
        while (iter.hasNext()) {
            ret.retainAll(Arrays.asList(iter.next().getClassification()));
            if (ret.isEmpty()) {
                return Classification.NONE;
            }
        }
        return ret.toArray(new IClassification[ret.size()]);
    }

}
