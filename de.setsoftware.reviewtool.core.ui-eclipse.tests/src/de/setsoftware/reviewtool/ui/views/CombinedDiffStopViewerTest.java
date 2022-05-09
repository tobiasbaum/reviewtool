package de.setsoftware.reviewtool.ui.views;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.text.Position;
import org.junit.Test;

import de.setsoftware.reviewtool.base.LineSequence;
import de.setsoftware.reviewtool.base.Pair;

public class CombinedDiffStopViewerTest {

    private static final String NB = "\u00A0";
    private static final String CM = "\u200B";

    @Test
    public void testDetermineAlignedTexts1() throws Exception {
        doTest(
            "a\n"
            + "b\n"
            + "c\n",
            "a\n"
            + "b\n"
            + "c\n",
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            "a\n"
            + "b\n"
            + "c\n",
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            "a\n"
            + "b\n"
            + "c\n",
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList());
    }

    @Test
    public void testDetermineAlignedTexts2() throws Exception {
        doTest(
            "a\n"
            + "b\n"
            + "c\n",
            "a\n"
            + "B\n"
            + "c\n",
            Arrays.asList(Pair.create(1, 1)),
            Arrays.asList(Pair.create(1, 1)),
            Arrays.asList(new Position(2, 1)),
            Arrays.asList(new Position(2, 1)),
            "a\n"
            + "b" + CM + "\n"
            + "c\n",
            Arrays.asList(Pair.create(1, 1)),
            Collections.emptyList(),
            Arrays.asList(new Position(2, 1)),
            "a\n"
            + "B" + CM + "\n"
            + "c\n",
            Arrays.asList(Pair.create(1, 1)),
            Collections.emptyList(),
            Arrays.asList(new Position(2, 1)));
    }

    @Test
    public void testDetermineAlignedTexts3() throws Exception {
        doTest(
            "a\n"
            + "b\n"
            + "c\n",
            "a\n"
            + "c\n",
            Arrays.asList(Pair.create(1, 1)),
            Arrays.asList(Pair.create(1, 0)),
            Arrays.asList(new Position(2, 2)),
            Arrays.asList(new Position(2, 0)),
            "a\n"
            + "b" + CM + "\n"
            + "c\n",
            Arrays.asList(Pair.create(1, 1)),
            Collections.emptyList(),
            Arrays.asList(new Position(2, 3)),
            "a\n"
            + NB + "\n"
            + "c\n",
            Arrays.asList(Pair.create(1, 0)),
            Arrays.asList(Pair.create(1, 1)),
            Arrays.asList(new Position(2, 0)));
    }

    @Test
    public void testDetermineAlignedTexts4() throws Exception {
        doTest(
            "a\n"
            + "c\n",
            "a\n"
            + "b\n"
            + "c\n",
            Arrays.asList(Pair.create(1, 0)),
            Arrays.asList(Pair.create(1, 1)),
            Arrays.asList(new Position(2, 0)),
            Arrays.asList(new Position(2, 2)),
            "a\n"
            + NB + "\n"
            + "c\n",
            Arrays.asList(Pair.create(1, 0)),
            Arrays.asList(Pair.create(1, 1)),
            Arrays.asList(new Position(2, 0)),
            "a\n"
            + "b" + CM + "\n"
            + "c\n",
            Arrays.asList(Pair.create(1, 1)),
            Collections.emptyList(),
            Arrays.asList(new Position(2, 3)));
    }

    @Test
    public void testDetermineAlignedTexts5() throws Exception {
        doTest(
            "a\n"
            + "d\n"
            + "e\n",
            "a\n"
            + "b\n"
            + "c\n"
            + "d\n",
            Arrays.asList(Pair.create(1, 0), Pair.create(2, 1)),
            Arrays.asList(Pair.create(1, 2), Pair.create(4, 0)),
            Arrays.asList(new Position(2, 0), new Position(4, 2)),
            Arrays.asList(new Position(2, 4), new Position(8, 0)),
            "a\n"
            + NB + "\n"
            + NB + "\n"
            + "d\n"
            + "e" + CM + "\n",
            Arrays.asList(Pair.create(1, 0), Pair.create(4, 1)),
            Arrays.asList(Pair.create(1, 2)),
            Arrays.asList(new Position(2, 0), new Position(8, 3)),
            "a\n"
            + "b" + CM + "\n"
            + "c" + CM + "\n"
            + "d\n"
            + NB + "\n",
            Arrays.asList(Pair.create(1, 2), Pair.create(4, 0)),
            Arrays.asList(Pair.create(4, 1)),
            Arrays.asList(new Position(2, 6), new Position(10, 0)));
    }

    private static void doTest(
            String oldText,
            String newText,
            List<Pair<Integer, Integer>> oldRanges,
            List<Pair<Integer, Integer>> newRanges,
            List<Position> oldPositions,
            List<Position> newPositions,
            String expectedOld,
            List<Pair<Integer, Integer>> expectedAdjustedLineRangesOld,
            List<Pair<Integer, Integer>> expectedFillerLineRangesOld,
            List<Position> expectedPositionsOld,
            String expectedNew,
            List<Pair<Integer, Integer>> expectedAdjustedLineRangesNew,
            List<Pair<Integer, Integer>> expectedFillerLineRangesNew,
            List<Position> expectedPositionsNew) throws Exception {

        final StringBuilder actualOld = new StringBuilder();
        final StringBuilder actualNew = new StringBuilder();
        final List<Pair<Integer, Integer>> adjustedLineRangesOld = new ArrayList<>();
        final List<Pair<Integer, Integer>> adjustedLineRangesNew = new ArrayList<>();
        final List<Pair<Integer, Integer>> fillerLineRangesOld = new ArrayList<>();
        final List<Pair<Integer, Integer>> fillerLineRangesNew = new ArrayList<>();
        final List<Position> adjustedPositionsOld = new ArrayList<>();
        final List<Position> adjustedPositionsNew = new ArrayList<>();
        CombinedDiffStopViewer.determineAlignedTexts(
                new LineSequence(oldText.getBytes("UTF-8"), "UTF-8"),
                oldRanges,
                oldPositions,
                new LineSequence(newText.getBytes("UTF-8"), "UTF-8"),
                newRanges,
                newPositions,
                actualOld,
                adjustedLineRangesOld,
                fillerLineRangesOld,
                adjustedPositionsOld,
                actualNew,
                adjustedLineRangesNew,
                fillerLineRangesNew,
                adjustedPositionsNew);

        assertEquals(expectedOld, actualOld.toString());
        assertEquals(expectedNew, actualNew.toString());
        assertEquals(expectedAdjustedLineRangesOld, adjustedLineRangesOld);
        assertEquals(expectedAdjustedLineRangesNew, adjustedLineRangesNew);
        assertEquals(expectedFillerLineRangesOld, fillerLineRangesOld);
        assertEquals(expectedFillerLineRangesNew, fillerLineRangesNew);
        assertEquals(expectedPositionsOld, adjustedPositionsOld);
        assertEquals(expectedPositionsNew, adjustedPositionsNew);
    }

}
