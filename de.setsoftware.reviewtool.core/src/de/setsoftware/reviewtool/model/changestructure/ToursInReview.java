package de.setsoftware.reviewtool.model.changestructure;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

import de.setsoftware.reviewtool.base.Logger;
import de.setsoftware.reviewtool.base.Pair;
import de.setsoftware.reviewtool.base.ReviewtoolException;
import de.setsoftware.reviewtool.base.ValueWrapper;
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

    /**
     * Interface for user interaction during the creation of {@link ToursInReview}.
     */
    public static interface ICreateToursUi {

        /**
         * Lets the user choose one of the given tour structures.
         * The given list contains pairs with a description of the merge algorithm and the resulting tours.
         * There always is at least one choice.
         */
        public abstract List<? extends Tour> selectInitialTours(
                List<? extends Pair<String, List<? extends Tour>>> choices);

        /**
         * Lets the user choose any subset (including the empty set) of the given sets of
         * changes that shall be considered irrelevant for review. The given list contains
         * pairs with a description of the filter strategy and the resulting filter candidates.
         */
        public abstract List<? extends Pair<String, Set<? extends Change>>> selectIrrelevant(
                List<? extends Pair<String, Set<? extends Change>>> strategyResuls);

    }

    private final List<Tour> tours;
    private int currentTourIndex;
    private final WeakListeners<IToursInReviewChangeListener> listeners = new WeakListeners<>();

    private ToursInReview(List<? extends Tour> tours) {
        this.tours = new ArrayList<>(tours);
        this.currentTourIndex = 0;
    }

    /**
     * Creates a new object with the given tours (mainly for tests).
     */
    public static ToursInReview create(List<Tour> tours) {
        return new ToursInReview(tours);
    }

    /**
     * Loads the tours for the given ticket and creates a corresponding {@link ToursInReview}
     * object with initial settings. When there is user interaction and the user cancels,
     * null is returned.
     */
    public static ToursInReview create(
            IChangeSource src,
            IChangeSourceUi changeSourceUi,
            List<? extends IIrrelevanceDetermination> irrelevanceDeterminationStrategies,
            List<? extends ITourRestructuring> tourRestructuringStrategies,
            ICreateToursUi createUi,
            String ticketKey) {
        final List<Commit> changes = src.getChanges(ticketKey, changeSourceUi);
        final List<Commit> filteredChanges = filterChanges(irrelevanceDeterminationStrategies, changes, createUi);
        if (filteredChanges == null) {
            return null;
        }

        final List<Tour> tours = toTours(filteredChanges, src.createTracer());
        final List<? extends Tour> userSelection =
                determinePossibleRestructurings(tourRestructuringStrategies, tours, createUi);
        if (userSelection == null) {
            return null;
        }

        return new ToursInReview(userSelection);
    }

    private static List<Commit> filterChanges(
            List<? extends IIrrelevanceDetermination> irrelevanceDeterminationStrategies,
            List<? extends Commit> changes,
            ICreateToursUi createUi) {

        Telemetry.event("originalChanges")
            .param("count", countChanges(changes, false))
            .param("relevant", countChanges(changes, true))
            .log();

        final List<Pair<String, Set<? extends Change>>> strategyResuls = new ArrayList<>();
        for (final IIrrelevanceDetermination strategy : irrelevanceDeterminationStrategies) {
            try {
                final Set<? extends Change> irrelevantChanges = determineIrrelevantChanges(changes, strategy);
                Telemetry.event("relevanceFilterResult")
                    .param("description", strategy.getDescription())
                    .param("size", irrelevantChanges.size())
                    .log();

                if (areAllIrrelevant(irrelevantChanges)) {
                    //skip strategies that won't result in further changes to irrelevant
                    continue;
                }
                strategyResuls.add(Pair.<String, Set<? extends Change>>create(
                        strategy.getDescription(),
                        irrelevantChanges));
            } catch (final Exception e) {
                //skip instable strategies
                Logger.error("exception in filtering", e);
            }
        }

        final List<? extends Pair<String, Set<? extends Change>>> selected = createUi.selectIrrelevant(strategyResuls);
        final Set<Change> toMakeIrrelevant = new HashSet<>();
        final Set<String> selectedDescriptions = new LinkedHashSet<>();
        for (final Pair<String, Set<? extends Change>> set : selected) {
            toMakeIrrelevant.addAll(set.getSecond());
            selectedDescriptions.add(set.getFirst());
        }
        Telemetry.event("selectedRelevanceFilter")
            .param("descriptions", selectedDescriptions)
            .log();

        final List<Commit> ret = new ArrayList<>();
        for (final Commit c : changes) {
            ret.add(c.makeChangesIrrelevant(toMakeIrrelevant));
        }
        return ret;
    }

    private static int countChanges(List<? extends Commit> changes, boolean onlyRelevant) {
        int ret = 0;
        for (final Commit commit : changes) {
            for (final Change change : commit.getChanges()) {
                if (!(onlyRelevant && change.isIrrelevantForReview())) {
                    ret++;
                }
            }
        }
        return ret;
    }

    private static Set<? extends Change> determineIrrelevantChanges(
            List<? extends Commit> changes,
            IIrrelevanceDetermination strategy) {

        final Set<Change> ret = new HashSet<>();
        for (final Commit commit : changes) {
            for (final Change change : commit.getChanges()) {
                if (strategy.isIrrelevant(change)) {
                    ret.add(change);
                }
            }
        }
        return ret;
    }

    private static boolean areAllIrrelevant(Set<? extends Change> changes) {
        for (final Change change : changes) {
            if (!change.isIrrelevantForReview()) {
                return false;
            }
        }
        return true;
    }

    private static List<? extends Tour> determinePossibleRestructurings(
            List<? extends ITourRestructuring> tourRestructuringStrategies,
            List<Tour> originalTours,
            ICreateToursUi createUi) {

        final List<Pair<String, List<? extends Tour>>> possibleRestructurings = new ArrayList<>();

        possibleRestructurings.add(Pair.<String, List<? extends Tour>>create("one tour per commit", originalTours));
        Telemetry.event("originalTourStructure")
                .params(Tour.determineSize(originalTours))
                .log();


        for (final ITourRestructuring restructuringStrategy : tourRestructuringStrategies) {
            try {
                final List<? extends Tour> restructuredTour =
                        restructuringStrategy.restructure(new ArrayList<>(originalTours));

                Telemetry.event("possibleTourStructure")
                    .param("strategy", restructuringStrategy.getClass())
                    .params(Tour.determineSize(restructuredTour))
                    .log();

                if (restructuredTour != null) {
                    possibleRestructurings.add(Pair.<String, List<? extends Tour>>create(
                            restructuringStrategy.getDescription(), restructuredTour));
                }
            } catch (final Exception e) {
                //skip instable restructurings
                Logger.error("exception in restructuring", e);
            }
        }

        return createUi.selectInitialTours(possibleRestructurings);
    }

    private static List<Tour> toTours(List<Commit> changes, IFragmentTracer tracer) {
        final List<Tour> ret = new ArrayList<>();
        for (final Commit c : changes) {
            ret.add(new Tour(
                    c.getMessage(),
                    toSliceFragments(c.getChanges(), tracer)));
        }
        return ret;
    }

    private static List<Stop> toSliceFragments(List<Change> changes, IFragmentTracer tracer) {
        final List<Stop> ret = new ArrayList<>();
        for (final Change c : changes) {
            ret.add(toSliceFragment(c, tracer));
        }
        return ret;
    }

    private static Stop toSliceFragment(Change c, final IFragmentTracer tracer) {
        final ValueWrapper<Stop> ret = new ValueWrapper<>();
        c.accept(new ChangeVisitor() {

            @Override
            public void handle(TextualChangeHunk visitee) {
                ret.setValue(new Stop(
                        visitee.getFrom(),
                        visitee.getTo(),
                        tracer.traceFragment(visitee.getTo()),
                        visitee.isIrrelevantForReview()));
            }

            @Override
            public void handle(BinaryChange visitee) {
                ret.setValue(new Stop(
                        visitee.getFrom(),
                        visitee.getTo(),
                        tracer.traceFile(visitee.getTo()),
                        visitee.isIrrelevantForReview()));
            }

        });
        return ret.get();
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
            Telemetry.event("tourActivated")
                .param("tourIndex", index)
                .log();
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

        Telemetry.event("toursMerged")
                .param("mergedTourIndices", tourIndices)
                .params(Tour.determineSize(this.tours))
                .log();
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

}
