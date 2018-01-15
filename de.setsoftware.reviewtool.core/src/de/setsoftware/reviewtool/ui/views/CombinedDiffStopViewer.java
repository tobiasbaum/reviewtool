package de.setsoftware.reviewtool.ui.views;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.part.ViewPart;

import de.setsoftware.reviewtool.base.LineSequence;
import de.setsoftware.reviewtool.base.Pair;
import de.setsoftware.reviewtool.base.ReviewtoolException;
import de.setsoftware.reviewtool.diffalgorithms.DiffAlgorithmFactory;
import de.setsoftware.reviewtool.model.Constants;
import de.setsoftware.reviewtool.model.api.IFileHistoryNode;
import de.setsoftware.reviewtool.model.api.IFragment;
import de.setsoftware.reviewtool.model.api.IRevisionedFile;
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

        private final List<Position> ranges;
        private Color hunkColor;

        /**
         * Constructor.
         * @param ranges The ranges to highlight.
         */
        ChangeHighlighter(final List<Position> ranges) {
            this.ranges = ranges;
        }

        @Override
        public void applyTextPresentation(final TextPresentation textPresentation) {
            final Color fgColor = this.getTextColor();
            for (final Position r : this.ranges) {
                final StyleRange range = new StyleRange(r.getOffset(), r.getLength(), fgColor, null);
                textPresentation.mergeStyleRange(range);
            }
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
     * Highlights a range by choosing a different background colour.
     */
    private static final class BackgroundHighlighter implements ITextPresentationListener {

        private final List<Position> ranges;
        private final String colorKey;
        private final RGB defaultColor;
        private Color hunkColor;

        /**
         * Constructor.
         * @param ranges The ranges to highlight.
         */
        BackgroundHighlighter(final List<Position> ranges, String colorKey, RGB defaultColor) {
            this.ranges = ranges;
            this.colorKey = colorKey;
            this.defaultColor = defaultColor;
        }

        @Override
        public void applyTextPresentation(final TextPresentation textPresentation) {
            final Color bgColor = this.getTextColor();
            for (final Position r : this.ranges) {
                final StyleRange range = new StyleRange(r.getOffset(), r.getLength(), null, bgColor);
                textPresentation.mergeStyleRange(range);
            }
        }

        /**
         * @return The color to use for the range.
         */
        private Color getTextColor() {
            if (this.hunkColor == null) {
                if (!JFaceResources.getColorRegistry().hasValueFor(this.colorKey)) {
                    JFaceResources.getColorRegistry().put(this.colorKey, this.defaultColor);
                }
                this.hunkColor = JFaceResources.getColorRegistry().get(this.colorKey);
            }
            return this.hunkColor;
        }
    }

    /**
     * Bridge implementation for SelectableMergeViewers.
     */
    private static final class SelectableMergeViewerImpl {

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

        public SourceViewer getLeft() {
            return this.viewers[VIEWER_LEFT];
        }

        public SourceViewer getRight() {
            return this.viewers[VIEWER_RIGHT];
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
         * Returns the used viewers.
         */
        public abstract SourceViewer[] getViewers();

        public abstract SourceViewer getLeft();

        public abstract SourceViewer getRight();

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
        public SourceViewer getLeft() {
            return this.getSelectableImpl().getLeft();
        }

        @Override
        public SourceViewer getRight() {
            return this.getSelectableImpl().getRight();
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
        public SourceViewer getLeft() {
            return this.getSelectableImpl().getLeft();
        }

        @Override
        public SourceViewer getRight() {
            return this.getSelectableImpl().getRight();
        }

        @Override
        public SourceViewer[] getViewers() {
            return this.getSelectableImpl().viewers;
        }
    }

    /**
     * Helper class to capture file contents as strings as well as bytes.
     */
    private static final class FileContent {
        private final byte[] bytes;
        private final LineSequence lines;
        private final String charset;

        public FileContent(byte[] data, String charset) throws IOException {
            this.bytes = data;
            this.lines = new LineSequence(data, charset);
            this.charset = charset;
        }
    }

    private static final int CONTEXT_LENGTH = 3;

    private void createBinaryHunkViewer(final ViewPart view, final Composite parent) {
        final Label label = new Label(parent, SWT.NULL);
        label.setText("binary");
        label.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT));
        ViewHelper.createContextMenuWithoutSelectionProvider(view, label);
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
                this.createBinaryHunkViewer(view, scrollContent);
            } else {

                final FileContent oldContents;
                final FileContent newContents;
                try {
                    oldContents = this.loadFile(firstRevision);
                    newContents = this.loadFile(lastRevision);
                } catch (final Exception e) {
                    throw new ReviewtoolException(e);
                }

                final List<Pair<IFragment, IFragment>> relevantHunks =
                        DiffAlgorithmFactory.createDefault().determineDiff(
                                firstRevision,
                                oldContents.bytes,
                                lastRevision,
                                newContents.bytes,
                                newContents.charset);

                final List<Position> oldPositions = new ArrayList<>();
                final List<Position> newPositions = new ArrayList<>();
                final List<Position> oldLineRanges = new ArrayList<>();
                final List<Position> newLineRanges = new ArrayList<>();

                for (final Pair<IFragment, IFragment> hunk : relevantHunks) {
                    final IFragment sourceFragment = hunk.getFirst();
                    final IFragment targetFragment = hunk.getSecond();

                    oldPositions.add(fragmentToPosition(oldContents.lines, sourceFragment));
                    newPositions.add(fragmentToPosition(newContents.lines, targetFragment));
                    oldLineRanges.add(fragmentToLineRange(oldContents.lines, sourceFragment));
                    newLineRanges.add(fragmentToLineRange(newContents.lines, targetFragment));
                }

                final CompareConfiguration compareConfiguration = new CompareConfiguration();
                compareConfiguration.setLeftLabel(toLabel(firstRevision));
                compareConfiguration.setRightLabel(toLabel(lastRevision));
                final SelectableMergeViewer viewer;
                if (this.isJava(lastRevision)) {
                    viewer = new SelectableJavaMergeViewer(scrollContent, SWT.BORDER, compareConfiguration);
                } else {
                    viewer = new SelectableTextMergeViewer(scrollContent, SWT.BORDER, compareConfiguration);
                }
                viewer.setInput(new DiffNode(
                        this.createTextItem(firstRevision, oldContents.lines),
                        this.createTextItem(lastRevision, newContents.lines)));
                viewer.clearSelections();

                markAndReveal(viewer.getLeft(), oldPositions, oldLineRanges,
                        Constants.DIFF_BACKGROUND_OLD, new RGB(232, 153, 153));
                markAndReveal(viewer.getRight(), newPositions, newLineRanges,
                        Constants.DIFF_BACKGROUND_NEW, new RGB(148, 255, 157));
                for (final SourceViewer v : viewer.getViewers()) {
                    ViewHelper.createContextMenu(view, v.getTextWidget(), v);
                }
            }
        }
    }

    private TextItem createTextItem(final IRevisionedFile revision, final LineSequence contents) {
        return new TextItem(revision.getRevision().toString(),
                contents.getLinesConcatenated(0, contents.getNumberOfLines()),
                System.currentTimeMillis());
    }

    private static Position fragmentToPosition(final LineSequence contents, final IFragment fragment) {
        final int startOffset =
                contents.getStartPositionOfLine(fragment.getFrom().getLine() - 1)
                        + (fragment.getFrom().getColumn() - 1);
        final int endOffset =
                contents.getStartPositionOfLine(fragment.getTo().getLine() - 1)
                        + (fragment.getTo().getColumn() - 1);
        return new Position(startOffset, endOffset - startOffset);
    }

    private static Position fragmentToLineRange(final LineSequence contents, final IFragment fragment) {
        final int startOffset = contents.getStartPositionOfLine(fragment.getFrom().getLine() - 1);
        final int endOffset;
        if (fragment.getTo().getColumn() <= 1) {
            endOffset = contents.getStartPositionOfLine(fragment.getTo().getLine() - 1);
        } else {
            endOffset = contents.getStartPositionOfLine(fragment.getTo().getLine());
        }
        return new Position(startOffset, endOffset - startOffset);
    }

    private FileContent loadFile(IRevisionedFile revision) throws Exception {
        final byte[] data = revision.getContents();
        try {
            StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(data));
            return new FileContent(data, "UTF-8");
        } catch (final CharacterCodingException e) {
            return new FileContent(data, "ISO-8859-1");
        }
    }

    private static void markAndReveal(
            final SourceViewer viewer,
            final List<Position> ranges,
            final List<Position> lineRanges,
            String backgroundColorKey,
            RGB defaultBackgroundColor) {
        if (viewer == null) {
            return;
        }

        markBackground(viewer, lineRanges, backgroundColorKey, defaultBackgroundColor);
        mark(viewer, ranges);
        if (!ranges.isEmpty()) {
            reveal(viewer, ranges.iterator().next());
        }
    }

    private static void mark(final SourceViewer viewer, final List<Position> ranges) {
        final ChangeHighlighter listener = new ChangeHighlighter(ranges);
        viewer.addTextPresentationListener(listener);
        viewer.invalidateTextPresentation();
    }

    private static void markBackground(
            final SourceViewer viewer,
            final List<Position> ranges,
            String backgroundColorKey,
            RGB defaultBackgroundColor) {

        final BackgroundHighlighter listener =
                new BackgroundHighlighter(ranges, backgroundColorKey, defaultBackgroundColor);
        viewer.addTextPresentationListener(listener);
        viewer.invalidateTextPresentation();
    }

    /**
     * Makes the given range visible within the pane.
     */
    private static void reveal(final SourceViewer viewer, final Position range) {
        viewer.revealRange(range.getOffset(), range.getLength());
        final int top = viewer.getTopIndex();
        viewer.setTopIndex(top < CONTEXT_LENGTH ? 0 : top - CONTEXT_LENGTH);
    }

}
