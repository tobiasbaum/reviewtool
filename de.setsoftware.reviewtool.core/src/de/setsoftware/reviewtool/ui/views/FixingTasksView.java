package de.setsoftware.reviewtool.ui.views;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
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
import org.eclipse.ui.part.ViewPart;

import de.setsoftware.reviewtool.base.Logger;
import de.setsoftware.reviewtool.base.ReviewtoolException;
import de.setsoftware.reviewtool.model.Constants;
import de.setsoftware.reviewtool.model.EclipseMarker;
import de.setsoftware.reviewtool.model.IReviewDataSaveListener;
import de.setsoftware.reviewtool.model.PositionTransformer;
import de.setsoftware.reviewtool.model.ReviewStateManager;
import de.setsoftware.reviewtool.model.changestructure.ChangestructureFactory;
import de.setsoftware.reviewtool.model.changestructure.PositionLookupTable;
import de.setsoftware.reviewtool.model.changestructure.ToursInReview;
import de.setsoftware.reviewtool.model.remarks.DummyMarker;
import de.setsoftware.reviewtool.model.remarks.FileLinePosition;
import de.setsoftware.reviewtool.model.remarks.GlobalPosition;
import de.setsoftware.reviewtool.model.remarks.Position;
import de.setsoftware.reviewtool.model.remarks.ReviewData;
import de.setsoftware.reviewtool.model.remarks.ReviewRemark;
import de.setsoftware.reviewtool.model.remarks.ReviewRemarkComment;
import de.setsoftware.reviewtool.model.remarks.ReviewRemarkException;
import de.setsoftware.reviewtool.model.remarks.ReviewRound;
import de.setsoftware.reviewtool.ui.dialogs.CorrectSyntaxDialog;

/**
 * A view to show the review remarks that need to be fixed (and the other remarks, too).
 */
public class FixingTasksView extends ViewPart implements ReviewModeListener, IReviewDataSaveListener {

    private Composite comp;
    private Composite currentContent;
    private final Set<String> ticketIdHistory = new LinkedHashSet<>();
    private TreeViewer treeViewer;
    private ReviewStateManager mgr;

    @Override
    public void createPartControl(Composite comp) {
        this.comp = comp;

        ViewDataSource.get().registerListener(this);
    }

    @Override
    public void setFocus() {
        if (this.currentContent != null) {
            this.currentContent.setFocus();
        }
    }

    @Override
    public void notifyIdle() {
        this.mgr = null;
        this.disposeOldContent();
        this.currentContent = this.createNoFixingContent();
        this.comp.layout();
    }

    @Override
    public void notifyReview(ReviewStateManager mgr, ToursInReview tours) {
        this.mgr = null;
        this.disposeOldContent();
        this.currentContent = this.createNoFixingContent();
        this.comp.layout();
    }

    @Override
    public void notifyFixing(ReviewStateManager mgr) {
        this.mgr = mgr;
        mgr.addSaveListener(this);
        final ReviewData data = CorrectSyntaxDialog.getCurrentReviewDataParsed(mgr, DummyMarker.FACTORY);
        this.disposeOldContent();
        this.currentContent = this.createFixingContent(data);
        this.ticketIdHistory.add(mgr.getTicketKey());
        this.comp.layout();
    }

    @Override
    public void onSave(String newData) {
        if (this.treeViewer != null) {
            try {
                final Object[] expandedElements = this.treeViewer.getExpandedElements();
                final ReviewData parsed =
                        ReviewData.parse(this.mgr.getReviewersForRounds(), DummyMarker.FACTORY, newData);
                this.treeViewer.setInput(parsed);
                this.treeViewer.setExpandedElements(expandedElements);
            } catch (final ReviewRemarkException e) {
                Logger.warn("changed remarks could not be parsed", e);
            }
        }
    }

