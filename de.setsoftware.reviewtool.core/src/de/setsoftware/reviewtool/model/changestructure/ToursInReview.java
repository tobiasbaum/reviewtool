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
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.statushandlers.StatusManager;

import de.setsoftware.reviewtool.base.Logger;
import de.setsoftware.reviewtool.base.Pair;
import de.setsoftware.reviewtool.base.ReviewtoolException;
import de.setsoftware.reviewtool.base.WeakListeners;
import de.setsoftware.reviewtool.model.Constants;
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
         * When the user cancels, null is returned.
         */
        public abstract List<? extends Tour> selectInitialTours(
                List<? extends Pair<String, List<? extends Tour>>> choices);

        /**
         * Lets the user choose any subset (including the empty set) of the given sets of
         * changes that shall be considered irrelevant for review. The given list contains
         * pairs with a description of the filter strategy and the resulting filter candidates.
         * When the user cancels, null is returned.
         */
        public abstract List<? extends Pair<String, Set<? extends Change>>> selectIrrelevant(
                List<? extends Pair<String, Set<? extends Change>>> strategyResuls);

    }

    private final VirtualFileHistoryGraph historyGraph;
    private final List<Tour> tours;
    private final IChangeData remoteChanges;
    private List<File> modifiedFiles;
    private int currentTourIndex;
    private final WeakListeners<IToursInReviewChangeListener> listeners = new WeakListeners<>();

    private ToursInReview(final List<? extends Tour> tours, final IChangeData remoteChanges) {
        this.historyGraph = new VirtualFileHistoryGraph(remoteChanges.getHistoryGraph());
        this.tours = new ArrayList<>(tours);
        this.remoteChanges = remoteChanges;
        this.modifiedFiles = remoteChanges.getLocalPaths();
        this.currentTourIndex = 0;
    }

    private ToursInReview(final List<? extends Tour> tours) {
        this.historyGraph = new VirtualFileHistoryGraph();
        this.tours = new ArrayList<>(tours);
        this.remoteChanges = null;
        this.modifiedFiles = new ArrayList<>();
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
        changeSourceUi.subTask("Determining relevant changes...");
        final IChangeData changes = src.getRepositoryChanges(ticketKey, changeSourceUi);
        changeSourceUi.subTask("Filtering changes...");
        final List<Commit> filteredChanges =
                filterChanges(irrelevanceDeterminationStrategies, changes.getMatchedCommits(),
                        createUi, changeSourceUi);
        if (filteredChanges == null) {
            return null;
        }

        changeSourceUi.subTask("Creating tours from changes...");
        final List<Tour> tours = toTours(
                filteredChanges,
                false,
                new FragmentTracer(changes.getHistoryGraph()),
                changeSourceUi);

        final List<? extends Tour> userSelection =
                determinePossibleRestructurings(tourRestructuringStrategies, tours, createUi, changeSourceUi);
        if (userSelection == null) {
            return null;
        }

        final ToursInReview result = new ToursInReview(userSelection, changes);
        result.createLocalTour(null, changeSourceUi, null);
        return result;
    }

    /**
     * (Re)creates the local tour by (re)collecting local changes and combining them with the repository changes
     * in a {@link VirtualFileHistoryGraph}.
     *
     * @param progressMonitor The progress monitor to use.
     * @param markerFactory The marker factory to use. May be null if initially called while creating the tours.
     */
    public void createLocalTour(
            final List<File> paths,
            final IProgressMonitor progressMonitor,
            final IStopMarkerFactory markerFactory) {

        progressMonitor.subTask("Collecting local changes...");
        final IChangeData localChanges;
        try {
            if (paths == null) {
                localChanges = this.remoteChanges.getSource().getLocalChanges(this.remoteChanges, null,
                        progressMonitor);
            } else {
                this.modifiedFiles.addAll(paths);
                localChanges = this.remoteChanges.getSource().getLocalChanges(this.remoteChanges, this.modifiedFiles,
                        progressMonitor);
            }
        } catch (final ReviewtoolException e) {
            //if there is a problem while determining the local changes, ignore them
            Logger.warn("problem while determining local changes", e);
            return;
        }
        this.modifiedFiles = new ArrayList<>(localChanges.getLocalPaths());

        if (this.historyGraph.size() > 1) {
            this.historyGraph.remove(1);
        }
        this.historyGraph.add(localChanges.getHistoryGraph());

        final List<Tour> tours = toTours(
                localChanges.getMatchedCommits(),
                true,
                new FragmentTracer(localChanges.getHistoryGraph()),
                progressMonitor);

        boolean listenersCalled = false;
        if (tours.isEmpty()) {
            listenersCalled = this.removeLocalTour(markerFactory);
        } else {
            this.addOrReplaceLocalTour(tours.get(0));
        }
        this.updateMostRecentFragmentsOfNonLocalTours();

        if (!listenersCalled) {
            this.notifyListenersAboutTourStructureChange(markerFactory);
        }
    }

    private void updateMostRecentFragmentsOfNonLocalTours() {
        final IFragmentTracer tracer = new FragmentTracer(this.historyGraph);
        for (final Tour tour : this.tours) {
            if (tour.isLocal()) {
                continue;
            }
            for (final Stop stop : tour.getStops()) {
                stop.updateMostRecentData(tracer);
            }
        }
    }

    private static List<Commit> filterChanges(
            final List<? extends IIrrelevanceDetermination> irrelevanceDeterminationStrategies,
            final List<? extends Commit> changes,
            final ICreateToursUi createUi,
            final IProgressMonitor progressMonitor) {

        Telemetry.event("originalChanges")
            .param("count", countChanges(changes, false))
            .param("relevant", countChanges(changes, true))
            .log();

        final List<Pair<String, Set<? extends Change>>> strategyResuls = new ArrayList<>();
        for (final IIrrelevanceDetermination strategy : irrelevanceDeterminationStrategies) {
            try {
                if (progressMonitor.isCanceled()) {
                    throw new OperationCanceledException();
                }

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
        if (selected == null) {
            return null;
        }
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
            final List<? extends ITourRestructuring> tourRestructuringStrategies,
            final List<Tour> originalTours,
            final ICreateToursUi createUi,
            final IProgressMonitor progressMonitor) {

        final List<Pair<String, List<? extends Tour>>> possibleRestructurings = new ArrayList<>();

        possibleRestructurings.add(Pair.<String, List<? extends Tour>>create("one tour per commit", originalTours));
        Telemetry.event("originalTourStructure")
                .params(Tour.determineSize(originalTours))
                .log();


        for (final ITourRestructuring restructuringStrategy : tourRestructuringStrategies) {
            if (progressMonitor.isCanceled()) {
                throw new OperationCanceledException();
            }

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

    private static List<Tour> toTours(final List<Commit> changes, final boolean isLocal, final IFragmentTracer tracer,
            final IProgressMonitor progressMonitor) {
        final List<Tour> ret = new ArrayList<>();
        for (final Commit c : changes) {
            if (progressMonitor.isCanceled()) {
                throw new OperationCanceledException();
            }
            ret.add(new Tour(
                    c.getMessage(),
                    toSliceFragments(c.getChanges(), tracer),
                    c.isVisible(),
                    isLocal));
        }
        return ret;
    }

    private static List<Stop> toSliceFragments(List<Change> changes, IFragmentTracer tracer) {
        final List<Stop> ret = new ArrayList<>();
        for (final Change c : changes) {
            ret.addAll(toSliceFragment(c, tracer));
        }
        return ret;
    }

    private static List<Stop> toSliceFragment(Change c, final IFragmentTracer tracer) {
        final List<Stop> ret = new ArrayList<>();
        c.accept(new ChangeVisitor() {

            @Override
            public void handle(TextualChangeHunk visitee) {
                final List<Fragment> mostRecentFragments = tracer.traceFragment(visitee.getToFragment());
                for (final Fragment fragment : mostRecentFragments) {
                    ret.add(new Stop(visitee, fragment));
                }
            }

            @Override
            public void handle(BinaryChange visitee) {
                for (final FileInRevision fileInRevision : tracer.traceFile(visitee.getFrom())) {
                    ret.add(new Stop(visitee, fileInRevision));
                }
            }

        });
        return ret;
    }

    /**
     * Creates markers for the tour stops.
     */
    public void createMarkers(final IStopMarkerFactory markerFactory, final IProgressMonitor progressMonitor) {
        final Map<IResource, PositionLookupTable> lookupTables = new HashMap<>();
        for (int i = 0; i < this.tours.size(); i++) {
            final Tour s = this.tours.get(i);
            for (final Stop f : s.getStops()) {
                if (progressMonitor != null && progressMonitor.isCanceled()) {
                    throw new OperationCanceledException();
                }
                this.createMarkerFor(markerFactory, lookupTables, f, i == this.currentTourIndex);
            }
        }
    }

    private IMarker createMarkerFor(
            IStopMarkerFactory markerFactory,
            final Map<IResource, PositionLookupTable> lookupTables,
            final Stop f,
            final boolean tourActive) {

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
                final IMarker marker = markerFactory.createStopMarker(resource, tourActive);
                marker.setAttribute(IMarker.LINE_NUMBER, pos.getFrom().getLine());
                marker.setAttribute(IMarker.CHAR_START,
                        lookupTables.get(resource).getCharsSinceFileStart(pos.getFrom()));
                marker.setAttribute(IMarker.CHAR_END,
                        lookupTables.get(resource).getCharsSinceFileStart(pos.getTo()));
                return marker;
            } else {
                return markerFactory.createStopMarker(resource, tourActive);
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
    public IMarker createMarkerFor(
            IStopMarkerFactory markerFactory,
            final Stop f) {
        return this.createMarkerFor(markerFactory, new HashMap<IResource, PositionLookupTable>(), f, true);
    }

    /**
     * Returns a {@link IFileHistoryNode} for passed file.
     * @param file The file whose change history to retrieve.
     * @return The {@link IFileHistoryNode} describing changes for passed {@link FileInRevision} or null if not found.
     */
    public IFileHistoryNode getFileHistoryNode(final FileInRevision file) {
        return this.historyGraph.getNodeFor(file);
    }

    public List<Tour> getTours() {
        return this.tours;
    }

    /**
     * Sets the given tour as the active tour, if it is not already active.
     * Recreates markers accordingly.
     */
    public void ensureTourActive(Tour t, IStopMarkerFactory markerFactory) throws CoreException {
        this.ensureTourActive(t, markerFactory, true);
    }

    /**
     * Sets the given tour as the active tour, if it is not already active.
     * Recreates markers accordingly.
     */
    public void ensureTourActive(Tour t, IStopMarkerFactory markerFactory, boolean notify)
        throws CoreException {

        final int index = this.tours.indexOf(t);
        if (index != this.currentTourIndex) {
            this.clearMarkers();
            final Tour oldActive = this.getActiveTour();
            this.currentTourIndex = index;
            this.createMarkers(markerFactory, null);
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
        ResourcesPlugin.getWorkspace().getRoot().deleteMarkers(
                Constants.INACTIVESTOPMARKER_ID, true, IResource.DEPTH_INFINITE);
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
    public void mergeTours(List<Tour> toursToMerge, IStopMarkerFactory markerFactory) throws CoreException {
        final List<Tour> mergeableToursToMerge = new ArrayList<>();
        for (final Tour t : toursToMerge) {
            if (!t.isLocal()) {
                mergeableToursToMerge.add(t);
            }
        }

        if (mergeableToursToMerge.size() <= 1) {
            return;
        }

        //determine the indices of the tours; they are needed for telemetry logging later
        final List<Integer> tourIndices = new ArrayList<>();
        for (final Tour t : mergeableToursToMerge) {
            tourIndices.add(this.tours.indexOf(t));
        }

        //determine the merged tour
        Tour mergeResult = mergeableToursToMerge.get(0);
        for (int i = 1; i < mergeableToursToMerge.size(); i++) {
            mergeResult = mergeResult.mergeWith(mergeableToursToMerge.get(i));
        }

        //save the currently active tour
        final Tour activeTour = this.getActiveTour();

        //replace the largest part with the merge result and remove the old tours
        final Tour largestTour = this.determineLargestTour(mergeableToursToMerge);
        this.tours.set(this.tours.indexOf(largestTour), mergeResult);
        this.tours.removeAll(mergeableToursToMerge);

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

    /**
     * Adds a new local tour or replaces the old one.
     * @param newLocalTour The new local tour.
     */
    private void addOrReplaceLocalTour(final Tour newLocalTour) {
        this.removeLocalTour();
        this.tours.add(newLocalTour);
    }

    /**
     * Removes the local tour. If this tour is currently active, another tour is selected if available.
     * @param markerFactory The marker factory to use. May be null if initially called while creating the tours.
     *
     * @return {@code true} if updating markers and calling listeners have been scheduled, else {@code false}.
     */
    private boolean removeLocalTour(final IStopMarkerFactory markerFactory) {
        final boolean wasLocalTourActive = this.removeLocalTour();
        if (wasLocalTourActive) {
            if (!this.tours.isEmpty()) {
                assert markerFactory != null;
                final Tour tourToSelect = this.tours.get(this.tours.size() - 1);
                this.changeTourAfterActiveTourRemoval(tourToSelect, markerFactory);
                return true;
            } else {
                this.currentTourIndex = 0;
            }
        }
        return false;
    }

    /**
     * Removes the local tour if available.
     * @return {@code true} if the local tour was active before removal, {@code false} if it was inactive
     *      or if no local tour was removed.
     */
    private boolean removeLocalTour() {
        if (!this.tours.isEmpty()) {
            final Tour lastTour = this.tours.get(this.tours.size() - 1);
            if (lastTour.isLocal()) {
                this.tours.remove(this.tours.size() - 1);
                return this.currentTourIndex == this.tours.size();
            }
        }
        return false;
    }

    private void notifyListenersAboutTourStructureChange(final IStopMarkerFactory markerFactory) {
        // markerFactors is null only if called from ToursInReview.create(), and in this case ensureTourActive()
        // is called later on which recreates the markers
        if (markerFactory != null) {
            new WorkspaceJob("Stop marker update") {
                @Override
                public IStatus runInWorkspace(IProgressMonitor progressMonitor) throws CoreException {
                    ToursInReview.this.clearMarkers();
                    ToursInReview.this.createMarkers(markerFactory, progressMonitor);
                    return Status.OK_STATUS;
                }
            }.schedule();
        }

        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                for (final IToursInReviewChangeListener l : ToursInReview.this.listeners) {
                    l.toursChanged();
                }
            }
        });
    }

    /**
     * Changes the active tour and notifies listeners about a change in the tour structure. This is necessary
     * when the local tour was active before being removed. We use {@link Display#asyncExec(Runnable)} because
     * we could be called in a context where resources are locked (i.e. from within a {@link IResourceChangeListener}).
     *
     * @param nextTour The new tour to be selected.
     * @param markerFactory The marker factory to use.
     */
    private void changeTourAfterActiveTourRemoval(final Tour nextTour, final IStopMarkerFactory markerFactory) {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                try {
                    ToursInReview.this.ensureTourActive(nextTour, markerFactory, false);
                    for (final IToursInReviewChangeListener l : ToursInReview.this.listeners) {
                        l.toursChanged();
                    }
                } catch (final CoreException e) {
                    StatusManager.getManager().handle(e, "CoRT");
                }
            }
        });
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

    /**
     * Determines a stop that is as close as possible to the given line in the given resource.
     * The closeness measure is tweaked to (hopefully) capture the users intention as good as possible
     * for cases where he did not click directly on a stop.
     */
    public Pair<Tour, Stop> findNearestStop(IPath absoluteResourcePath, int line) {
        if (this.tours.isEmpty()) {
            return null;
        }
        Tour bestTour = null;
        Stop bestStop = null;
        int bestDist = Integer.MAX_VALUE;
        for (final Tour t : this.tours) {
            for (final Stop stop : t.getStops()) {
                final int candidateDist = this.calculateDistance(stop, absoluteResourcePath, line);
                if (candidateDist < bestDist) {
                    bestTour = t;
                    bestStop = stop;
                    bestDist = candidateDist;
                }
            }
        }
        return Pair.create(bestTour, bestStop);
    }

    private int calculateDistance(Stop stop, IPath resource, int line) {
        if (!stop.getMostRecentFile().toLocalPath().equals(resource)) {
            return Integer.MAX_VALUE;
        }

        final Fragment fragment = stop.getMostRecentFragment();
        if (fragment == null) {
            return Integer.MAX_VALUE - 1;
        }

        if (line < fragment.getFrom().getLine()) {
            //there is a bias that lets lines between stops belong more closely to the stop above than below
            //  to a certain degree
            return (fragment.getFrom().getLine() - line) * 4;
        } else if (line > fragment.getTo().getLine()) {
            return line - fragment.getTo().getLine();
        } else {
            return 0;
        }
    }
}
