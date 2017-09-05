package de.setsoftware.reviewtool.ui.views;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.IShowInTarget;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.part.ShowInContext;
import org.eclipse.ui.part.ViewPart;

import de.setsoftware.reviewtool.base.Logger;
import de.setsoftware.reviewtool.base.Pair;
import de.setsoftware.reviewtool.base.ReviewtoolException;
import de.setsoftware.reviewtool.model.PositionTransformer;
import de.setsoftware.reviewtool.model.ReviewStateManager;
import de.setsoftware.reviewtool.model.api.IFragment;
import de.setsoftware.reviewtool.model.api.ITextualChange;
import de.setsoftware.reviewtool.model.changestructure.ChangestructureFactory;
import de.setsoftware.reviewtool.model.changestructure.PositionLookupTable;
import de.setsoftware.reviewtool.model.changestructure.Stop;
import de.setsoftware.reviewtool.model.changestructure.Tour;
import de.setsoftware.reviewtool.model.changestructure.TourElement;
import de.setsoftware.reviewtool.model.changestructure.ToursInReview;
import de.setsoftware.reviewtool.model.changestructure.ToursInReview.IToursInReviewChangeListener;
import de.setsoftware.reviewtool.model.remarks.GlobalPosition;
import de.setsoftware.reviewtool.model.remarks.Position;
import de.setsoftware.reviewtool.telemetry.Telemetry;
import de.setsoftware.reviewtool.ui.dialogs.DialogHelper;
import de.setsoftware.reviewtool.ui.dialogs.RealMarkerFactory;
import de.setsoftware.reviewtool.viewtracking.CodeViewTracker;
import de.setsoftware.reviewtool.viewtracking.ITrackerCreationListener;
import de.setsoftware.reviewtool.viewtracking.IViewStatisticsListener;
import de.setsoftware.reviewtool.viewtracking.TrackerManager;
import de.setsoftware.reviewtool.viewtracking.ViewStatDataForStop;
import de.setsoftware.reviewtool.viewtracking.ViewStatistics;

/**
 * A review to show the content (tours and stops) belonging to a review.
 */
public class ReviewContentView extends ViewPart implements ReviewModeListener, IShowInTarget {

    /**
     * Action that toggles a filter flag.
     */
    private abstract static class FilterStateAction extends Action {

        private final String id;
        private final ViewerFilter filter;
        private TreeViewer treeViewer;

        public FilterStateAction(String id, String text) {
            super(text, SWT.TOGGLE);
            this.id = id;
            this.filter = new ViewerFilter() {
                @Override
                public boolean select(Viewer viewer, Object parentElement, Object element) {
                    if (element instanceof Stop) {
                        return FilterStateAction.this.shallShow((Stop) element);
                    } else {
                        return true;
                    }
                }
            };
            this.setChecked(Boolean.parseBoolean(DialogHelper.getSetting(id)));
        }

        @Override
        public void run() {
            this.applyFilter();
            DialogHelper.saveSetting(this.id, Boolean.toString(this.isChecked()));
        }

        public void attach(TreeViewer tv) {
            this.treeViewer = tv;
            this.applyFilter();
        }

        private void applyFilter() {
            if (this.treeViewer == null) {
                return;
            }
            if (this.isChecked()) {
                if (!Arrays.asList(this.treeViewer.getFilters()).contains(this.filter)) {
                    this.treeViewer.addFilter(this.filter);
                }
            } else {
                this.treeViewer.removeFilter(this.filter);
            }
            Telemetry.event("applyContentTreeFilter")
                    .param("id", this.id)
                    .param("checked", this.isChecked())
                    .log();
        }

        protected abstract boolean shallShow(Stop s);
    }

    private Composite comp;
    private Composite currentContent;
    private FilterStateAction hideChecked;
    private FilterStateAction hideVisited;
    private FilterStateAction hideIrrelevant;
    private String ticketId;

