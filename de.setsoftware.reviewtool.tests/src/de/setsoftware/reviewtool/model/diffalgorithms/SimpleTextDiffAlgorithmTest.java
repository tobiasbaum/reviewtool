package de.setsoftware.reviewtool.model.diffalgorithms;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import de.setsoftware.reviewtool.base.Pair;
import de.setsoftware.reviewtool.diffalgorithms.SimpleTextDiffAlgorithm;
import de.setsoftware.reviewtool.model.changestructure.PositionInText;

public class SimpleTextDiffAlgorithmTest {

    private static List<Pair<PositionInText, PositionInText>> determineDiff(String oldContent, String newContent)
            throws Exception {
        return new SimpleTextDiffAlgorithm().determineDiff(
                oldContent.getBytes("UTF-8"), newContent.getBytes("UTF-8"), "UTF-8");
    }

    private static Pair<PositionInText, PositionInText> insertedLines(int startIncl, int endIncl) {
        return changeIn(startIncl, endIncl);
    }

    private static Pair<PositionInText, PositionInText> changeIn(int startIncl, int endIncl) {
        return Pair.create(new PositionInText(startIncl, 1), new PositionInText(endIncl + 1, 0));
    }

    private static Pair<PositionInText, PositionInText> deletionAt(int line) {
        return changeIn(line, line - 1);
    }

    @Test
    public void testEqualEmptyContent() throws Exception {
        final List<Pair<PositionInText, PositionInText>> diff = determineDiff("", "");
        assertEquals(Collections.emptyList(), diff);
    }

    @Test
    public void testEqualContent() throws Exception {
        final String text = "package a;\r\n"
                + "\r\n"
                + "class X {\r\n"
                + "}\r\n";
        final List<Pair<PositionInText, PositionInText>> diff = determineDiff(text, text);
        assertEquals(Collections.emptyList(), diff);
    }

    @Test
    public void testFullInsertion() throws Exception {
        final List<Pair<PositionInText, PositionInText>> diff = determineDiff(
                "",
                "package a;\r\n"
                        + "\r\n"
                        + "class X {\r\n"
                        + "}\r\n");
        assertEquals(Arrays.asList(insertedLines(1, 4)), diff);
    }

    @Test
    public void testFullInsertionWithMultipleEqualLines() throws Exception {
        final List<Pair<PositionInText, PositionInText>> diff = determineDiff(
                "",
                "package a;\r\n"
                        + "\r\n"
                        + "class X {\r\n"
                        + "\r\n"
                        + "\r\n"
                        + "}\r\n");
        assertEquals(Arrays.asList(insertedLines(1, 6)), diff);
    }

    @Test
    public void testPartialInsertion() throws Exception {
        final List<Pair<PositionInText, PositionInText>> diff = determineDiff(
                "package a;\r\n"
                        + "\r\n"
                        + "class X {\r\n"
                        + "}\r\n",
                        "package a;\r\n"
                                + "\r\n"
                                + "class X {\r\n"
                                + "    private int x;\r\n"
                                + "}\r\n");
        assertEquals(Arrays.asList(insertedLines(4, 4)), diff);
    }

    @Test
    public void testSimpleChange() throws Exception {
        final List<Pair<PositionInText, PositionInText>> diff = determineDiff(
                "package a;\r\n"
                        + "\r\n"
                        + "class X {\r\n"
                        + "    private long y;\r\n"
                        + "}\r\n",
                        "package a;\r\n"
                                + "\r\n"
                                + "class X {\r\n"
                                + "    private int x;\r\n"
                                + "}\r\n");
        assertEquals(Arrays.asList(changeIn(4, 4)), diff);
    }

    @Test
    public void testTwoChanges() throws Exception {
        final List<Pair<PositionInText, PositionInText>> diff = determineDiff(
                "package a;\r\n"
                        + "\r\n"
                        + "class X {\r\n"
                        + "    private long x;\r\n"
                        + "    private long y;\r\n"
                        + "    private long z;\r\n"
                        + "}\r\n",
                        "package a;\r\n"
                                + "\r\n"
                                + "class X {\r\n"
                                + "    private int x;\r\n"
                                + "    private long y;\r\n"
                                + "    private int z;\r\n"
                                + "}\r\n");
        assertEquals(Arrays.asList(changeIn(4, 4), changeIn(6, 6)), diff);
    }

    @Test
    public void testAsymmetricChanges() throws Exception {
        final List<Pair<PositionInText, PositionInText>> diff = determineDiff(
                "package a;\r\n"
                        + "\r\n"
                        + "class X {\r\n"
                        + "    private long a1;\r\n"
                        + "    private long a2;\r\n"
                        + "    private long a3;\r\n"
                        + "    private long a4;\r\n"
                        + "}\r\n",
                        "package a;\r\n"
                                + "\r\n"
                                + "class X {\r\n"
                                + "    private int a1;\r\n"
                                + "    private long a2;\r\n"
                                + "    private int a3;\r\n"
                                + "    private int a4;\r\n"
                                + "}\r\n");
        assertEquals(Arrays.asList(changeIn(4, 4), changeIn(6, 7)), diff);
    }

    @Test
    public void testPartialDeletion() throws Exception {
        final List<Pair<PositionInText, PositionInText>> diff = determineDiff(
                "package a;\r\n"
                        + "\r\n"
                        + "class X {\r\n"
                        + "    private int x;\r\n"
                        + "}\r\n",
                        "package a;\r\n"
                                + "\r\n"
                                + "class X {\r\n"
                                + "}\r\n");
        assertEquals(Arrays.asList(deletionAt(4)), diff);
    }

}
