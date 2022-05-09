package de.setsoftware.reviewtool.ui.views;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
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
import org.eclipse.swt.custom.LineBackgroundEvent;
import org.eclipse.swt.custom.LineBackgroundListener;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IViewPart;

import de.setsoftware.reviewtool.base.LineSequence;
import de.setsoftware.reviewtool.base.Pair;
import de.setsoftware.reviewtool.base.PartialOrderAlgorithms;
import de.setsoftware.reviewtool.base.ReviewtoolException;
import de.setsoftware.reviewtool.diffalgorithms.DiffAlgorithmFactory;
import de.setsoftware.reviewtool.model.Constants;
import de.setsoftware.reviewtool.model.api.IFileHistoryEdge;
import de.setsoftware.reviewtool.model.api.IFileHistoryNode;
import de.setsoftware.reviewtool.model.api.IFragment;
import de.setsoftware.reviewtool.model.api.IHunk;
import de.setsoftware.reviewtool.model.api.IRevisionedFile;
import de.setsoftware.reviewtool.model.api.IStop;
import de.setsoftware.reviewtool.ui.api.IStopViewer;

/**
 * Displays all differences of a {@link IStop} combined in a single window.
 */
@SuppressWarnings("restriction")
public class CombinedDiffStopViewer implements IStopViewer {

    /**
     * Highlights a range by choosing a different color.
     */
    private static final class ChangeHighlighter implements ITextPresentationListener {

        private final List<Position> ranges;
        private Color hunkColor;

        /**
         * Constructor.
         *
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
         * Returns the color to use for the range.
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
    private static final class BackgroundHighlighter implements LineBackgroundListener {

        private final BitSet coloredLines;
        private final String colorKey;
        private final RGB defaultColor;
        private Color hunkColor;

        /**
         * Constructor.
         *
         * @param ranges The ranges to highlight.
         */
        BackgroundHighlighter(
                final List<Pair<Integer, Integer>> ranges, final String colorKey, final RGB defaultColor) {
            this.coloredLines = new BitSet();
            for (final Pair<Integer, Integer> range : ranges) {
                if (range.getSecond() > 0) {
                    this.coloredLines.set(range.getFirst(), range.getFirst() + range.getSecond());
                }
            }
            this.colorKey = colorKey;
            this.defaultColor = defaultColor;
        }

        @Override
        public void lineGetBackground(LineBackgroundEvent event) {
            final int lineNumber = ((StyledText) event.widget).getLineAtOffset(event.lineOffset);
            if (this.coloredLines.get(lineNumber)) {
                event.lineBackground = this.getTextColor();
            }
        }

        /**
         * Returns the color to use for the range.
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

        protected SourceViewer createSourceViewer(final SourceViewer superResult) {
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
            return this.getSelectableImpl().createSourceViewer(viewer);
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
            return this.getSelectableImpl().createSourceViewer(viewer);
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

        public FileContent(final byte[] data, final String charset) throws IOException {
            this.bytes = data;
            this.lines = new LineSequence(data, charset);
            this.charset = charset;
        }
    }

    /**
     * Helper class that wraps the highlighters for one side of the merge viewer.
     * We need to store them to be able to deregister them again.
     */
    private static final class Highlights {
        private final ChangeHighlighter rangeHighlights;
        private final BackgroundHighlighter lineHighlights;
        private final BackgroundHighlighter fillerHighlights;

        public Highlights(final List<Position> ranges,
                final List<Pair<Integer, Integer>> lineRanges,
                final List<Pair<Integer, Integer>> fillerLineRanges,
                final String backgroundColorKey,
                final RGB defaultBackgroundColor) {
            this.rangeHighlights = new ChangeHighlighter(ranges);
            this.lineHighlights = new BackgroundHighlighter(lineRanges, backgroundColorKey, defaultBackgroundColor);
            this.fillerHighlights = new BackgroundHighlighter(fillerLineRanges, "diffFiller", new RGB(100, 100, 100));
        }

        public void remove(final SourceViewer viewer) {
            viewer.removeTextPresentationListener(this.rangeHighlights);
            viewer.getTextWidget().removeLineBackgroundListener(this.lineHighlights);
            viewer.getTextWidget().removeLineBackgroundListener(this.fillerHighlights);
        }

