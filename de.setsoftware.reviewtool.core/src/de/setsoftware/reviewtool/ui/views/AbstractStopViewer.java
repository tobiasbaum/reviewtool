package de.setsoftware.reviewtool.ui.views;

import java.util.List;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.contentmergeviewer.TextMergeViewer;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.ITextPresentationListener;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.part.ViewPart;

import de.setsoftware.reviewtool.model.Constants;
import de.setsoftware.reviewtool.model.api.IFragment;
import de.setsoftware.reviewtool.model.api.IRevisionedFile;
import de.setsoftware.reviewtool.ui.IStopViewer;

/**
 * Represents the foundation for concrete stop viewers.
 */
public abstract class AbstractStopViewer implements IStopViewer {

    /**
     * Highlights a range by choosing a different colour.
     */
    private static final class ChangeHighlighter implements ITextPresentationListener {

        private final Position range;
        private Color hunkColor;

        /**
         * Constructor.
         * @param range The range to highlight.
         */
        ChangeHighlighter(final Position range) {
            this.range = range;
        }

        @Override
        public void applyTextPresentation(final TextPresentation textPresentation) {
            final Color fgColor = getTextColor();
            final StyleRange range = new StyleRange(this.range.getOffset(), this.range.getLength(), fgColor, null);
            textPresentation.mergeStyleRange(range);
        }

        /**
         * @return The color to use for the range.
         */
        private Color getTextColor() {
            if (this.hunkColor == null) {
                this.hunkColor = JFaceResources.getColorRegistry().get(Constants.INCOMING_COLOR);
                if (this.hunkColor == null) {
                    return Display.getCurrent().getSystemColor(SWT.COLOR_BLUE);
                }
            }
            return this.hunkColor;
        }
    }

    /**
     * Subclass of TextMergeViewer which allows to access the left and right merge panes.
     */
    private static final class SelectableTextMergeViewer extends TextMergeViewer {

        private static final int CONTEXT_LENGTH = 3;

        private static final int VIEWER_LEFT = 1;
        private static final int VIEWER_RIGHT = 2;
        private static final int NUM_VIEWERS = VIEWER_RIGHT + 1;

        private SourceViewer[] viewers;
        private int nextViewer;

        /**
         * Constructor.
         */
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
         * Remove any text selections as those hide in-line differences.
         */
        public void clearSelections() {
            for (final SourceViewer viewer : this.viewers) {
                viewer.setSelectedRange(0, 0);
            }
        }

        /**
         * Marks some range of the left pane.
         * @param range The range to mark.
         * @param reveal If {@code true}, the range is made visible within the pane.
         */
        public void markLeft(final Position range, final boolean reveal) {
            this.mark(this.viewers[VIEWER_LEFT], range, reveal);
        }

        /**
         * Marks some range of the right pane.
         * @param range The range to mark.
         * @param reveal If {@code true}, the range is made visible within the pane.
         */
        public void markRight(final Position range, final boolean reveal) {
            this.mark(this.viewers[VIEWER_RIGHT], range, reveal);
        }

        /**
         * Marks some range of some pane.
         * @param viewer The pane to use.
         * @param range The range to mark.
         * @param reveal If {@code true}, the range is made visible within the pane.
         */
        private void mark(final SourceViewer viewer, final Position range, final boolean reveal) {
            if (viewer == null) {
                return;
            }

            final ChangeHighlighter listener = new ChangeHighlighter(range);
            viewer.addTextPresentationListener(listener);
            viewer.invalidateTextPresentation();

            if (reveal) {
                viewer.revealRange(range.getOffset(), range.getLength());
                final int top = viewer.getTopIndex();
                viewer.setTopIndex(top < CONTEXT_LENGTH ? 0 : top - CONTEXT_LENGTH);
            }
        }
    }

    /**
     * Builds a string containing the concatenated contents of the fragments passed.
     * @param fragments The list of fragments.
     * @return The concatenated contents of the fragments passed.
     */
    protected String mapFragmentsToString(final List<? extends IFragment> fragments) {
        final StringBuilder text = new StringBuilder();
        boolean first = true;
        for (final IFragment f : fragments) {
            if (first) {
                first = false;
            } else {
                text.append("\n...\n\n");
            }
            text.append(f.getContentFullLines());
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
            final IRevisionedFile sourceRevision, final IRevisionedFile targetRevision,
            final List<? extends IFragment> sourceFragments,
            final List<? extends IFragment> targetFragments,
            final List<Position> rangesLeft,
            final List<Position> rangesRight) {
        if (sourceFragments.isEmpty() || targetFragments.isEmpty()) {
            this.createBinaryHunkViewer(view, parent);
        } else {
            this.createTextHunkViewer(view, parent, sourceRevision, targetRevision, sourceFragments, targetFragments,
                    rangesLeft, rangesRight);
        }
    }

    private void createBinaryHunkViewer(final ViewPart view, final Composite parent) {
        final Label label = new Label(parent, SWT.NULL);
        label.setText("binary");
        label.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT));
        ViewHelper.createContextMenuWithoutSelectionProvider(view, label);
    }

    private void createTextHunkViewer(
            final ViewPart viewPart,
            final Composite parent,
            final IRevisionedFile sourceRevision,
            final IRevisionedFile targetRevision,
            final List<? extends IFragment> sourceFragments,
            final List<? extends IFragment> targetFragments,
            final List<Position> rangesLeft,
            final List<Position> rangesRight) {
        final CompareConfiguration compareConfiguration = new CompareConfiguration();
        compareConfiguration.setLeftLabel(toLabel(sourceRevision));
        compareConfiguration.setRightLabel(toLabel(targetRevision));
        final SelectableTextMergeViewer viewer = new SelectableTextMergeViewer(parent, SWT.BORDER,
                compareConfiguration);
        viewer.setInput(new DiffNode(
                new TextItem(sourceRevision.getRevision().toString(),
                        this.mapFragmentsToString(sourceFragments),
                        System.currentTimeMillis()),
                new TextItem(targetRevision.getRevision().toString(),
                        this.mapFragmentsToString(targetFragments),
                        System.currentTimeMillis())));
        viewer.clearSelections();

        boolean reveal = true;
        for (final Position rangeRight : rangesRight) {
            viewer.markRight(rangeRight, reveal);
            reveal = false;
        }
        reveal = true;
        for (final Position rangeLeft : rangesLeft) {
            viewer.markLeft(rangeLeft, reveal);
            reveal = false;
        }
        for (final SourceViewer v : viewer.viewers) {
            ViewHelper.createContextMenu(viewPart, v.getTextWidget(), v);
        }
    }

    private static String toLabel(IRevisionedFile revision) {
        return revision.toString();
    }

}