    private Composite createFixingContent(final ReviewData remarks) {
        final Composite panel = new Composite(this.comp, SWT.NULL);
        panel.setLayout(new FillLayout());

        final TreeViewer tv = new TreeViewer(panel);
        ColumnViewerToolTipSupport.enableFor(tv);
        tv.setUseHashlookup(true);
        tv.setContentProvider(new RemarkTreeContentProvider(remarks));
        tv.setLabelProvider(new FixingTaskLabelProvider());
        tv.setInput(remarks);
        this.treeViewer = tv;

        final Tree tree = tv.getTree();
        tree.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                FixingTasksView.this.jumpToItem((TreeItem) e.item, tv);
            }
        });

        ViewHelper.createContextMenu(this, tv.getControl(), tv);
        tv.expandToLevel(2);

        return panel;
    }

    private void jumpToItem(final TreeItem item, TreeViewer tv) {
        if (item.getData() instanceof ReviewRemark) {
            jumpToRemark((ReviewRemark) item.getData());
        } else if (item.getData() instanceof CommentForRemark) {
            jumpToRemark(((CommentForRemark) item.getData()).remark);
        } else {
            tv.expandToLevel(item.getData(), 1);
            if (item.getItemCount() > 0) {
                this.jumpToItem(item.getItem(0), tv);
            }
        }
    }

    /**
     * Jumps to the given remark.
     */
    public static void jumpToRemark(ReviewRemark remark) {
        try {
            final IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

            //markers automatically shift when the text is edited, so use markers if possible
            final IMarker marker = findMarkerFor(remark);
            if (marker != null) {
                ViewHelper.openEditorForMarker(page, marker, false);
            } else {
                jumpToPosition(page, Position.parse(remark.getPositionString()));
            }
        } catch (final CoreException | IOException | ReviewtoolException e) {
            Logger.warn("could not jump to remark", e);
        }
    }

    private static IMarker findMarkerFor(ReviewRemark remark) {
        try {
            final IMarker[] markers = ResourcesPlugin.getWorkspace().getRoot().findMarkers(
                    Constants.REVIEWMARKER_ID, false, IResource.DEPTH_INFINITE);
            //first look for markers at that position
            for (final IMarker marker : markers) {
                if (remark.hasSameTextAndPositionAs(toRemark(marker))) {
                    return marker;
                }
            }
            //then look for markers with the same text
            for (final IMarker marker : markers) {
                if (remark.getText().equals(toRemark(marker).getText())) {
                    return marker;
                }
            }
            return null;
        } catch (final CoreException | ReviewtoolException e) {
            Logger.warn("error while searching marker", e);
            return null;
        }
    }

    private static ReviewRemark toRemark(final IMarker marker) {
        return ReviewRemark.getFor(EclipseMarker.wrap(marker));
    }

    private static void jumpToPosition(IWorkbenchPage page, Position pos) throws IOException, CoreException {
        if (pos instanceof GlobalPosition) {
            return;
        }

        final IPath path = PositionTransformer.toPath(pos);
        if (path == null) {
            return;
        }

        final IFileStore fileStore = EFS.getLocalFileSystem().getStore(path);
        final IEditorPart part = ViewHelper.openEditorForFile(page, fileStore, false);
        if (pos instanceof FileLinePosition && fileStore.fetchInfo().exists()) {
            final PositionLookupTable lookup = PositionLookupTable.create(fileStore);
            final int posStart = lookup.getCharsSinceFileStart(ChangestructureFactory.createPositionInText(
                    ((FileLinePosition) pos).getLine(), 1));
            ViewHelper.setSelection(part, new TextSelection(posStart, 0));
        }
    }

    private Composite createNoFixingContent() {
        final Composite panel = new Composite(this.comp, SWT.NULL);
        panel.setLayout(new FillLayout());
        final Label label = new Label(panel, SWT.NULL);
        if (this.ticketIdHistory.isEmpty()) {
            label.setText("Not in fixing mode.");
        } else {
            final List<String> reversedHistory = new ArrayList<>(this.ticketIdHistory);
            Collections.reverse(reversedHistory);
            label.setText("Not in fixing mode.\nLast finished: "
                    + reversedHistory.stream().limit(5).collect(Collectors.joining(", ")));
        }
        ViewHelper.createContextMenuWithoutSelectionProvider(this, label);
        return panel;
    }

    private void disposeOldContent() {
        if (this.treeViewer != null) {
            this.treeViewer = null;
        }
        if (this.currentContent != null) {
            this.currentContent.dispose();
        }
    }

    /**
     * The categories for remarks in the tree (i.e. the top-level of the tree).
     */
    private static enum CategoryForTree {
        RECENT_OTHER("Other remarks"),
        RECENT_POSITIVE("Positive"),
        WORK("To Fix"),
        RECENT_DONE("Already fixed"),
        OLDER("Older remarks");

        private final String text;

        private CategoryForTree(String text) {
            this.text = text;
        }
    }

    /**
     * Top-level category object for the tree.
     */
    private static final class CategoryItem {

        private final CategoryForTree targetCategory;

        public CategoryItem(CategoryForTree targetCategory) {
            this.targetCategory = targetCategory;
        }

        public List<ReviewRemark> getRelevantRemarks(ReviewData remarks) {
            final List<ReviewRemark> ret = new ArrayList<>();
            final List<ReviewRound> reviewRounds = remarks.getReviewRounds();
            for (int i = 0; i < reviewRounds.size(); i++) {
                final boolean lastRound = i == reviewRounds.size() - 1;
                for (final ReviewRemark remark : reviewRounds.get(i).getRemarks()) {
                    if (classify(remark, lastRound) == this.targetCategory) {
                        ret.add(remark);
                    }
                }
            }
            return ret;
        }

        public static CategoryForTree classify(ReviewRemark remark, boolean lastRound) {
            if (lastRound) {
                switch (remark.getRemarkType()) {
                case OTHER:
                    return CategoryForTree.RECENT_OTHER;
                case POSITIVE:
                    return CategoryForTree.RECENT_POSITIVE;
                case ALREADY_FIXED:
                    return CategoryForTree.RECENT_DONE;
                case CAN_FIX:
                case MUST_FIX:
                case TEMPORARY:
                default:
                    return CategoryForTree.WORK;
                }
            } else {
                return remark.needsFixing() ? CategoryForTree.WORK : CategoryForTree.OLDER;
            }
        }

        @Override
        public String toString() {
            return this.targetCategory.text;
        }

        @Override
        public int hashCode() {
            return this.targetCategory.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof CategoryItem)) {
                return false;
            }
            return this.targetCategory == ((CategoryItem) o).targetCategory;
        }

    }

    /**
     * Helper class that combines a {@link ReviewRemarkComment} with its {@link ReviewRemark},
     * because there is no getter for the parent remark on the comment.
     */
    private class CommentForRemark {
        private final ReviewRemark remark;
        private final ReviewRemarkComment comment;

        public CommentForRemark(ReviewRemark parentElement, ReviewRemarkComment comment) {
            this.remark = parentElement;
            this.comment = comment;
        }
    }

    /**
     * Provides the tree with the remarks.
     */
    private class RemarkTreeContentProvider implements ITreeContentProvider {

        private ReviewData remarks;

        public RemarkTreeContentProvider(ReviewData remarks) {
            this.remarks = remarks;
        }

        @Override
        public Object[] getElements(Object inputElement) {
            assert inputElement == this.remarks;
            return this.getChildren(null);
        }

        @Override
        public Object[] getChildren(Object parentElement) {
            if (parentElement == null) {
                return this.getTopLevelCategories().toArray();
            } else if (parentElement instanceof CategoryItem) {
                return ((CategoryItem) parentElement).getRelevantRemarks(this.remarks).toArray();
            } else if (parentElement instanceof ReviewRemark) {
                return ((ReviewRemark) parentElement).getFollowUpComments().stream()
                        .map(c -> new CommentForRemark((ReviewRemark) parentElement, c)).toArray();
            } else {
                return new Object[0];
            }
        }

        private List<CategoryItem> getTopLevelCategories() {
            final EnumSet<CategoryForTree> activeCategories = EnumSet.noneOf(CategoryForTree.class);
            final List<ReviewRound> reviewRounds = this.remarks.getReviewRounds();
            for (int i = 0; i < reviewRounds.size(); i++) {
                final boolean lastRound = i == reviewRounds.size() - 1;
                for (final ReviewRemark remark : reviewRounds.get(i).getRemarks()) {
                    activeCategories.add(CategoryItem.classify(remark, lastRound));
                }
            }
            final List<CategoryItem> ret = new ArrayList<>();
            for (final CategoryForTree c : activeCategories) {
                ret.add(new CategoryItem(c));
            }
            return ret;
        }

        @Override
        public Object getParent(Object element) {
            if (element instanceof ReviewRemark) {
                for (final CategoryItem c : this.getTopLevelCategories()) {
                    if (Arrays.asList(c.getRelevantRemarks(this.remarks)).contains(element)) {
                        return c;
                    }
                }
                return null;
            } else if (element instanceof CommentForRemark) {
                return ((CommentForRemark) element).remark;
            } else {
                return null;
            }
        }

        @Override
        public boolean hasChildren(Object element) {
            return this.getChildren(element).length > 0;
        }

        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            this.remarks = (ReviewData) newInput;
        }

        @Override
        public void dispose() {
        }

    }

    /**
     * Label provider for the tree with tours and stops.
     */
    private static final class FixingTaskLabelProvider extends CellLabelProvider {

        private static final RGB BLUE = new RGB(0, 0, 100);
        private static final RGB GREEN = new RGB(0, 200, 0);
        private static final RGB DARK_GREEN = new RGB(0, 100, 0);
        private static final RGB YELLOW = new RGB(200, 200, 10);
        private static final RGB RED = new RGB(255, 0, 0);
        private static final RGB GRAY = new RGB(50, 50, 50);

        private String getText(Object element) {
            if (element instanceof ReviewRemark) {
                final ReviewRemark r = (ReviewRemark) element;
                final ReviewRemarkComment remarkText = r.getComments().get(0);
                return (r.getPositionString() + " " + remarkText.getText()).trim();
            } else if (element instanceof CommentForRemark) {
                final ReviewRemarkComment c = ((CommentForRemark) element).comment;
                return c.getUser() + ": " + c.getText();
            } else {
                return element.toString();
            }
        }

        private Image getImage(Object element) {
            if (element instanceof ReviewRemark) {
                return this.determineImageForRemark((ReviewRemark) element);
            } else {
                return null;
            }
        }

        private Image determineImageForRemark(ReviewRemark remark) {
            final char letter1;
            final RGB color1;
            switch (remark.getRemarkType()) {
            case ALREADY_FIXED:
                letter1 = 'D';
                color1 = GREEN;
                break;
            case CAN_FIX:
                letter1 = 'C';
                color1 = remark.needsFixing() ? YELLOW : GREEN;
                break;
            case OTHER:
                letter1 = 'O';
                color1 = GRAY;
                break;
            case POSITIVE:
                letter1 = 'P';
                color1 = BLUE;
                break;
            case TEMPORARY:
                letter1 = 'T';
                color1 = RED;
                break;
            case MUST_FIX:
            default:
                letter1 = 'M';
                color1 = remark.needsFixing() ? RED : GREEN;
                break;
            }

            final char letter2;
            final RGB color2;
            switch (remark.getResolution()) {
            case FIXED:
                letter2 = '✓';
                color2 = DARK_GREEN;
                break;
            case QUESTION:
                letter2 = '?';
                color2 = BLUE;
                break;
            case WONT_FIX:
                letter2 = 'x';
                color2 = RED;
                break;
            case OPEN:
            default:
                letter2 = ' ';
                color2 = null;
                break;
            }

            return ImageCache.getLetterBasedIcon(letter1, color1, letter2, color2);
        }

        @Override
        public Point getToolTipShift(Object object) {
            //use a tool-tip shift to avoid that the user clicks on the tool-tip
            //  when he wants to click on the tree item
            return new Point(10, 10);
        }

        @Override
        public String getToolTipText(Object element) {
            if (element instanceof CategoryItem) {
                return null;
            } else {
                return this.getText(element);
            }
        }

        @Override
        public void update(ViewerCell cell) {
            final Object element = cell.getElement();
            cell.setText(this.getText(element).replace('\n', ' '));
            cell.setImage(this.getImage(element));
        }
    }

}