        public void apply(final SourceViewer viewer) {
            viewer.getTextWidget().addLineBackgroundListener(this.fillerHighlights);
            viewer.getTextWidget().addLineBackgroundListener(this.lineHighlights);
            viewer.addTextPresentationListener(this.rangeHighlights);
            viewer.invalidateTextPresentation();
        }

    }

    private static final int CONTEXT_LENGTH = 3;

    private List<IRevisionedFile> allRevisions;
    private Combo comboLeft;
    private Combo comboRight;
    private SelectableMergeViewer viewer;

    private Highlights highlightsLeft;
    private Highlights highlightsRight;

    private void createBinaryHunkViewer(final IViewPart view, final Composite parent) {
        final Label label = new Label(parent, SWT.NULL);
        label.setText("binary");
        label.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT));
        ViewHelper.createContextMenuWithoutSelectionProvider(view, label);
    }

    private boolean isJava(final IRevisionedFile file) {
        return file.getPath().endsWith(".java");
    }

    private static String toLabel(final IRevisionedFile revision) {
        final String original = revision.toString();
        String shortened = original;
        while (shortened.length() > 100 && shortened.contains("/")) {
            final int index = shortened.indexOf('/');
            shortened = shortened.substring(index + 1);
        }
        return shortened.equals(original) ? shortened : (".../" + shortened);
    }

    @Override
    public void createStopView(final IViewPart view, final Composite scrollContent, final IStop stop) {
        this.allRevisions = this.determineAllRevisionsOfFile(stop);
        final Set<IRevisionedFile> revisionsForStop = new LinkedHashSet<>();
        revisionsForStop.addAll(stop.getHistory().keySet());
        revisionsForStop.addAll(stop.getHistory().values());
        final List<IRevisionedFile> sortedStopRevisions = PartialOrderAlgorithms.topoSort(revisionsForStop);
        final IRevisionedFile initialLeftRevision = sortedStopRevisions.get(0);
        final IRevisionedFile initialRightRevision = sortedStopRevisions.get(sortedStopRevisions.size() - 1);

        if (stop.isBinaryChange()) {
            this.createBinaryHunkViewer(view, scrollContent);
        } else {

            final SelectionListener fileChangedListener = new SelectionListener() {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    CombinedDiffStopViewer.this.refreshShownContents();
                }

                @Override
                public void widgetDefaultSelected(final SelectionEvent e) {
                    this.widgetSelected(e);
                }
            };

            this.comboLeft = new Combo(scrollContent, SWT.READ_ONLY);
            this.comboLeft.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
            this.addAll(this.comboLeft, this.allRevisions.subList(0, this.allRevisions.size() - 1));
            this.comboLeft.select(this.allRevisions.indexOf(initialLeftRevision));
            this.comboLeft.addSelectionListener(fileChangedListener);

            this.comboRight = new Combo(scrollContent, SWT.READ_ONLY);
            this.comboRight.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
            this.addAll(this.comboRight, this.allRevisions.subList(1, this.allRevisions.size()));
            this.comboRight.select(this.allRevisions.indexOf(initialRightRevision) - 1);
            this.comboRight.addSelectionListener(fileChangedListener);

            this.setTooltips(initialLeftRevision, initialRightRevision);

            final Composite viewerWrapper = new Composite(scrollContent, SWT.NONE);
            viewerWrapper.setLayout(new FillLayout());
            viewerWrapper.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
            final CompareConfiguration compareConfiguration = new CompareConfiguration();
            if (this.isJava(initialRightRevision)) {
                this.viewer = new SelectableJavaMergeViewer(viewerWrapper, SWT.BORDER, compareConfiguration);
            } else {
                this.viewer = new SelectableTextMergeViewer(viewerWrapper, SWT.BORDER, compareConfiguration);
            }

            this.initDiffViewerContent(initialLeftRevision, initialRightRevision);
            this.moveToLineForStop(stop, initialLeftRevision, initialRightRevision);
            for (final SourceViewer v : this.viewer.getViewers()) {
                ViewHelper.createContextMenu(view, v.getTextWidget(), v);
            }
        }
    }

    private void setTooltips(final IRevisionedFile leftFile, final IRevisionedFile rightFile) {
        this.comboLeft.setToolTipText(leftFile.toString());
        this.comboRight.setToolTipText(rightFile.toString());
    }

    private void initDiffViewerContent(final IRevisionedFile leftRevision, final IRevisionedFile rightRevision) {
        final FileContent oldContents;
        final FileContent newContents;
        try {
            oldContents = this.loadFile(leftRevision);
            newContents = this.loadFile(rightRevision);
        } catch (final Exception e) {
            throw new ReviewtoolException(e);
        }

        final List<Pair<IFragment, IFragment>> relevantHunks =
                DiffAlgorithmFactory.createDefault().determineDiff(
                        leftRevision,
                        oldContents.bytes,
                        rightRevision,
                        newContents.bytes,
                        newContents.charset);

        final List<Position> oldPositions = new ArrayList<>();
        final List<Position> newPositions = new ArrayList<>();
        final List<Pair<Integer, Integer>> oldLineRanges = new ArrayList<>();
        final List<Pair<Integer, Integer>> newLineRanges = new ArrayList<>();

        for (final Pair<IFragment, IFragment> hunk : relevantHunks) {
            final IFragment sourceFragment = hunk.getFirst();
            final IFragment targetFragment = hunk.getSecond();

            oldPositions.add(fragmentToPosition(oldContents.lines, sourceFragment));
            newPositions.add(fragmentToPosition(newContents.lines, targetFragment));
            oldLineRanges.add(fragmentToLineRange(oldContents.lines, sourceFragment));
            newLineRanges.add(fragmentToLineRange(newContents.lines, targetFragment));
        }

        final StringBuilder textOldAligned = new StringBuilder();
        final StringBuilder textNewAligned = new StringBuilder();
        final List<Pair<Integer, Integer>> adjustedLineRangesOld = new ArrayList<>();
        final List<Pair<Integer, Integer>> adjustedLineRangesNew = new ArrayList<>();
        final List<Pair<Integer, Integer>> fillerLineRangesOld = new ArrayList<>();
        final List<Pair<Integer, Integer>> fillerLineRangesNew = new ArrayList<>();
        final List<Position> adjustedPositionsOld = new ArrayList<>();
        final List<Position> adjustedPositionsNew = new ArrayList<>();
        determineAlignedTexts(
                oldContents.lines, oldLineRanges, oldPositions,
                newContents.lines, newLineRanges, newPositions,
                textOldAligned, adjustedLineRangesOld, fillerLineRangesOld, adjustedPositionsOld,
                textNewAligned, adjustedLineRangesNew, fillerLineRangesNew, adjustedPositionsNew);

        this.viewer.setInput(new DiffNode(
                this.createTextItem(leftRevision, textOldAligned.toString()),
                this.createTextItem(rightRevision, textNewAligned.toString())));
        this.viewer.clearSelections();

        this.highlightsLeft = mark(
                this.viewer.getLeft(), this.highlightsLeft, adjustedPositionsOld, adjustedLineRangesOld,
                fillerLineRangesOld, Constants.DIFF_BACKGROUND_OLD, new RGB(232, 153, 153));
        this.highlightsRight = mark(
                this.viewer.getRight(), this.highlightsRight, adjustedPositionsNew, adjustedLineRangesNew,
                fillerLineRangesNew, Constants.DIFF_BACKGROUND_NEW, new RGB(148, 255, 157));
    }

    static void determineAlignedTexts(
            LineSequence oldLines,
            List<Pair<Integer, Integer>> oldLineRanges,
            List<Position> oldPositions,
            LineSequence newLines,
            List<Pair<Integer, Integer>> newLineRanges,
            List<Position> newPositions,
            StringBuilder outputBufferOld,
            List<Pair<Integer, Integer>> adjustedLineRangesOld,
            List<Pair<Integer, Integer>> fillerRangesOld,
            List<Position> adjustedPositionsOld,
            StringBuilder outputBufferNew,
            List<Pair<Integer, Integer>> adjustedLineRangesNew,
            List<Pair<Integer, Integer>> fillerRangesNew,
            List<Position> adjustedPositionsNew) {

        assert oldLineRanges.size() == newLineRanges.size();
        assert oldPositions.size() == newPositions.size();
        assert oldPositions.size() == oldLineRanges.size();
        final Iterator<Pair<Integer, Integer>> iterOld = oldLineRanges.iterator();
        final Iterator<Pair<Integer, Integer>> iterNew = newLineRanges.iterator();
        final Iterator<Position> iterPosOld = oldPositions.iterator();
        final Iterator<Position> iterPosNew = newPositions.iterator();

        int curLineOld = 0;
        int curLineNew = 0;
        int adjustmentSumOld = 0;
        int adjustmentSumNew = 0;
        int offsetAdjustmentOld = 0;
        int offsetAdjustmentNew = 0;
        while (iterOld.hasNext()) {
            final Pair<Integer, Integer> curHunkOld = iterOld.next();
            final Pair<Integer, Integer> curHunkNew = iterNew.next();
            final Position curPosOld = iterPosOld.next();
            final Position curPosNew = iterPosNew.next();

            final int nextLineOld = curHunkOld.getFirst() + curHunkOld.getSecond();
            outputBufferOld.append(oldLines.getLinesConcatenated(curLineOld, curHunkOld.getFirst()));
            final String changedLinesOld = oldLines.getLinesConcatenated(curHunkOld.getFirst(), nextLineOld);
            final String changedLinesOldWithMarkers = addChangeMarkers(changedLinesOld);
            outputBufferOld.append(changedLinesOldWithMarkers);
            final int nextLineNew = curHunkNew.getFirst() + curHunkNew.getSecond();
            outputBufferNew.append(newLines.getLinesConcatenated(curLineNew, curHunkNew.getFirst()));
            final String changedLinesNew = newLines.getLinesConcatenated(curHunkNew.getFirst(), nextLineNew);
            final String changedLinesNewWithMarkers = addChangeMarkers(changedLinesNew);
            outputBufferNew.append(changedLinesNewWithMarkers);

            adjustedLineRangesOld.add(Pair.create(curHunkOld.getFirst() + adjustmentSumOld, curHunkOld.getSecond()));
            adjustedLineRangesNew.add(Pair.create(curHunkNew.getFirst() + adjustmentSumNew, curHunkNew.getSecond()));

            final int increaseOld = changedLinesOldWithMarkers.length() - changedLinesOld.length();
            adjustedPositionsOld.add(new Position(
                    curPosOld.getOffset() + offsetAdjustmentOld,
                    curPosOld.getLength() + increaseOld - (curPosOld.getLength() < changedLinesOld.length() ? 1 : 0)));
            final int increaseNew = changedLinesNewWithMarkers.length() - changedLinesNew.length();
            adjustedPositionsNew.add(new Position(
                    curPosNew.getOffset() + offsetAdjustmentNew,
                    curPosNew.getLength() + increaseNew - (curPosNew.getLength() < changedLinesNew.length() ? 1 : 0)));

            offsetAdjustmentOld += increaseOld;
            offsetAdjustmentNew += increaseNew;

            final int lenOld = curHunkOld.getSecond();
            final int lenNew = curHunkNew.getSecond();

            if (lenOld < lenNew) {
                final int len = lenNew - lenOld;
                addFillerLines(outputBufferOld, len);
                fillerRangesOld.add(Pair.create(nextLineOld + adjustmentSumOld, len));
                adjustmentSumOld += len;
                offsetAdjustmentOld += 2 * len;
            } else if (lenOld > lenNew) {
                final int len = lenOld - lenNew;
                addFillerLines(outputBufferNew, len);
                fillerRangesNew.add(Pair.create(nextLineNew + adjustmentSumNew, len));
                adjustmentSumNew += len;
                offsetAdjustmentNew += 2 * len;
            }

            curLineOld = nextLineOld;
            curLineNew = nextLineNew;
        }
        outputBufferOld.append(oldLines.getLinesConcatenated(curLineOld, oldLines.getNumberOfLines()));
        outputBufferNew.append(newLines.getLinesConcatenated(curLineNew, newLines.getNumberOfLines()));
    }

    private static String addChangeMarkers(String lines) {
        //The Eclipse diff editor does not allow the change of the diff algorithm. Therefore, its
        //  results sometimes differ from our results. To force it to use the same alignment, the lines
        //  that we consider changed are forced to be different from other lines outside the change by
        //  adding invisible characters that (hopefully) don't occur in normal text.
        return lines.replace("\n", "\u200B\n");
    }

    private static void addFillerLines(StringBuilder outputBuffer, int count) {
        for (int i = 0; i < count; i++) {
            outputBuffer.append((char) 0xA0).append('\n');
        }
    }

    private void moveToLineForStop(
            final IStop stop,
            final IRevisionedFile leftRevision,
            final IRevisionedFile rightRevision) {

        reveal(this.viewer.getLeft(), this.getLineFor(stop, leftRevision, false));
        reveal(this.viewer.getRight(), this.getLineFor(stop, rightRevision, true));
    }

    private int getLineFor(final IStop stop, final IRevisionedFile revision, final boolean right) {
        for (final IHunk hunk : stop.getContentFor(revision)) {
            return (right ? hunk.getTarget() : hunk.getSource()).getFrom().getLine();
        }
        return stop.getMostRecentFragment().getFrom().getLine();
    }

    private void refreshShownContents() {
        final int oldLineLeft = this.viewer.getLeft().getTopIndex();
        final int oldLineRight = this.viewer.getRight().getTopIndex();
        final IRevisionedFile fileLeft = this.allRevisions.get(this.comboLeft.getSelectionIndex());
        final IRevisionedFile fileRight = this.allRevisions.get(this.comboRight.getSelectionIndex() + 1);
        this.initDiffViewerContent(fileLeft, fileRight);
        this.setTooltips(fileLeft, fileRight);
        this.viewer.getLeft().setTopIndex(oldLineLeft);
        this.viewer.getRight().setTopIndex(oldLineRight);
    }

    private void addAll(final Combo combo, final List<? extends IRevisionedFile> subList) {
        for (final IRevisionedFile file : subList) {
            combo.add(toLabel(file));
        }
    }

    private List<IRevisionedFile> determineAllRevisionsOfFile(final IStop stop) {
        final IRevisionedFile lastRevision = stop.getOriginalMostRecentFile();
        final IFileHistoryNode node = stop.getWorkingCopy().getFileHistoryGraph().getNodeFor(lastRevision);
        final LinkedHashSet<IRevisionedFile> filesBuffer = new LinkedHashSet<>();
        this.determineAllRevisionsOfFileRec(node, filesBuffer);
        return PartialOrderAlgorithms.topoSort(filesBuffer);
    }

    private void determineAllRevisionsOfFileRec(final IFileHistoryNode node, final Set<IRevisionedFile> buffer) {
        if (buffer.contains(node.getFile())) {
            return;
        }
        buffer.add(node.getFile());
        for (final IFileHistoryEdge edge : node.getAncestors()) {
            this.determineAllRevisionsOfFileRec(edge.getAncestor(), buffer);
        }
    }

    private TextItem createTextItem(final IRevisionedFile revision, String content) {
        return new TextItem(revision.getRevision().toString(),
                content,
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

    private static Pair<Integer, Integer> fragmentToLineRange(final LineSequence contents, final IFragment fragment) {
        if (fragment.getTo().getColumn() > 1) {
            return Pair.create(fragment.getFrom().getLine() - 1, fragment.getNumberOfLines() + 1);
        } else {
            return Pair.create(fragment.getFrom().getLine() - 1, fragment.getNumberOfLines());
        }
    }

    private FileContent loadFile(final IRevisionedFile revision) throws Exception {
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

    private static Highlights mark(
            final SourceViewer viewer,
            final Highlights oldHighlights,
            final List<Position> ranges,
            final List<Pair<Integer, Integer>> lineRanges,
            final List<Pair<Integer, Integer>> fillerLineRanges,
            final String backgroundColorKey,
            final RGB defaultBackgroundColor) {
        if (viewer == null) {
            return oldHighlights;
        }

        if (oldHighlights != null) {
            oldHighlights.remove(viewer);
        }
        final Highlights newHighlights =
                new Highlights(ranges, lineRanges, fillerLineRanges, backgroundColorKey, defaultBackgroundColor);
        newHighlights.apply(viewer);
        return newHighlights;
    }

    /**
     * Makes the line visible in the given viewer and includes context lines above if possible.
     */
    private static void reveal(final SourceViewer viewer, final int line) {
        viewer.setTopIndex(line < CONTEXT_LENGTH ? 0 : line - CONTEXT_LENGTH);
    }

}
