package de.setsoftware.reviewtool.model.changestructure;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

import de.setsoftware.reviewtool.base.ReviewtoolException;
import de.setsoftware.reviewtool.base.WeakListeners;
import de.setsoftware.reviewtool.model.Constants;
import de.setsoftware.reviewtool.model.IMarkerFactory;
import de.setsoftware.reviewtool.telemetry.Telemetry;

/**
 * Manages the current state regarding the changes/tours under review.
 */
public class ToursInReview {

    /**
     * Interface for observers of instances of {@link ToursInReview}.
     */
    public static interface IToursInReviewChangeListener {
        /**
         * Is called when the available tours change (e.g. due to a merge or split).
         */
        public abstract void toursChanged();

        /**
         * Is called when the active tour changes. Will not be called when the active tour
         * changes together with the tours as a whole.
         */
        public abstract void activeTourChanged(Tour oldActive, Tour newActive);
    }

    private final List<Tour> tours;
    private int currentTourIndex;
    private final WeakListeners<IToursInReviewChangeListener> listeners = new WeakListeners<>();

    private ToursInReview(List<Tour> tours) {
        this.tours = tours;
        this.currentTourIndex = 0;
    }

    /**
     * Loads the tours for the given ticket and creates a corresponding {@link ToursInReview}
     * object with initial settings.
     */
    public static ToursInReview create(
            IChangeSource src,
            IChangeSourceUi changeSourceUi,
            ISlicingStrategy slicer,
            String ticketKey) {
        final List<Tour> tours = slicer.toTours(src.getChanges(ticketKey, changeSourceUi));
        return new ToursInReview(tours);
    }

    /**
     * Creates a new object with the given tours (mainly for tests).
     */
    public static ToursInReview create(List<Tour> tours) {
        return new ToursInReview(tours);
    }

    /**
     * Creates markers for the tour stops. Takes the currently active tour into account.
     */
    public void createMarkers(IMarkerFactory markerFactory) {
        if (this.tours.size() <= this.currentTourIndex) {
            return;
        }
        final Tour s = this.tours.get(this.currentTourIndex);
        final Map<IResource, PositionLookupTable> lookupTables = new HashMap<>();
        for (final Stop f : s.getStops()) {
            createMarkerFor(markerFactory, lookupTables, f);
        }
    }

    private static IMarker createMarkerFor(
            IMarkerFactory markerFactory,
            final Map<IResource, PositionLookupTable> lookupTables,
            final Stop f) {

        try {
            final IResource resource = f.getMostRecentFile().determineResource();
            if (resource == null) {
                return null;
            }
            if (f.isDetailedFragmentKnown()) {
                if (!lookupTables.containsKey(resource)) {
                    lookupTables.put(resource, PositionLookupTable.create((IFile) resource));
                }
                final Fragment pos = f.getMostRecentFragment();
                final IMarker marker = markerFactory.createMarker(resource, Constants.STOPMARKER_ID);
                marker.setAttribute(IMarker.LINE_NUMBER, pos.getFrom().getLine());
                marker.setAttribute(IMarker.CHAR_START,
                        lookupTables.get(resource).getCharsSinceFileStart(pos.getFrom()) - 1);
                marker.setAttribute(IMarker.CHAR_END,
                        lookupTables.get(resource).getCharsSinceFileStart(pos.getTo()));
                return marker;
            } else {
                return markerFactory.createMarker(resource, Constants.STOPMARKER_ID);
            }
        } catch (final CoreException | IOException e) {
            throw new ReviewtoolException(e);
        }
    }

    /**
     * Creates a marker for the given fragment.
     * If multiple markers have to be created, use the method that caches lookup tables instead.
     * If a marker could not be created (for example because the resource is not available in Eclipse), null
     * is returned.
     */
    public static IMarker createMarkerFor(
            IMarkerFactory markerFactory,
            final Stop f) {
        return createMarkerFor(markerFactory, new HashMap<IResource, PositionLookupTable>(), f);
    }

    public List<Tour> getTours() {
        return this.tours;
    }

    /**
     * Sets the given tour as the active tour, if it is not already active.
     * Recreates markers accordingly.
     */
    public void ensureTourActive(Tour t, IMarkerFactory markerFactory) throws CoreException {
        this.ensureTourActive(t, markerFactory, true);
    }

    /**
     * Sets the given tour as the active tour, if it is not already active.
     * Recreates markers accordingly.
     */
    public void ensureTourActive(Tour t, IMarkerFactory markerFactory, boolean notify)
        throws CoreException {

        final int index = this.tours.indexOf(t);
        if (index != this.currentTourIndex) {
            this.clearMarkers();
            final Tour oldActive = this.getActiveTour();
            this.currentTourIndex = index;
            this.createMarkers(markerFactory);
            if (notify) {
                for (final IToursInReviewChangeListener l : this.listeners) {
                    l.activeTourChanged(oldActive, this.getActiveTour());
                }
            }
            Telemetry.get().tourActivated(index);
        }
    }

