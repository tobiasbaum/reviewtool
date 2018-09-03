package de.setsoftware.reviewtool.model.changestructure;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;

import de.setsoftware.reviewtool.base.Logger;
import de.setsoftware.reviewtool.base.Multiset;
import de.setsoftware.reviewtool.base.Pair;
import de.setsoftware.reviewtool.base.ReviewtoolException;
import de.setsoftware.reviewtool.base.WeakListeners;
import de.setsoftware.reviewtool.model.Constants;
import de.setsoftware.reviewtool.model.api.IBinaryChange;
import de.setsoftware.reviewtool.model.api.IChange;
import de.setsoftware.reviewtool.model.api.IChangeData;
import de.setsoftware.reviewtool.model.api.IChangeSourceUi;
import de.setsoftware.reviewtool.model.api.IChangeVisitor;
import de.setsoftware.reviewtool.model.api.IClassification;
import de.setsoftware.reviewtool.model.api.ICommit;
import de.setsoftware.reviewtool.model.api.IFragment;
import de.setsoftware.reviewtool.model.api.IFragmentTracer;
import de.setsoftware.reviewtool.model.api.IRevisionedFile;
import de.setsoftware.reviewtool.model.api.ITextualChange;
import de.setsoftware.reviewtool.model.api.IWorkingCopy;
import de.setsoftware.reviewtool.ordering.efficientalgorithm.TourCalculatorControl;
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
         * changes together with the tours as a whole. Both arguments can be null, meaning that
         * there is no respective tour.
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
         * Lets the user choose which subset of commits to review, which filters
         * to apply and which of the commits to merge into a tour.
         * When the user cancels, null is returned.
         *
         * @param changes All commits belonging to the review.
         * @param strategyResults The classifications assigned to the changes, with the number of changes classified
         * @param reviewRounds The review rounds conducted so far (to show them to the user).
         */
        public abstract UserSelectedReductions selectIrrelevant(
                List<? extends ICommit> changes,
                Multiset<IClassification> strategyResults,
                List<ReviewRoundInfo> reviewRounds);

    }

    /**
     * Transfer object for the results of the user interaction to select
     * subset of commits, filters, ...
     */
    public static final class UserSelectedReductions {
        private final List<? extends ICommit> commitSubset;
        private final Set<? extends IClassification> toMakeIrrelevant;

        public UserSelectedReductions(
                final List<? extends ICommit> chosenCommitSubset,
                final Set<? extends IClassification> chosenFilterSubset) {
            this.commitSubset = chosenCommitSubset;
            this.toMakeIrrelevant = chosenFilterSubset;
        }
    }

    /**
     * Infos on a review round.
     */
    public static final class ReviewRoundInfo implements Comparable<ReviewRoundInfo> {
        private final int number;
        private final Date date;
        private final String user;

        public ReviewRoundInfo(final int number, final Date date, final String user) {
            this.number = number;
            this.date = date;
            this.user = user;
        }

        @Override
        public int compareTo(final ReviewRoundInfo o) {
            return Integer.compare(this.number, o.number);
        }

        @Override
        public boolean equals(final Object o) {
            if (!(o instanceof ReviewRoundInfo)) {
                return false;
            }
            return this.compareTo((ReviewRoundInfo) o) == 0;
        }

        @Override
        public int hashCode() {
            return this.number;
        }

        public Date getTime() {
            return this.date;
        }

        public String getReviewer() {
            return this.user;
        }

        public int getNumber() {
            return this.number;
        }
    }

    private final ChangeManager changeManager;
    private final List<Tour> topmostTours;
    private int currentTourIndex;
    private final WeakListeners<IToursInReviewChangeListener> listeners = new WeakListeners<>();
    private final IChangeManagerListener mostRecentFragmentUpdater;

    private ToursInReview(final ChangeManager changeManager, final List<? extends Tour> topmostTours) {
        this.changeManager = changeManager;
        this.topmostTours = new ArrayList<>(topmostTours);
        this.currentTourIndex = 0;
        this.mostRecentFragmentUpdater = new IChangeManagerListener() {
            @Override
            public void localChangeInfoUpdated(final ChangeManager changeManager) {
                ToursInReview.this.updateMostRecentFragmentsWithLocalChanges();
            }
        };
        this.changeManager.addListener(this.mostRecentFragmentUpdater);
        this.updateMostRecentFragmentsWithLocalChanges();
    }

    private ToursInReview(final List<? extends Tour> topmostTours) {
        this.changeManager = new ChangeManager(false);
        this.topmostTours = new ArrayList<>(topmostTours);
        this.currentTourIndex = 0;
        this.mostRecentFragmentUpdater = null;
    }

    /**
     * Creates a new object with the given tours (mainly for tests).
     */
    public static ToursInReview create(final List<Tour> tours) {
        return new ToursInReview(tours);
    }

    /**
     * Loads the tours for the given ticket and creates a corresponding {@link ToursInReview}
     * object with initial settings. When there is user interaction and the user cancels,
     * null is returned.
     */
    public static ToursInReview create(
            final ChangeManager changeManager,
            final IChangeSourceUi changeSourceUi,
            final List<? extends IChangeClassifier> changeClassificationStrategies,
            final List<? extends ITourRestructuring> tourRestructuringStrategies,
            final IStopOrdering orderingAlgorithm,
            final ICreateToursUi createUi,
            final IChangeData changes,
            final List<ReviewRoundInfo> reviewRounds) {
        changeSourceUi.subTask("Filtering changes...");
        final List<? extends ICommit> filteredChanges =
                filterChanges(changeClassificationStrategies, changes.getMatchedCommits(),
                        createUi, changeSourceUi, reviewRounds);
        if (filteredChanges == null) {
            return null;
        }

        changeSourceUi.subTask("Creating tours from changes...");
        final List<Tour> tours = toTours(
                filteredChanges,
                new FragmentTracer(),
                changeSourceUi);

        final List<? extends Tour> userSelection =
                determinePossibleRestructurings(tourRestructuringStrategies, tours, createUi, changeSourceUi);
        if (userSelection == null) {
            return null;
        }

        changeSourceUi.subTask("Ordering stops...");
        final List<? extends Tour> toursToShow = groupAndSort(
                userSelection,
                orderingAlgorithm,
                new TourCalculatorControl() {
                    private static final long FAST_MODE_THRESHOLD = 20000;
                    private final long startTime = System.currentTimeMillis();
                    @Override
                    public synchronized boolean isCanceled() {
                        return changeSourceUi.isCanceled();
                    }

                    @Override
                    public boolean isFastModeNeeded() {
                        return System.currentTimeMillis() - this.startTime > FAST_MODE_THRESHOLD;
                    }
                });

        return new ToursInReview(changeManager, toursToShow);
    }

    private static List<? extends Tour> groupAndSort(
            final List<? extends Tour> userSelection,
            final IStopOrdering orderingAlgorithm,
            final TourCalculatorControl isCanceled) {

        try {
            final List<Tour> ret = new ArrayList<>();
            for (final Tour t : userSelection) {
                ret.add(new Tour(t.getDescription(), orderingAlgorithm.groupAndSort(t.getStops(), isCanceled)));
            }
            return ret;
        } catch (final InterruptedException e) {
            throw new OperationCanceledException();
        }
    }

    private synchronized void updateMostRecentFragmentsWithLocalChanges() {
        final IFragmentTracer tracer = new FragmentTracer();
        for (final Tour tour : this.topmostTours) {
            for (final Stop stop : tour.getStops()) {
                stop.updateMostRecentData(tracer);
            }
        }
    }

    private static List<? extends ICommit> filterChanges(
            final List<? extends IChangeClassifier> changeClassificationStrategies,
            final List<? extends ICommit> changes,
            final ICreateToursUi createUi,
            final IProgressMonitor progressMonitor,
            final List<ReviewRoundInfo> reviewRounds) {

        Telemetry.event("originalChanges")
            .param("count", countChanges(changes))
            .log();

        final List<ICommit> changesWithClassifications = new ArrayList<>();
        for (final ICommit commit : changes) {
            changesWithClassifications.add(commit.transformChanges(
                    (IChange c) -> addClassifications(c, changeClassificationStrategies, progressMonitor)));
        }

        final Multiset<IClassification> strategyResults = new Multiset<>();
        for (final ICommit commit : changesWithClassifications) {
            for (final IChange change : commit.getChanges()) {
                for (final IClassification classification : change.getClassification()) {
                    strategyResults.add(classification);
                }
            }
        }

        for (final IClassification classification : strategyResults.keySet()) {
            Telemetry.event("classificationResult")
                .param("description", classification.getName())
                .param("size", strategyResults.get(classification))
                .log();
        }

        final UserSelectedReductions selected =
                createUi.selectIrrelevant(changesWithClassifications, strategyResults, reviewRounds);
        if (selected == null) {
            return null;
        }
        Telemetry.event("selectedRelevanceFilter")
            .param("descriptions", selected.toMakeIrrelevant)
            .log();
        final Set<String> selectedCommits = new LinkedHashSet<>();
        for (final ICommit commit : selected.commitSubset) {
            selectedCommits.add(commit.getRevision().toString());
        }
        Telemetry.event("selectedCommitSubset")
            .param("commits", selectedCommits)
            .log();

        final List<ICommit> ret = new ArrayList<>();
        for (final ICommit c : selected.commitSubset) {
            ret.add(c);
        }

        CommitsInReview.setCommits(ret);

        return ret;
    }

    private static IChange addClassifications(
            IChange change,
            List<? extends IChangeClassifier> changeClassificationStrategies,
            final IProgressMonitor progressMonitor) {
        IChange ret = change;
        for (final IChangeClassifier strategy : changeClassificationStrategies) {
            if (progressMonitor.isCanceled()) {
                throw new OperationCanceledException();
            }
            try {
                final IClassification cl = strategy.classify(ret);
                if (cl != null) {
                    ret = ret.addClassification(cl);
                }
            } catch (final Exception e) {
                Logger.error("exception in classification", e);
            }
        }
        return ret;
    }

    private static int countChanges(final List<? extends ICommit> changes) {
        int ret = 0;
        for (final ICommit commit : changes) {
            ret += commit.getChanges().size();
        }
        return ret;
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

    private static List<Tour> toTours(final List<? extends ICommit> changes, final IFragmentTracer tracer,
            final IProgressMonitor progressMonitor) {
        final List<Tour> ret = new ArrayList<>();
        for (final ICommit c : changes) {
            if (progressMonitor.isCanceled()) {
                throw new OperationCanceledException();
            }
            ret.add(new Tour(
                    c.getMessage(),
                    toSliceFragments(c.getChanges(), tracer)));
        }
        return ret;
    }

    private static List<Stop> toSliceFragments(final List<? extends IChange> changes, final IFragmentTracer tracer) {
        final List<Stop> ret = new ArrayList<>();
        for (final IChange c : changes) {
            ret.addAll(toSliceFragment(c, tracer));
        }
        return ret;
    }

    private static List<Stop> toSliceFragment(final IChange c, final IFragmentTracer tracer) {
        final List<Stop> ret = new ArrayList<>();
        c.accept(new IChangeVisitor() {

            @Override
            public void handle(final ITextualChange visitee) {
                final IWorkingCopy wc = c.getWorkingCopy();
                final List<? extends IFragment> mostRecentFragments =
                        tracer.traceFragment(wc.getRepository().getFileHistoryGraph(), visitee.getToFragment(), false);
                for (final IFragment fragment : mostRecentFragments) {
                    if (wc.toAbsolutePathInWc(fragment.getFile().getPath()) != null) {
                        ret.add(new Stop(visitee, fragment));
                    }
                }
            }

            @Override
            public void handle(final IBinaryChange visitee) {
                final IWorkingCopy wc = c.getWorkingCopy();
                for (final IRevisionedFile fileInRevision :
                        tracer.traceFile(wc.getRepository().getFileHistoryGraph(), visitee.getFrom(), false)) {
                    if (wc.toAbsolutePathInWc(fileInRevision.getPath()) != null) {
                        ret.add(new Stop(visitee, fileInRevision));
                    }
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
        for (int i = 0; i < this.topmostTours.size(); i++) {
            final Tour s = this.topmostTours.get(i);
            for (final Stop f : s.getStops()) {
                if (progressMonitor.isCanceled()) {
                    throw new OperationCanceledException();
                }
                this.createMarkerFor(markerFactory, lookupTables, f, i == this.currentTourIndex);
            }
        }
    }

    private IMarker createMarkerFor(
            final IStopMarkerFactory markerFactory,
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
                final IFragment pos = f.getMostRecentFragment();
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
            final IStopMarkerFactory markerFactory,
            final Stop f) {
        return this.createMarkerFor(markerFactory, new HashMap<IResource, PositionLookupTable>(), f, true);
    }

    public List<Tour> getTopmostTours() {
        return this.topmostTours;
    }

    /**
     * Sets the given tour as the active tour, if it is not already active.
     * Recreates markers accordingly.
     */
    public void ensureTourActive(final Tour t, final IStopMarkerFactory markerFactory) {
        this.ensureTourActive(t, markerFactory, true);
    }

    /**
     * Sets the given tour as the active tour, if it is not already active.
     * Recreates markers accordingly.
     */
    public void ensureTourActive(final Tour t, final IStopMarkerFactory markerFactory, final boolean notify) {

        final int index = this.topmostTours.indexOf(t);
        if (index != this.currentTourIndex) {
            final Tour oldActive = this.getActiveTour();
            this.currentTourIndex = index;
            new WorkspaceJob("Review marker update") {
                @Override
                public IStatus runInWorkspace(final IProgressMonitor progressMonitor) throws CoreException {
                    ToursInReview.this.clearMarkers();
                    ToursInReview.this.createMarkers(markerFactory, progressMonitor);
                    return Status.OK_STATUS;
                }
            }.schedule();
            if (notify) {
                this.listeners.notifyListeners(l -> l.activeTourChanged(oldActive, this.getActiveTour()));
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
        return this.currentTourIndex >= this.topmostTours.size() || this.currentTourIndex < 0
                ? null : this.topmostTours.get(this.currentTourIndex);
    }

    public void registerListener(final IToursInReviewChangeListener listener) {
        this.listeners.add(listener);
    }

    /**
     * Returns all stops (from all tours) that refer to the given file.
     */
    public List<Stop> getStopsFor(final File absolutePath) {
        final List<Stop> ret = new ArrayList<>();
        for (final Tour t : this.topmostTours) {
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
    public int findTourIndexWithStop(final Stop currentStop) {
        for (int i = 0; i < this.topmostTours.size(); i++) {
            for (final Stop s : this.topmostTours.get(i).getStops()) {
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
    public Pair<Tour, Stop> findNearestStop(final IPath absoluteResourcePath, final int line) {
        if (this.topmostTours.isEmpty()) {
            return null;
        }
        Tour bestTour = null;
        Stop bestStop = null;
        int bestDist = Integer.MAX_VALUE;
        for (final Tour t : this.topmostTours) {
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

    private int calculateDistance(final Stop stop, final IPath resource, final int line) {
        if (!stop.getMostRecentFile().toLocalPath(stop.getWorkingCopy()).equals(resource)) {
            return Integer.MAX_VALUE;
        }

        final IFragment fragment = stop.getMostRecentFragment();
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

    /**
     * Determines the direct parent tour of the given element.
     * Returns null when none is found.
     */
    public Tour getParentFor(final TourElement element) {
        for (final Tour t : this.topmostTours) {
            final Tour parent = t.findParentFor(element);
            if (parent != null) {
                return parent;
            }
        }
        return null;
    }

    /**
     * Determines the topmost parent tour of the given element.
     * Returns null when none is found.
     */
    public Tour getTopmostTourWith(final TourElement element) {
        for (final Tour t : this.topmostTours) {
            final Tour parent = t.findParentFor(element);
            if (parent != null) {
                return t;
            }
        }
        return null;
    }
}