    @Override
    public void createPartControl(Composite comp) {
        this.comp = comp;

        this.hideChecked = new FilterStateAction("hideChecked", "Hide stops that are marked as checked") {
            @Override
            protected boolean shallShow(Stop s) {
                final ViewStatistics statistics = TrackerManager.get().getStatistics();
                return statistics == null || !statistics.isMarkedAsChecked(s);
            }
        };
        this.hideVisited = new FilterStateAction("hideVisited", "Hide visited stops") {
            @Override
            protected boolean shallShow(Stop s) {
                final ViewStatDataForStop viewRatio = TrackerManager.get().determineViewRatio(s);
                return viewRatio.isPartlyUnvisited();
            }

        };
        this.hideIrrelevant = new FilterStateAction("hideIrrelevant", "Hide irrelevant stops") {
            @Override
            protected boolean shallShow(Stop s) {
                return !s.isIrrelevantForReview();
            }
        };

        this.getViewSite().getActionBars().getMenuManager().add(this.hideChecked);
        this.getViewSite().getActionBars().getMenuManager().add(this.hideVisited);
        this.getViewSite().getActionBars().getMenuManager().add(this.hideIrrelevant);

        ViewDataSource.get().registerListener(this);
    }

    @Override
    public void setFocus() {
        if (this.currentContent != null) {
            this.currentContent.setFocus();
        }
    }

    @Override
    public void notifyReview(ReviewStateManager mgr, ToursInReview tours) {
        this.disposeOldContent();
        this.currentContent = this.createReviewContent(tours);
        this.ticketId = mgr.getTicketKey();
        this.comp.layout();
    }

