package de.setsoftware.reviewtool.ui.views;

import java.util.List;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.contentmergeviewer.TextMergeViewer;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.part.ViewPart;

import de.setsoftware.reviewtool.model.changestructure.FileInRevision;
import de.setsoftware.reviewtool.model.changestructure.Fragment;
import de.setsoftware.reviewtool.ui.IStopViewer;

/**
 * Represents the foundation for concrete stop viewers.
 */
public abstract class AbstractStopViewer implements IStopViewer {

    /**
     * Subclass of TextMergeViewer which allows to access the left and right merge panes.
     */
    private final class SelectableTextMergeViewer extends TextMergeViewer {
        private static final int CONTEXT_LENGTH = 3;

        private static final int VIEWER_LEFT = 1;
        private static final int VIEWER_RIGHT = 2;
        private static final int NUM_VIEWERS = VIEWER_RIGHT + 1;

        private SourceViewer[] viewers;
        private int nextViewer;

        public SelectableTextMergeViewer(final Composite parent, final int style,
                final CompareConfiguration configuration) {
            super(parent, style, configuration);
        }

        @Override
        protected SourceViewer createSourceViewer(final Composite parent, final int textOrientation) {
            final SourceViewer viewer = super.createSourceViewer(parent, textOrientation);
            if (this.viewers == null) {
                this.viewers = new SourceViewer[NUM_VIEWERS];
            }
            if (this.nextViewer < this.viewers.length) {
                this.viewers[this.nextViewer++] = viewer;
            }
            return viewer;
        }

        /**
         * Selects some range of the left pane.
         * @param range The range to select.
         */
        public void selectLeft(final Position range) {
            this.select(this.viewers[VIEWER_LEFT], range);
        }

        /**
         * Selects some range of the right pane.
         * @param range The range to select.
         */
        public void selectRight(final Position range) {
            this.select(this.viewers[VIEWER_RIGHT], range);
        }

        /**
         * Selects some range of some pane.
         * @param viewer The pane to use.
         * @param range The range to select.
         */
        private void select(final SourceViewer viewer, final Position range) {
            if (viewer == null) {
                return;
            }
            viewer.setSelectedRange(range.getOffset(), range.getLength());
            viewer.revealRange(range.getOffset(), range.getLength());
            final int top = viewer.getTopIndex();
            viewer.setTopIndex(top < CONTEXT_LENGTH ? 0 : top - CONTEXT_LENGTH);
        }
    }

    /**
     * Builds a string containing the concatenated contents of the fragments passed.
     * @param fragments The list of fragments.
     * @return The concatenated contents of the fragments passed.
     */
    protected String mapFragmentsToString(final List<Fragment> fragments) {
        final StringBuilder text = new StringBuilder();
        boolean first = true;
        for (final Fragment f : fragments) {
            if (first) {
                first = false;
            } else {
                text.append("\n...\n\n");
            }
            text.append(f.getContent());
        }
        return text.toString();
    }

    /**
     * Creates a difference viewer.
     * @param view The {@link ViewPart} to use.
     * @param parent The {@link Composite} to use as parent for the difference viewer.
     * @param sourceRevision The source revision.
     * @param targetRevision The target revision.
     * @param sourceFragments The source fragments.
     * @param targetFragments The target fragments.
     */
    protected void createDiffViewer(final ViewPart view, final Composite parent,
            final FileInRevision sourceRevision, final FileInRevision targetRevision,
            final List<Fragment> sourceFragments, final List<Fragment> targetFragments,
            final Position rangeLeft, final Position rangeRight) {
        if (sourceFragments == null || targetFragments == null
                || sourceFragments.isEmpty() || targetFragments.isEmpty()) {
            this.createBinaryHunkViewer(view, parent);
        } else {
            this.createTextHunkViewer(parent, sourceRevision, targetRevision, sourceFragments, targetFragments,
                    rangeLeft, rangeRight);
        }
    }

    private void createBinaryHunkViewer(final ViewPart view, final Composite parent) {
        final Label label = new Label(parent, SWT.NULL);
        label.setText("binary");
        label.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT));
        ViewHelper.createContextMenuWithoutSelectionProvider(view, label);
    }

    private void createTextHunkViewer(final Composite parent,
            final FileInRevision sourceRevision, final FileInRevision targetRevision,
            final List<Fragment> sourceFragments, final List<Fragment> targetFragments,
            final Position rangeLeft, final Position rangeRight) {
        final CompareConfiguration compareConfiguration = new CompareConfiguration();
        compareConfiguration.setLeftLabel(this.toLabel(sourceRevision));
        compareConfiguration.setRightLabel(this.toLabel(targetRevision));
        final SelectableTextMergeViewer viewer = new SelectableTextMergeViewer(parent, SWT.BORDER,
                compareConfiguration);
        viewer.setInput(new DiffNode(
                new TextItem(sourceRevision.getRevision().toString(),
                        this.mapFragmentsToString(sourceFragments),
                        System.currentTimeMillis()),
                new TextItem(targetRevision.getRevision().toString(),
                        this.mapFragmentsToString(targetFragments),
                        System.currentTimeMillis())));
        if (rangeLeft != null) {
            viewer.selectLeft(rangeLeft);
        }
        if (rangeRight != null) {
            viewer.selectRight(rangeRight);
        }
    }

    private String toLabel(FileInRevision revision) {
        return revision.toString();
    }

}
