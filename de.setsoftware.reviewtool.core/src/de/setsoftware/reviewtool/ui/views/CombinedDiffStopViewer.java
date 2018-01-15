package de.setsoftware.reviewtool.ui.views;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.contentmergeviewer.TextMergeViewer;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.jdt.internal.ui.compare.JavaMergeViewer;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.ITextPresentationListener;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.viewers.ContentViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.part.ViewPart;

import de.setsoftware.reviewtool.base.LineSequence;
import de.setsoftware.reviewtool.base.ReviewtoolException;
import de.setsoftware.reviewtool.model.Constants;
import de.setsoftware.reviewtool.model.api.IFileDiff;
import de.setsoftware.reviewtool.model.api.IFileHistoryNode;
import de.setsoftware.reviewtool.model.api.IFragment;
import de.setsoftware.reviewtool.model.api.IHunk;
import de.setsoftware.reviewtool.model.api.IRevisionedFile;
import de.setsoftware.reviewtool.model.changestructure.ChangestructureFactory;
import de.setsoftware.reviewtool.model.changestructure.FileInRevision;
import de.setsoftware.reviewtool.model.changestructure.Stop;
import de.setsoftware.reviewtool.model.changestructure.ToursInReview;
import de.setsoftware.reviewtool.ui.IStopViewer;

/**
 * Displays all differences of a {@link Stop} combined in a single window.
 */
public class CombinedDiffStopViewer implements IStopViewer {

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
            final Color fgColor = this.getTextColor();
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
     * Bridge implementation for SelectableMergeViewers.
     */
    private static final class SelectableMergeViewerImpl {
        private static final int CONTEXT_LENGTH = 3;

        private static final int VIEWER_LEFT = 1;
        private static final int VIEWER_RIGHT = 2;
        private static final int NUM_VIEWERS = VIEWER_RIGHT + 1;

        private SourceViewer[] viewers;
        private int nextViewer;