    /**
     * Clears all current tour stop markers.
     */
    public void clearMarkers() throws CoreException {
        ResourcesPlugin.getWorkspace().getRoot().deleteMarkers(
                Constants.STOPMARKER_ID, true, IResource.DEPTH_INFINITE);
    }

    /**
     * Returns the currently active tour or null if there is none (which should only
     * occur when there are no tours).
     */
    public Tour getActiveTour() {
        return this.currentTourIndex >= this.tours.size() || this.currentTourIndex < 0
                ? null : this.tours.get(this.currentTourIndex);
    }

    /**
     * Merges the given tours. If one of them is currently active, the merge result will be active
     * afterwards, otherwise the active tour stays the same. The merged tour's position is the previous
     * position of the "biggest" part.
     */
    public void mergeTours(List<Tour> toursToMerge, IMarkerFactory markerFactory) throws CoreException {
        if (toursToMerge.size() <= 1) {
            return;
        }

        //determine the indices of the tours; they are needed for telemetry logging later
        final List<Integer> tourIndices = new ArrayList<>();
        for (final Tour t : toursToMerge) {
            tourIndices.add(this.tours.indexOf(t));
        }

        //determine the merged tour
        Tour mergeResult = toursToMerge.get(0);
        for (int i = 1; i < toursToMerge.size(); i++) {
            mergeResult = mergeResult.mergeWith(toursToMerge.get(i));
        }

        //save the currently active tour
        final Tour activeTour = this.getActiveTour();

        //replace the largest part with the merge result and remove the old tours
        final Tour largestTour = this.determineLargestTour(toursToMerge);
        this.tours.set(this.tours.indexOf(largestTour), mergeResult);
        this.tours.removeAll(toursToMerge);

        //restore the active tour
        this.currentTourIndex = this.tours.indexOf(activeTour);
        if (this.currentTourIndex < 0) {
            this.ensureTourActive(mergeResult, markerFactory, false);
        }

        for (final IToursInReviewChangeListener l : this.listeners) {
            l.toursChanged();
        }

        Telemetry.get().toursMerged(
                tourIndices,
                this.getNumberOfTours(),
                this.getNumberOfStops());
    }

    private Tour determineLargestTour(List<Tour> toursToMerge) {
        int largestSize = Integer.MIN_VALUE;
        Tour largestTour = null;
        for (final Tour t : toursToMerge) {
            final int curSize = t.getStops().size();
            if (curSize > largestSize) {
                largestSize = curSize;
                largestTour = t;
            }
        }
        return largestTour;
    }

    public void registerListener(IToursInReviewChangeListener listener) {
        this.listeners.add(listener);
    }

    /**
     * Returns all stops (from all tours) that refer to the given file.
     */
    public List<Stop> getStopsFor(File absolutePath) {
        final List<Stop> ret = new ArrayList<>();
        for (final Tour t : this.tours) {
            for (final Stop s : t.getStops()) {
                if (absolutePath.equals(s.getAbsoluteFile())) {
                    ret.add(s);
                }
            }
        }
        return ret;
    }

    /**
     * Returns the (first) tour that contains the given stop.
     * If none exists, -1 is returned.
     */
    public int findTourIndexWithStop(Stop currentStop) {
        for (int i = 0; i < this.tours.size(); i++) {
            for (final Stop s : this.tours.get(i).getStops()) {
                if (s == currentStop) {
                    return i;
                }
            }
        }
        return 0;
    }

    public int getNumberOfTours() {
        return this.tours.size();
    }

    /**
     * Returns the total number of stops in all tours.
     */
    public int getNumberOfStops() {
        int ret = 0;
        for (final Tour t : this.tours) {
            ret += t.getNumberOfStops();
        }
        return ret;
    }

    /**
     * Returns the total number of fragments in all stops of all tours.
     */
    public int getNumberOfFragments() {
        int ret = 0;
        for (final Tour t : this.tours) {
            ret += t.getNumberOfFragments();
        }
        return ret;
    }

    /**
     * Returns the total count of all added lines (right-hand side of a fragment).
     * A change is counted as both remove and add.
     */
    public int getNumberOfAddedLines() {
        int ret = 0;
        for (final Tour t : this.tours) {
            ret += t.getNumberOfAddedLines();
        }
        return ret;
    }

    /**
     * Returns the total count of all removed lines (left-hand side of a fragment).
     * A change is counted as both remove and add.
     */
    public int getNumberOfRemovedLines() {
        int ret = 0;
        for (final Tour t : this.tours) {
            ret += t.getNumberOfRemovedLines();
        }
        return ret;
    }

}
