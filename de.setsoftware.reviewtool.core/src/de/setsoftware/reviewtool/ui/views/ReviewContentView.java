package de.setsoftware.reviewtool.ui.views;

import java.io.File;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.ViewPart;

import de.setsoftware.reviewtool.base.ReviewtoolException;
import de.setsoftware.reviewtool.model.PositionTransformer;
import de.setsoftware.reviewtool.model.ReviewStateManager;
import de.setsoftware.reviewtool.model.changestructure.Stop;
import de.setsoftware.reviewtool.model.changestructure.Tour;
import de.setsoftware.reviewtool.model.changestructure.ToursInReview;
import de.setsoftware.reviewtool.model.changestructure.ToursInReview.IToursInReviewChangeListener;
import de.setsoftware.reviewtool.viewtracking.CodeViewTracker;
import de.setsoftware.reviewtool.viewtracking.ITrackerCreationListener;
import de.setsoftware.reviewtool.viewtracking.IViewStatisticsListener;
import de.setsoftware.reviewtool.viewtracking.TrackerManager;

/**
 * A review to show the content (tours and stops) belonging to a review.
 */
public class ReviewContentView extends ViewPart implements ReviewModeListener {

    private Composite comp;
    private Composite currentContent;

    @Override
    public void createPartControl(Composite comp) {
        this.comp = comp;

        ViewDataSource.get().registerListener(this);
    }

    @Override
    public void setFocus() {
    }

    @Override
    public void notifyReview(ReviewStateManager mgr, ToursInReview tours) {
        this.disposeOldContent();
        this.currentContent = this.createReviewContent(tours);
        this.comp.layout();
    }

    private Composite createReviewContent(final ToursInReview tours) {
        final Composite panel = new Composite(this.comp, SWT.NULL);
        panel.setLayout(new FillLayout());

        final TreeViewer tv = new TreeViewer(panel);
        tv.setContentProvider(new ViewContentProvider(tours));
        tv.setLabelProvider(new TourAndStopLabelProvider());
        tv.setInput(tours);

        final Tree tree = tv.getTree();
        tree.addListener(SWT.MouseDoubleClick, new Listener() {
            @Override
            public void handleEvent(Event event) {
                final Point point = new Point(event.x, event.y);
                final TreeItem item = tree.getItem(point);
                if (item != null) {
                    if (item.getData() instanceof Stop) {
                        final Stop stop = (Stop) item.getData();
                        final Tour tour = (Tour) item.getParentItem().getData();
                        ReviewContentView.this.jumpTo(tv, tours, tour, stop);
                    }
                }
            }
        });

        ViewHelper.createContextMenu(this, tv.getControl(), tv);
        ensureActiveTourExpanded(tv, tours);

        return panel;
    }

    private void jumpTo(TreeViewer tv, ToursInReview tours, Tour tour, Stop fragment) {
        CurrentFragment.setCurrentFragment(fragment);
        try {
            tours.ensureTourActive(tour, new RealMarkerFactory());
            ensureActiveTourExpanded(tv, tours);

            this.openEditorFor(fragment);
        } catch (final CoreException e) {
            throw new ReviewtoolException(e);
        }
    }

    private static void ensureActiveTourExpanded(TreeViewer tv, ToursInReview tours) {
        final Tour activeTour = tours.getActiveTour();
        tv.expandToLevel(activeTour, TreeViewer.ALL_LEVELS);
    }