        protected SourceViewer createSourceViewer(
                SourceViewer superResult, final Composite parent, final int textOrientation) {
            if (this.viewers == null) {
                this.viewers = new SourceViewer[NUM_VIEWERS];
            }
            if (this.nextViewer < this.viewers.length) {
                this.viewers[this.nextViewer++] = superResult;
            }
            return superResult;
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
     * Interface for the addition behavior we need for merge viewers.
     */
    public static interface SelectableMergeViewer {

        /**
         * Remove any text selections as those hide in-line differences.
         */
        public abstract void clearSelections();

        /**
         * Marks some range of the left pane.
         * @param range The range to mark.
         * @param reveal If {@code true}, the range is made visible within the pane.
         */
        public abstract void markLeft(final Position range, final boolean reveal);

        /**
         * Marks some range of the right pane.
         * @param range The range to mark.
         * @param reveal If {@code true}, the range is made visible within the pane.
         */
        public abstract void markRight(final Position range, final boolean reveal);

        /**
         * Returns the used viewers.
         */
        public abstract SourceViewer[] getViewers();

        /**
         * Corresponds to {@link ContentViewer#setInput}.
         */
        public abstract void setInput(Object input);

    }

    /**
     * Subclass of TextMergeViewer which allows to access the left and right merge panes.
     */
    private static final class SelectableTextMergeViewer extends TextMergeViewer implements SelectableMergeViewer {

        private SelectableMergeViewerImpl selectableImpl;

        /**
         * Constructor.
         */
        public SelectableTextMergeViewer(final Composite parent, final int style,
                final CompareConfiguration configuration) {
            super(parent, style, configuration);
        }

        private SelectableMergeViewerImpl getSelectableImpl() {
            //the constructor of the super class calls virtual methods, therefore we cannot
            //  initialize the field in the constructor
            if (this.selectableImpl == null) {
                this.selectableImpl = new SelectableMergeViewerImpl();
            }
            return this.selectableImpl;
        }

        @Override
        protected SourceViewer createSourceViewer(final Composite parent, final int textOrientation) {
            final SourceViewer viewer = super.createSourceViewer(parent, textOrientation);
            return this.getSelectableImpl().createSourceViewer(viewer, parent, textOrientation);
        }

        @Override
        public void clearSelections() {
            this.getSelectableImpl().clearSelections();
        }

        @Override
        public void markLeft(final Position range, final boolean reveal) {
            this.getSelectableImpl().markLeft(range, reveal);
        }

        @Override
        public void markRight(final Position range, final boolean reveal) {
            this.getSelectableImpl().markRight(range, reveal);
        }

        @Override
        public SourceViewer[] getViewers() {
            return this.getSelectableImpl().viewers;
        }
    }

    /**
     * Subclass of JavaMergeViewer which allows to access the left and right merge panes.
     */
    private static final class SelectableJavaMergeViewer extends JavaMergeViewer implements SelectableMergeViewer {

        private SelectableMergeViewerImpl selectableImpl;

        /**
         * Constructor.
         */
        public SelectableJavaMergeViewer(final Composite parent, final int style,
                final CompareConfiguration configuration) {
            super(parent, style, configuration);
        }

        private SelectableMergeViewerImpl getSelectableImpl() {
            //the constructor of the super class calls virtual methods, therefore we cannot
            //  initialize the field in the constructor
            if (this.selectableImpl == null) {
                this.selectableImpl = new SelectableMergeViewerImpl();
            }
            return this.selectableImpl;
        }

        @Override
        protected SourceViewer createSourceViewer(final Composite parent, final int textOrientation) {
            final SourceViewer viewer = super.createSourceViewer(parent, textOrientation);
            return this.getSelectableImpl().createSourceViewer(viewer, parent, textOrientation);
        }

        @Override
        public void clearSelections() {
            this.getSelectableImpl().clearSelections();
        }

        @Override
        public void markLeft(final Position range, final boolean reveal) {
            this.getSelectableImpl().markLeft(range, reveal);
        }

        @Override
        public void markRight(final Position range, final boolean reveal) {
            this.getSelectableImpl().markRight(range, reveal);
        }

        @Override
        public SourceViewer[] getViewers() {
            return this.getSelectableImpl().viewers;
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
        final SelectableMergeViewer viewer;
        if (this.isJava(targetRevision)) {
            viewer = new SelectableJavaMergeViewer(parent, SWT.BORDER, compareConfiguration);
        } else {
            viewer = new SelectableTextMergeViewer(parent, SWT.BORDER, compareConfiguration);
        }
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
        for (final SourceViewer v : viewer.getViewers()) {
            ViewHelper.createContextMenu(viewPart, v.getTextWidget(), v);
        }
    }

    private boolean isJava(IRevisionedFile file) {
        return file.getPath().endsWith(".java");
    }

    private static String toLabel(IRevisionedFile revision) {
        return revision.toString();
    }

    @Override
    public void createStopView(final ViewPart view, final Composite scrollContent, final Stop stop) {
        final ToursInReview tours = ViewDataSource.get().getToursInReview();
        if (tours == null) {
            return;
        }

        final Map<IRevisionedFile, IRevisionedFile> changes = stop.getHistory();
        final List<? extends IRevisionedFile> sortedRevs = FileInRevision.sortByRevision(changes.keySet());
        final IRevisionedFile firstRevision = sortedRevs.get(0);
        final IRevisionedFile lastRevision = changes.get(sortedRevs.get(sortedRevs.size() - 1));

        final IFileHistoryNode node = tours.getFileHistoryNode(lastRevision);
        if (node != null) {
            if (stop.isBinaryChange()) {
                this.createDiffViewer(view, scrollContent, firstRevision, lastRevision,
                        new ArrayList<IFragment>(), new ArrayList<IFragment>(),
                        new ArrayList<Position>(), new ArrayList<Position>());
            } else {
                final IFileHistoryNode ancestor = tours.getFileHistoryNode(firstRevision);
                final Set<? extends IFileDiff> diffs = node.buildHistories(ancestor);
                // TODO: we currently cowardly refuse to display any history beyond the first merge parent
                // TODO: it happened to me once that "diffs" was empty here; fix here or somewhere else?
                final IFileDiff diff = diffs.iterator().next();

                final List<IFragment> origins = new ArrayList<>();
                for (final IRevisionedFile file : changes.keySet()) {
                    for (final IHunk hunk : stop.getContentFor(file)) {
                        origins.addAll(hunk.getTarget().getOrigins());
                    }
                }
                final List<? extends IHunk> relevantHunks = diff.getHunksWithTargetChangesInOneOf(origins);

                final LineSequence oldContents;
                final LineSequence newContents;
                try {
                    oldContents = fileToLineSequence(firstRevision);
                    newContents = fileToLineSequence(lastRevision);
                } catch (final Exception e) {
                    throw new ReviewtoolException(e);
                }

                final List<Position> oldPositions = new ArrayList<>();
                final List<Position> newPositions = new ArrayList<>();

                for (final IHunk hunk : relevantHunks) {
                    final IFragment sourceFragment = hunk.getSource();
                    final IFragment targetFragment = hunk.getTarget();

                    final int oldStartOffset =
                            oldContents.getStartPositionOfLine(sourceFragment.getFrom().getLine() - 1)
                                    + (sourceFragment.getFrom().getColumn() - 1);
                    final int oldEndOffset =
                            oldContents.getStartPositionOfLine(sourceFragment.getTo().getLine() - 1)
                                    + (sourceFragment.getTo().getColumn() - 1);
                    final int newStartOffset =
                            newContents.getStartPositionOfLine(targetFragment.getFrom().getLine() - 1)
                                    + (targetFragment.getFrom().getColumn() - 1);
                    final int newEndOffset =
                            newContents.getStartPositionOfLine(targetFragment.getTo().getLine() - 1)
                                    + (targetFragment.getTo().getColumn() - 1);

                    oldPositions.add(new Position(oldStartOffset, oldEndOffset - oldStartOffset));
                    newPositions.add(new Position(newStartOffset, newEndOffset - newStartOffset));
                }

                this.createDiffViewer(view, scrollContent, firstRevision, lastRevision,
                        Arrays.asList(this.createFragmentForWholeFile(firstRevision, oldContents)),
                        Arrays.asList(this.createFragmentForWholeFile(lastRevision, newContents)),
                        oldPositions,
                        newPositions);
            }
        }
    }

    private IFragment createFragmentForWholeFile(final IRevisionedFile revision, final LineSequence contents) {
        final int numLines = contents.getNumberOfLines();
        final IFragment fragment = ChangestructureFactory.createFragment(revision,
                ChangestructureFactory.createPositionInText(1, 1),
                ChangestructureFactory.createPositionInText(numLines + 1, 1));
        return fragment;
    }

    private static LineSequence fileToLineSequence(final IRevisionedFile file) throws Exception {
        final byte[] data = file.getContents();

        try {
            StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(data));
            return new LineSequence(data, "UTF-8");
        } catch (final CharacterCodingException e) {
            return new LineSequence(data, "ISO-8859-1");
        }
    }
}