    private Composite createReviewContent(final ToursInReview tours) {
        final Composite panel = new Composite(this.comp, SWT.NULL);
        panel.setLayout(new FillLayout());

        final TreeViewer tv = new TreeViewer(panel);
        ColumnViewerToolTipSupport.enableFor(tv);
        tv.setUseHashlookup(true);
        tv.setContentProvider(new ViewContentProvider(tours));
        tv.setLabelProvider(new TourAndStopLabelProvider());
        tv.setInput(tours);

        final Tree tree = tv.getTree();
        tree.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                ReviewContentView.this.jumpToStopForItem(tours, (TreeItem) e.item);
            }
        });

        this.hideChecked.attach(tv);
        this.hideVisited.attach(tv);
        this.hideIrrelevant.attach(tv);

        ViewHelper.createContextMenu(this, tv.getControl(), tv);
        ensureActiveTourExpanded(tv, tours);

        return panel;
    }

    private void jumpToStopForItem(final ToursInReview tours, final TreeItem item) {
        if (item.getData() instanceof Stop) {
            final Stop stop = (Stop) item.getData();
            final Tour tour = (Tour) item.getParentItem().getData();
            jumpTo(tours, tour, stop, "tree");
        }
    }

    /**
     * Jumps to the given fragment. Ensures that the corresponding tour is active.
     */
    public static void jumpTo(ToursInReview tours, Tour tour, Stop stop, String typeForTelemetry) {
        CurrentStop.setCurrentStop(stop);
        try {
            tours.ensureTourActive(tour, new RealMarkerFactory());
            Telemetry.event("jumpedTo")
                .param("resource", stop.getMostRecentFile().getPath())
                .param("line", stop.getMostRecentFragment() == null
                    ? -1 : stop.getMostRecentFragment().getFrom().getLine())
                .param("type", typeForTelemetry)
                .log();
            openEditorFor(tours, stop, false);
        } catch (final CoreException | IOException e) {
            throw new ReviewtoolException(e);
        }
    }

    /**
     * Jumps to the given fragment and opens it in a text editor. Ensures that the corresponding tour is active.
     */
    public static void openInTextEditor(ToursInReview tours, Tour tour, Stop stop) {
        CurrentStop.setCurrentStop(stop);
        try {
            tours.ensureTourActive(tour, new RealMarkerFactory());
            Telemetry.event("openTextEditor")
                .param("resource", stop.getMostRecentFile().getPath())
                .param("line", stop.getMostRecentFragment() == null
                    ? -1 : stop.getMostRecentFragment().getFrom().getLine())
                .log();
            openEditorFor(tours, stop, true);
        } catch (final CoreException | IOException e) {
            throw new ReviewtoolException(e);
        }
    }

    private static void ensureActiveTourExpanded(TreeViewer tv, ToursInReview tours) {
        final Tour activeTour = tours.getActiveTour();
        tv.expandToLevel(activeTour, TreeViewer.ALL_LEVELS);
    }

    private static void openEditorFor(final ToursInReview tours, final Stop stop, boolean forceTextEditor)
            throws CoreException, IOException {

        final IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

        //when jumping to a marker, Eclipse selects all the contained text. We don't want that, so
        //  we create a copy of the fragment without size
        Stop jumpTarget;
        if (stop.isDetailedFragmentKnown()) {
            final IFragment fragment = ChangestructureFactory.createFragment(
                    stop.getMostRecentFile(),
                    stop.getMostRecentFragment().getFrom(),
                    stop.getMostRecentFragment().getFrom());
            final ITextualChange change = ChangestructureFactory.createTextualChangeHunk(
                    fragment,
                    fragment,
                    false,
                    true);
            jumpTarget = new Stop(change, fragment);
        } else {
            jumpTarget = stop;
        }

        final IMarker marker = tours.createMarkerFor(new RealMarkerFactory(), jumpTarget);
        if (marker != null) {
            if (forceTextEditor) {
                marker.setAttribute(IDE.EDITOR_ID_ATTR, getTextEditorId());
            }
            IDE.openEditor(page, marker);
            marker.delete();
        } else {
            final IFileStore fileStore =
                    EFS.getLocalFileSystem().getStore(stop.getMostRecentFile().toLocalPath());
            final IEditorPart part;
            if (forceTextEditor) {
                part = page.openEditor(new FileStoreEditorInput(fileStore), getTextEditorId());
            } else {
                part = IDE.openEditorOnFileStore(page, fileStore);
            }
            //for files not in the workspace, we cannot create markers, but let's at least select the text
            if (stop.isDetailedFragmentKnown() && fileStore.fetchInfo().exists()) {
                final PositionLookupTable lookup = PositionLookupTable.create(fileStore);
                final int posStart = lookup.getCharsSinceFileStart(stop.getMostRecentFragment().getFrom());
                final int posEnd = lookup.getCharsSinceFileStart(stop.getMostRecentFragment().getTo());
                setSelection(part, new TextSelection(posStart, posEnd - posStart));
            }
        }
    }

    private static String getTextEditorId() {
        return "org.eclipse.ui.DefaultTextEditor";
    }

    private static void setSelection(IEditorPart part, TextSelection textSelection) {
        if (part instanceof MultiPageEditorPart) {
            final MultiPageEditorPart multiPage = (MultiPageEditorPart) part;
            for (final IEditorPart subPart : multiPage.findEditors(multiPage.getEditorInput())) {
                setSelection(subPart, textSelection);
            }
        } else {
            final ISelectionProvider sp = part.getEditorSite().getSelectionProvider();
            if (sp == null) {
                Logger.debug("cannot select, selection provider is null");
                return;
            }
            sp.setSelection(textSelection);
        }
    }

    @Override
    public void notifyFixing(ReviewStateManager mgr) {
        this.disposeOldContent();
        this.currentContent = this.createNoReviewContent();
        this.comp.layout();
    }

    @Override
    public void notifyIdle() {
        this.disposeOldContent();
        this.currentContent = this.createNoReviewContent();
        this.comp.layout();
    }

    @Override
    public boolean show(ShowInContext context) {
        try {
            Pair<? extends Object, Integer> pos = ViewHelper.extractFileAndLineFromSelection(
                    context.getSelection(), context.getInput());

            //unfortunately it seems to be very hard to get the line for a structured selection
            //therefore try if a better selection is available if going to the active editor hard-wired
            if (pos == null || pos.getSecond().intValue() == 0) {
                final IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
                if (page != null) {
                    final IEditorPart activeEditor = page.getActiveEditor();
                    if (activeEditor != null) {
                        final ISelection sel2 = activeEditor.getEditorSite().getSelectionProvider().getSelection();
                        final Pair<? extends Object, Integer> pos2 =
                                ViewHelper.extractFileAndLineFromSelection(sel2, context.getInput());
                        if (pos2 != null) {
                            pos = pos2;
                        }
                    }
                }
            }

            if (pos == null) {
                return false;
            }

            final ToursInReview tours = ViewDataSource.get().getToursInReview();
            if (tours == null) {
                return false;
            }

            final Object pathOrResource = pos.getFirst();
            final IPath path = pathOrResource instanceof IPath
                    ? (IPath) pathOrResource : ((IResource) pathOrResource).getLocation();
            final Pair<Tour, Stop> nearestStop = tours.findNearestStop(path, pos.getSecond());
            if (nearestStop == null) {
                return false;
            }

            jumpTo(tours, nearestStop.getFirst(), nearestStop.getSecond(), "showIn");

            return true;
        } catch (final ExecutionException e) {
            throw new ReviewtoolException(e);
        }
    }

    private Composite createNoReviewContent() {
        final Composite panel = new Composite(this.comp, SWT.NULL);
        panel.setLayout(new FillLayout());
        final Label label = new Label(panel, SWT.NULL);
        if (this.ticketId == null) {
            label.setText("Not in review mode");
        } else {
            label.setText("Not in review mode\nLast finished: " + this.ticketId);
        }
        ViewHelper.createContextMenuWithoutSelectionProvider(this, label);
        return panel;
    }

    private void disposeOldContent() {
        if (this.currentContent != null) {
            this.currentContent.dispose();
        }
    }

    /**
     * Provides the tree consisting of tours and stops.
     */
    private class ViewContentProvider implements ITreeContentProvider, IToursInReviewChangeListener,
            ITrackerCreationListener, IViewStatisticsListener, StopSelectionListener {

        private final ToursInReview tours;
        private TreeViewer viewer;

        public ViewContentProvider(ToursInReview tours) {
            this.tours = tours;
            this.tours.registerListener(this);
            TrackerManager.get().registerListener(this);
            CurrentStop.registerListener(this);
        }

        @Override
        public Object[] getElements(Object inputElement) {
            assert inputElement == this.tours;
            return this.getChildren(null);
        }

        @Override
        public Object[] getChildren(Object parentElement) {
            if (parentElement == null) {
                return this.tours.getTopmostTours().toArray();
            } else if (parentElement instanceof Tour) {
                final Tour s = (Tour) parentElement;
                return s.getChildren().toArray();
            } else {
                return new Object[0];
            }
        }

        @Override
        public Object getParent(Object element) {
            if (element instanceof TourElement) {
                return this.tours.getParentFor((TourElement) element);
            } else {
                return null;
            }
        }

        @Override
        public boolean hasChildren(Object element) {
            return !(element instanceof Stop);
        }

        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            this.viewer = (TreeViewer) viewer;
        }

        @Override
        public void dispose() {
            this.viewer = null;
        }

        @Override
        public void toursChanged() {
            if (this.viewer == null) {
                return;
            }
            this.viewer.refresh();
            ensureActiveTourExpanded(this.viewer, this.tours);
        }

        @Override
        public void activeTourChanged(Tour oldActive, Tour newActive) {
            if (this.viewer == null) {
                return;
            }
            if (oldActive != null) {
                this.viewer.update(oldActive, null);
            }
            if (newActive != null) {
                this.viewer.update(newActive, null);
            }
            ensureActiveTourExpanded(this.viewer, this.tours);
        }

        @Override
        public void trackerStarts(CodeViewTracker tracker) {
            tracker.getStatistics().addListener(this);
        }

        @Override
        public void statisticsChanged(File absolutePath) {
            if (this.viewer == null) {
                return;
            }
            final Set<Tour> toursToRefresh = new HashSet<>();
            for (final Stop stop : this.tours.getStopsFor(absolutePath)) {
                this.viewer.update(stop, null);
                //making an item in a tree disappear when it is filtered is not as easy as it sounds
                //  (at least I haven't found nice methods in the API for it), we have to refresh the
                //  whole tour
                toursToRefresh.add(this.getTourFor(stop));
            }
            for (final Tour t : toursToRefresh) {
                this.viewer.refresh(t, false);
            }
        }

        private Tour getTourFor(Stop stop) {
            return this.tours.getParentFor(stop);
        }

        @Override
        public void notifyStopChange(Stop newStopOrNull) {
            if (newStopOrNull == null) {
                return;
            }
            if (this.viewer == null) {
                return;
            }
            final ITreeSelection oldSelection = (ITreeSelection) this.viewer.getSelection();
            if (oldSelection != null && oldSelection.toList().contains(newStopOrNull)) {
                //do nothing if the element is already selected
                return;
            }
            final StructuredSelection selection = new StructuredSelection(newStopOrNull);
            this.viewer.setSelection(selection, true);
        }

    }

    /**
     * Label provider for the tree with tours and stops.
     */
    private static final class TourAndStopLabelProvider extends CellLabelProvider {
        private static final RGB[] VIEW_COLORS = new RGB[] {
            new RGB(255, 235, 0),
            new RGB(223, 235, 0),
            new RGB(191, 235, 0),
            new RGB(159, 235, 0),
            new RGB(127, 235, 0),
            new RGB(95, 235, 0),
            new RGB(63, 235, 0),
            new RGB(32, 235, 0),
            new RGB(0, 235, 0)
        };

        private static final RGB IRRELEVANT_COLOR = new RGB(170, 170, 170);

        private String getText(Object element) {
            if (element instanceof Tour) {
                return ((Tour) element).getDescription().replace("\r", "").replace("\n", "; ");
            } else if (element instanceof Stop) {
                final Stop f = (Stop) element;
                if (f.isDetailedFragmentKnown()) {
                    return this.determineFilename(f) + ", "
                            + f.getMostRecentFragment().getFrom() + " - "
                            + f.getMostRecentFragment().getTo();
                } else {
                    return this.determineFilename(f);
                }
            } else {
                return element.toString();
            }
        }

        private Image getImage(Object element) {
            if (element instanceof Stop) {
                final Stop f = (Stop) element;
                final ViewStatistics statistics = TrackerManager.get().getStatistics();
                if (statistics != null && statistics.isMarkedAsChecked(f)) {
                    return ImageCache.getGreenCheckMark();
                }
                final ViewStatDataForStop viewRatio = this.determineViewRatio(f);
                if (viewRatio.isNotViewedAtAll()) {
                    if (f.isIrrelevantForReview()) {
                        return ImageCache.getColoredHalfCircle(
                                IRRELEVANT_COLOR,
                                IRRELEVANT_COLOR);
                    } else {
                        return null;
                    }
                } else {
                    if (f.isIrrelevantForReview()) {
                        return ImageCache.getColoredHalfCircle(
                                VIEW_COLORS[toColorIndex(viewRatio.getMaxRatio())],
                                VIEW_COLORS[toColorIndex(viewRatio.getAverageRatio())]);
                    } else {
                        return ImageCache.getColoredRectangle(
                                VIEW_COLORS[toColorIndex(viewRatio.getMaxRatio())],
                                VIEW_COLORS[toColorIndex(viewRatio.getAverageRatio())]);
                    }
                }
            } else if (element instanceof Tour) {
                final ToursInReview tours = ViewDataSource.get().getToursInReview();
                if (tours != null && tours.getActiveTour() == element) {
                    return ImageCache.getColoredDot(new RGB(255, 0, 0));
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }

        private static int toColorIndex(double ratio) {
            return (int) (ratio * (VIEW_COLORS.length - 1));
        }

        private ViewStatDataForStop determineViewRatio(Stop f) {
            return TrackerManager.get().determineViewRatio(f);
        }

        private String determineFilename(final Stop f) {
            final Position pos = PositionTransformer.toPosition(
                    f.getMostRecentFile().toLocalPath(),
                    -1,
                    ResourcesPlugin.getWorkspace());
            if (pos instanceof GlobalPosition) {
                return new File(f.getMostRecentFile().getPath()).getName();
            } else {
                return pos.getShortFileName();
            }
        }

        @Override
        public Point getToolTipShift(Object object) {
            //use a tool-tip shift to avoid that the user clicks on the tool-tip
            //  when he wants to click on the tree item
            return new Point(10, 10);
        }

        @Override
        public String getToolTipText(Object element) {
            if (element instanceof Tour) {
                return ((Tour) element).getDescription().replace(" + ", "\n + ");
            } else if (element instanceof Stop) {
                final Stop f = (Stop) element;
                if (f.getMostRecentFragment() != null) {
                    return f.getMostRecentFragment().toString();
                } else {
                    return f.getMostRecentFile().toString();
                }
            } else {
                return element.toString() + "(" + element.getClass() + ")";
            }
        }

        @Override
        public void update(ViewerCell cell) {
            final Object element = cell.getElement();
            cell.setText(this.getText(element));
            cell.setImage(this.getImage(element));
        }
    }

}