    private void openEditorFor(Stop fragment) throws CoreException {
        final IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        final IMarker marker = ToursInReview.createMarkerFor(new RealMarkerFactory(), fragment);
        if (marker != null) {
            IDE.openEditor(page, marker);
            marker.delete();
        } else {
            final IFileStore fileStore =
                    EFS.getLocalFileSystem().getStore(fragment.getMostRecentFile().toLocalPath());
            IDE.openEditorOnFileStore(page, fileStore);
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

    private Composite createNoReviewContent() {
        final Composite panel = new Composite(this.comp, SWT.NULL);
        panel.setLayout(new FillLayout());
        final Label label = new Label(panel, SWT.NULL);
        label.setText("Nicht im Review-Modus");
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
    private static class ViewContentProvider implements ITreeContentProvider, IToursInReviewChangeListener,
            ITrackerCreationListener, IViewStatisticsListener {

        private final ToursInReview tours;
        private TreeViewer viewer;

        public ViewContentProvider(ToursInReview tours) {
            this.tours = tours;
            this.tours.registerListener(this);
            TrackerManager.get().registerListener(this);
        }

        @Override
        public Object[] getElements(Object inputElement) {
            assert inputElement == this.tours;
            return this.getChildren(null);
        }

        @Override
        public Object[] getChildren(Object parentElement) {
            if (parentElement == null) {
                return this.tours.getTours().toArray();
            } else if (parentElement instanceof Tour) {
                final Tour s = (Tour) parentElement;
                return s.getStops().toArray();
            } else {
                return new Object[0];
            }
        }

        @Override
        public Object getParent(Object element) {
            if (element instanceof Stop) {
                final Stop f = (Stop) element;
                for (final Tour s : this.tours.getTours()) {
                    if (s.getStops().contains(f)) {
                        return s;
                    }
                }
                return null;
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
            if (this.viewer != null) {
                this.viewer.refresh();
                ensureActiveTourExpanded(this.viewer, this.tours);
            }
        }

        @Override
        public void trackerStarts(CodeViewTracker tracker) {
            tracker.getStatistics().addListener(this);
        }

        @Override
        public void statisticsChanged(File absolutePath) {
            for (final Stop stop : this.tours.getStopsFor(absolutePath)) {
                this.viewer.update(stop, null);
            }
        }
    }

    /**
     * Label provider for the tree with tours and stops.
     */
    private static final class TourAndStopLabelProvider extends LabelProvider {
        private final Image[] viewStatImages = new Image[] {
            createColoredRectangle(new RGB(255, 235, 0)),
            createColoredRectangle(new RGB(223, 235, 0)),
            createColoredRectangle(new RGB(191, 235, 0)),
            createColoredRectangle(new RGB(159, 235, 0)),
            createColoredRectangle(new RGB(127, 235, 0)),
            createColoredRectangle(new RGB(95, 235, 0)),
            createColoredRectangle(new RGB(63, 235, 0)),
            createColoredRectangle(new RGB(32, 235, 0)),
            createColoredRectangle(new RGB(0, 235, 0))
        };

        private static Image createColoredRectangle(RGB rgb) {
            final Device dev = Display.getDefault();
            final Rectangle rect = new Rectangle(0, 0, 7, 10);
            final Image img = new Image(dev, rect);
            final GC gc = new GC(img);
            final Color color = new Color(dev, rgb);
            gc.setBackground(color);
            gc.setForeground(color);
            gc.drawRectangle(rect);
            gc.fillRectangle(rect);
            gc.dispose();
            return img;
        }

        @Override
        public String getText(Object element) {
            if (element instanceof Tour) {
                return ((Tour) element).getDescription();
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

        @Override
        public Image getImage(Object element) {
            if (element instanceof Stop) {
                final Stop f = (Stop) element;
                final double viewRatio = this.determineViewRatio(f);
                if (viewRatio <= 0) {
                    return null;
                } else {
                    final int index = (int) (viewRatio * (this.viewStatImages.length - 1));
                    return this.viewStatImages[index];
                }
            } else {
                return null;
            }
        }


        private double determineViewRatio(Stop f) {
            return TrackerManager.get().determineViewRatio(f);
        }

        private String determineFilename(final Stop f) {
            final IResource resource = f.getMostRecentFile().determineResource();
            if (resource != null) {
                return PositionTransformer.toPosition(resource, -1).getShortFileName();
            } else {
                return new File(f.getMostRecentFile().getPath()).getName();
            }
        }
    }

}
