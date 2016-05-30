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
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
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
import de.setsoftware.reviewtool.model.changestructure.Slice;
import de.setsoftware.reviewtool.model.changestructure.SliceFragment;
import de.setsoftware.reviewtool.model.changestructure.SlicesInReview;

/**
 * A review to show the content (slices and fragments) belonging to a review.
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
    public void notifyReview(ReviewStateManager mgr, SlicesInReview slices) {
        this.disposeOldContent();
        this.currentContent = this.createReviewContent(slices);
        this.comp.layout();
    }

    private Composite createReviewContent(SlicesInReview slices) {
        final Composite panel = new Composite(this.comp, SWT.NULL);
        panel.setLayout(new FillLayout());

        final TreeViewer tv = new TreeViewer(panel);
        tv.setContentProvider(new ViewContentProvider(slices));
        tv.setLabelProvider(new SliceAndFragmentLabelProvider());
        tv.setInput(slices);

        final Tree tree = tv.getTree();
        tree.addListener(SWT.MouseDoubleClick, new Listener() {
            @Override
            public void handleEvent(Event event) {
                final Point point = new Point(event.x, event.y);
                final TreeItem item = tree.getItem(point);
                if (item != null) {
                    if (item.getData() instanceof SliceFragment) {
                        ReviewContentView.this.jumpTo((SliceFragment) item.getData());
                    }
                }
            }
        });

        ViewHelper.createContextMenu(this, tv.getControl(), tv);

        return panel;
    }

    private void jumpTo(SliceFragment fragment) {
        CurrentFragment.setCurrentFragment(fragment);
        try {
            final IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
            final IMarker marker = SlicesInReview.createMarkerFor(new RealMarkerFactory(), fragment);
            if (marker != null) {
                IDE.openEditor(page, marker);
                marker.delete();
            } else {
                final IFileStore fileStore =
                        EFS.getLocalFileSystem().getStore(fragment.getMostRecentFile().toLocalPath());
                IDE.openEditorOnFileStore(page, fileStore);
            }
        } catch (final CoreException e) {
            throw new ReviewtoolException(e);
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
     * Provides the tree consisting of slices and fragments.
     */
    private static class ViewContentProvider implements ITreeContentProvider {

        private final SlicesInReview slices;

        public ViewContentProvider(SlicesInReview slices) {
            this.slices = slices;
        }

        @Override
        public Object[] getElements(Object inputElement) {
            assert inputElement == this.slices;
            return this.getChildren(null);
        }

        @Override
        public Object[] getChildren(Object parentElement) {
            if (parentElement == null) {
                return this.slices.getSlices().toArray();
            } else if (parentElement instanceof Slice) {
                final Slice s = (Slice) parentElement;
                return s.getFragments().toArray();
            } else {
                return new Object[0];
            }
        }

        @Override
        public Object getParent(Object element) {
            if (element instanceof SliceFragment) {
                final SliceFragment f = (SliceFragment) element;
                for (final Slice s : this.slices.getSlices()) {
                    if (s.getFragments().contains(f)) {
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
            return !(element instanceof SliceFragment);
        }

        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        }

        @Override
        public void dispose() {
        }

    }

    /**
     * Label provider for the tree with slices and fragments.
     */
    private static final class SliceAndFragmentLabelProvider extends LabelProvider {
        @Override
        public String getText(Object element) {
            if (element instanceof Slice) {
                return ((Slice) element).getDescription();
            } else if (element instanceof SliceFragment) {
                final SliceFragment f = (SliceFragment) element;
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

        private String determineFilename(final SliceFragment f) {
            final IResource resource = f.getMostRecentFile().determineResource();
            if (resource != null) {
                return PositionTransformer.toPosition(resource, -1).getShortFileName();
            } else {
                return new File(f.getMostRecentFile().getPath()).getName();
            }
        }
    }

}
