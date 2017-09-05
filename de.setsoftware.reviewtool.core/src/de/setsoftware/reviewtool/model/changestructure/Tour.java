package de.setsoftware.reviewtool.model.changestructure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import de.setsoftware.reviewtool.base.Multimap;
import de.setsoftware.reviewtool.model.api.IRevisionedFile;
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

    public String getDescription() {
        return this.description;
    }

    /**
     * Merges this tour with the given tour.
     * The description of the result is the concatenation of both descriptions, joined with " + ".
     * The stops are merged recursively and sorted by file and position in file. When in doubt, files
     * from this come before files from the other tour.
     */
    public Tour mergeWith(Tour t2) {
        final Multimap<IRevisionedFile, Stop> stopsInFile = new Multimap<>();

        for (final Stop s : this.stops) {
            stopsInFile.put(s.getMostRecentFile(), s);
        }
        for (final Stop s : t2.stops) {
            stopsInFile.put(s.getMostRecentFile(), s);
        }

        final List<Stop> mergedStops = new ArrayList<>();
        for (final Entry<IRevisionedFile, List<Stop>> e : stopsInFile.entrySet()) {
            mergedStops.addAll(this.sortByLine(this.mergeInSameFile(e.getValue())));
        }
        return new Tour(this.getDescription() + " + " + t2.getDescription(), mergedStops);
    }

    private List<Stop> mergeInSameFile(List<Stop> stopsInSameFile) {
        final List<Stop> ret = new ArrayList<>();
        final List<Stop> remainingStops = new LinkedList<>(stopsInSameFile);
        while (!remainingStops.isEmpty()) {
            Stop curMerged = remainingStops.remove(0);
            final Iterator<Stop> iter = remainingStops.iterator();
            while (iter.hasNext()) {
                final Stop s = iter.next();
                if (curMerged.canBeMergedWith(s)) {
                    iter.remove();
                    curMerged = curMerged.merge(s);
                }
            }
            ret.add(curMerged);
        }
        return ret;
    }

    private List<Stop> sortByLine(List<Stop> stopsInSameFile) {
        Collections.sort(stopsInSameFile, new Comparator<Stop>() {
            @Override
            public int compare(Stop o1, Stop o2) {
                return Integer.compare(this.getLine(o1), this.getLine(o2));
            }

            private int getLine(Stop s) {
                if (s.getMostRecentFragment() == null) {
                    return 0;
                } else {
                    return s.getMostRecentFragment().getFrom().getLine();
                }
            }
        });
        return stopsInSameFile;
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
     * Returns a list with all stops after the given stop.
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

}
