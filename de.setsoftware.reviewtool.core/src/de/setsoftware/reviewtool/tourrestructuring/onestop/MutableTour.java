package de.setsoftware.reviewtool.tourrestructuring.onestop;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import de.setsoftware.reviewtool.base.Util;
import de.setsoftware.reviewtool.model.changestructure.IReviewElement;
import de.setsoftware.reviewtool.model.changestructure.Stop;
import de.setsoftware.reviewtool.model.changestructure.Tour;

/**
 * A helper class for restructuring the tours, containing the information like a tour,
 * but being mutable instead.
 */
class MutableTour implements IReviewElement {

    /**
     * Helper class containing data about a stop and its position in a (mutable) tour.
     */
    private static class StopInTour {

        private final MutableTour tour;
        private final int index;

        public StopInTour(MutableTour tour, int stopIndex) {
            this.tour = tour;
            this.index = stopIndex;
        }

        public void merge(Stop s) {
            this.tour.mergeStop(this.index, s);
        }

    }

    private final Set<String> descriptionParts;
    private final List<Stop> stops;
    private final boolean isVisible;

    public MutableTour(Tour t) {
        this.descriptionParts = new LinkedHashSet<>();
        this.descriptionParts.add(t.getDescription());
        this.stops = new ArrayList<>(t.getStops());
        this.isVisible = t.isVisible();
    }

    public static List<Tour> toTours(List<MutableTour> mutableTours) {
        final List<Tour> ret = new ArrayList<>();
        for (final MutableTour t : mutableTours) {
            ret.add(new Tour(
                    Util.implode(t.descriptionParts, " + "),
                    t.stops,
                    t.isVisible()));
        }
        return ret;
    }

    public boolean canBeResolvedCompletely(List<MutableTour> mutableTours, int excludedIndex) {
        for (final Stop s : this.stops) {
            if (!this.canBeMerged(s, mutableTours, excludedIndex)) {
                return false;
            }
        }
        return true;
    }

    private boolean canBeMerged(Stop s, List<MutableTour> mutableTours, int excludedIndex) {
        final StopInTour toMergeWith = this.getStopToMergeWith(s, mutableTours, excludedIndex);
        return toMergeWith != null && !mutableTours.get(excludedIndex).equals(toMergeWith.tour);
    }

    public boolean resolve(List<MutableTour> mutableTours, int currentIndex) {
        boolean didSomething = false;
        final Iterator<Stop> iter = this.stops.iterator();
        while (iter.hasNext()) {
            final Stop s = iter.next();
            final StopInTour toMergeWith = this.getStopToMergeWith(s, mutableTours, currentIndex);
            if (toMergeWith != null) {
                toMergeWith.merge(s);
                toMergeWith.tour.descriptionParts.addAll(this.descriptionParts);
                iter.remove();
                didSomething = true;
            }
        }
        return didSomething;
    }

    private StopInTour getStopToMergeWith(Stop s, List<MutableTour> mutableTours, int currentIndex) {
        // try to merge with stops of later tours
        for (int tourIndex = currentIndex + 1; tourIndex < mutableTours.size(); tourIndex++) {
            final StopInTour mergeWith = mutableTours.get(tourIndex).getStopToMergeWith(s);
            if (mergeWith != null) {
                return mergeWith;
            }
        }
        // try to merge with stops in this tour
        {
            final StopInTour mergeWith = mutableTours.get(currentIndex).getStopToMergeWith(s);
            if (mergeWith != null) {
                return mergeWith;
            }
        }
        // try to merge with stops of earlier tours (only necessary for resolving complete tours)
        for (int tourIndex = currentIndex - 1; tourIndex >= 0; tourIndex--) {
            final StopInTour mergeWith = mutableTours.get(tourIndex).getStopToMergeWith(s);
            if (mergeWith != null) {
                return mergeWith;
            }
        }
        return null;
    }

    private StopInTour getStopToMergeWith(Stop s) {
        for (int stopIndex = 0; stopIndex < this.stops.size(); stopIndex++) {
            final Stop stop = this.stops.get(stopIndex);
            if (!s.equals(stop) && stop.canBeMergedWith(s)) {
                return new StopInTour(this, stopIndex);
            }
        }
        return null;
    }

    public void mergeStop(int index, Stop s) {
        this.stops.set(index, this.stops.get(index).merge(s));
    }

    public boolean isEmpty() {
        return this.stops.isEmpty();
    }

    @Override
    public boolean isVisible() {
        return this.isVisible;
    }
}
