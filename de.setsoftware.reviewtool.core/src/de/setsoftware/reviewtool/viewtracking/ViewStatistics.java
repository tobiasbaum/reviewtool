package de.setsoftware.reviewtool.viewtracking;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.setsoftware.reviewtool.base.WeakListeners;
import de.setsoftware.reviewtool.model.api.IFragment;
import de.setsoftware.reviewtool.model.changestructure.Stop;
import de.setsoftware.reviewtool.model.changestructure.Tour;
import de.setsoftware.reviewtool.model.changestructure.ToursInReview;

/**
 * Statistics on if and how long portions of files have been viewed.
 */
public class ViewStatistics {

    private final Map<File, ViewStatisticsForFile> files = new HashMap<>();
    private final WeakListeners<IViewStatisticsListener> listeners = new WeakListeners<>();
    private final Set<Stop> explicitMarks = new HashSet<>();

    /**
     * Marks that the given portion of the file has been viewed for one time slot.
     * Line numbers are one-based and both inclusive.
     */
    public void mark(File filePath, int fromLine, int toLine) {
        final File absFile = filePath.getAbsoluteFile();
        this.getOrCreate(absFile).mark(fromLine, toLine);
        this.notifyListeners(absFile);
    }

    /**
     * Marks that the file has been viewed for one time slot when no specific information
     * on the viewed part of the file is available.
     */
    public void markUnknownPosition(File filePath) {
        final File absFile = filePath.getAbsoluteFile();
        this.getOrCreate(absFile).markUnknownPosition();
        this.notifyListeners(absFile);
    }

    /**
     * Marks the given stop as checked when it is currently not, or removes the mark when it
     * is currently present. This kind of explicit manual marking is orthogonal to
     * the automatic marking of viewed portions of the file.
     */
    public void toggleExplicitlyCheckedMark(Collection<Stop> stops) {
        final Set<File> files = new LinkedHashSet<>();
        for (final Stop stop : stops) {
            if (this.isMarkedAsChecked(stop)) {
                this.explicitMarks.remove(stop);
            } else {
                this.explicitMarks.add(stop);
            }
            files.add(stop.getAbsoluteFile());
        }
        for (final File file : files) {
            this.notifyListeners(file);
        }
    }

    /**
     * Returns true iff this stop has been explicitly marked as checked.
     * This kind of explicit manual marking is orthogonal to the automatic marking of
     * viewed portions of the file.
     */
    public boolean isMarkedAsChecked(Stop stop) {
        return this.explicitMarks.contains(stop);
    }

    private ViewStatisticsForFile getOrCreate(File absFile) {
        ViewStatisticsForFile ret = this.files.get(absFile);
        if (ret == null) {
            ret = new ViewStatisticsForFile();
            this.files.put(absFile, ret);
        }
        return ret;
    }

    /**
     * Returns a number between 0.0 and 1.0 that denotes the time that the given stop
     * has been viewed. Zero means "not viewed at all", one means "every line has been
     * viewed long enough".
     */
    public ViewStatDataForStop determineViewRatio(Stop f, int longEnoughCount) {
        final File absFile = f.getAbsoluteFile();
        final ViewStatisticsForFile stats = this.files.get(absFile);
        if (stats == null) {
            return ViewStatDataForStop.NO_VIEWS;
        }
        final IFragment fragment = f.getMostRecentFragment();
        if (fragment == null) {
            return stats.determineViewRatioWithoutPosition(longEnoughCount);
        } else {
            final int toCorrection = fragment.getTo().getColumn() == 1 ? -1 : 0;
            return stats.determineViewRatio(fragment.getFrom().getLine(),
                    fragment.getTo().getLine() + toCorrection, longEnoughCount);
        }
    }

    private void notifyListeners(File absFile) {
        for (final IViewStatisticsListener l : this.listeners.getListeners()) {
            l.statisticsChanged(absFile);
        }
    }

    public void addListener(IViewStatisticsListener listener) {
        this.listeners.add(listener);
    }

    /**
     * Callback for events that can occur when the next unvisited stop is determined.
     */
    public static interface INextStopCallback {
        /**
         * Is called when the next stop belongs to a new tour.
         */
        public abstract void newTourStarted(Tour tour);

        public abstract void wrappedAround();
    }

    /**
     * Determines the next relevant stop that has not been viewed at all, starting from the given
     * stop. When the tour changes during this search, the given callback will be notified.
     * When no unvisited relevant stop exists after the given stop, search continues from the start.
     * If no unvisited relevant stop exists at all, null is returned.
     */
    public Stop getNextUnvisitedStop(
            ToursInReview tours, Stop currentStop, INextStopCallback nextStopCallback) {

        if (tours.getTours().isEmpty()) {
            return null;
        }

        final int startTourIndex = tours.findTourIndexWithStop(currentStop);

        final int tourCount = tours.getTours().size();
        for (int i = 0; i <= tourCount; i++) {
            final Tour tour = tours.getTours().get((startTourIndex + i) % tourCount);
            final List<Stop> remainingStops;
            if (i == 0) {
                remainingStops = tour.getStopsAfter(currentStop);
            } else if (i == tourCount) {
                remainingStops = tour.getStopsBefore(currentStop);
            } else {
                remainingStops = tour.getStops();
            }

            for (final Stop possibleNextStop : remainingStops) {
                if (this.shouldStillVisit(possibleNextStop)) {
                    if (i > 0 || !tour.getStops().contains(currentStop)) {
                        if (startTourIndex + i >= tourCount) {
                            nextStopCallback.wrappedAround();
                        }
                        if (i < tourCount) {
                            nextStopCallback.newTourStarted(tour);
                        }
                    }
                    return possibleNextStop;
                }
            }
        }

        return null;
    }

    private boolean shouldStillVisit(final Stop possibleNextStop) {
        return this.determineViewRatio(possibleNextStop, 1).isPartlyUnvisited()
                && !this.explicitMarks.contains(possibleNextStop)
                && !possibleNextStop.isIrrelevantForReview();
    }

}
