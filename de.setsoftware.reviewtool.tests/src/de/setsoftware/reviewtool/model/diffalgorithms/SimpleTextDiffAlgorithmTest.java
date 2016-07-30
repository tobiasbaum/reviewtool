package de.setsoftware.reviewtool.model.diffalgorithms;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import de.setsoftware.reviewtool.base.Pair;
import de.setsoftware.reviewtool.diffalgorithms.SimpleSourceDiffAlgorithm;
import de.setsoftware.reviewtool.model.changestructure.Fragment;
import de.setsoftware.reviewtool.model.changestructure.PositionInText;

public class SimpleTextDiffAlgorithmTest {

    private static List<Pair<PositionInText, PositionInText>> determineDiff(String oldContent, String newContent)
            throws Exception {
        return toPositionsInNewFile(new SimpleSourceDiffAlgorithm().determineDiff(
                null, oldContent.getBytes("UTF-8"), null, newContent.getBytes("UTF-8"), "UTF-8"));
    }

    private static List<Pair<PositionInText, PositionInText>> toPositionsInNewFile(
            List<Pair<Fragment, Fragment>> diff) {
        final List<Pair<PositionInText, PositionInText>> ret = new ArrayList<>();
        for (final Pair<Fragment, Fragment> p : diff) {
            ret.add(Pair.create(p.getSecond().getFrom(), p.getSecond().getTo()));
        }
        return ret;
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
    public void testFullInsertionMissingEolAtEof() throws Exception {
        final List<Pair<PositionInText, PositionInText>> diff = determineDiff(
                "",
                "package a;\r\n"
                        + "\r\n"
                        + "class X {\r\n"
                        + "}");
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

    @Test
    public void testChangeInRepeatedLines() throws Exception {
        final List<Pair<PositionInText, PositionInText>> diff = determineDiff(
                "aaa\r\n"
                        + "bbb\r\n"
                        + "aaa\r\n"
                        + "bbb\r\n"
                        + "aaa\r\n"
                        + "bbb\r\n"
                        + "aaa\r\n"
                        + "bbb\r\n",
                        "aaa\r\n"
                                + "bbb\r\n"
                                + "AAA\r\n"
                                + "BBB\r\n"
                                + "AAA\r\n"
                                + "BBB\r\n"
                                + "aaa\r\n"
                                + "bbb\r\n");
        assertEquals(Arrays.asList(changeIn(3, 6)), diff);
    }

    @Test
    public void testDeletionInRepeatedLines() throws Exception {
        final List<Pair<PositionInText, PositionInText>> diff = determineDiff(
                "aaa\r\n"
                        + "bbb\r\n"
                        + "aaa\r\n"
                        + "bbb\r\n"
                        + "ccc\r\n"
                        + "aaa\r\n"
                        + "bbb\r\n"
                        + "ccc\r\n"
                        + "aaa\r\n"
                        + "bbb\r\n",
                        "aaa\r\n"
                                + "bbb\r\n"
                                + "aaa\r\n"
                                + "bbb\r\n"
                                + "aaa\r\n"
                                + "bbb\r\n"
                                + "aaa\r\n"
                                + "bbb\r\n");
        assertEquals(Arrays.asList(deletionAt(5), deletionAt(7)), diff);
    }

    @Test
    public void testDeleteEmptyLines() throws Exception {
        final List<Pair<PositionInText, PositionInText>> diff = determineDiff(
                "package a;\r\n"
                        + "\r\n"
                        + "class X {\r\n"
                        + "\r\n"
                        + "    private long a1;\r\n"
                        + "\r\n"
                        + "    private long a2;\r\n"
                        + "\r\n"
                        + "    private long a3;\r\n"
                        + "\r\n"
                        + "    private long a4;\r\n"
                        + "\r\n"
                        + "}\r\n",
                        "package a;\r\n"
                                + "\r\n"
                                + "class X {\r\n"
                                + "\r\n"
                                + "    private long a1;\r\n"
                                + "    private long a2;\r\n"
                                + "    private long a3;\r\n"
                                + "    private long a4;\r\n"
                                + "\r\n"
                                + "}\r\n");
        assertEquals(
                Arrays.asList(deletionAt(6), deletionAt(7), deletionAt(8)),
                diff);
    }

    @Test
    public void testAddEmptyLines() throws Exception {
        final List<Pair<PositionInText, PositionInText>> diff = determineDiff(
                "package a;\r\n"
                        + "\r\n"
                        + "class X {\r\n"
                        + "\r\n"
                        + "    private long a1;\r\n"
                        + "    private long a2;\r\n"
                        + "    private long a3;\r\n"
                        + "    private long a4;\r\n"
                        + "\r\n"
                        + "}\r\n",
                        "package a;\r\n"
                                + "\r\n"
                                + "class X {\r\n"
                                + "\r\n"
                                + "    private long a1;\r\n"
                                + "\r\n"
                                + "    private long a2;\r\n"
                                + "\r\n"
                                + "    private long a3;\r\n"
                                + "\r\n"
                                + "    private long a4;\r\n"
                                + "\r\n"
                                + "}\r\n");
        assertEquals(
                Arrays.asList(insertedLines(6, 6),
                        insertedLines(8, 8),
                        insertedLines(10, 10)),
                diff);
    }

    @Test
    public void testAddStuffWithEmptyLine() throws Exception {
        final List<Pair<PositionInText, PositionInText>> diff = determineDiff(
                "package a;\r\n"
                        + "\r\n"
                        + "class X {\r\n"
                        + "\r\n"
                        + "    private long a1;\r\n"
                        + "    private long a2;\r\n"
                        + "\r\n"
                        + "}\r\n",
                        "package a;\r\n"
                                + "\r\n"
                                + "class X {\r\n"
                                + "\r\n"
                                + "    private long a1;\r\n"
                                + "    private long a2;\r\n"
                                + "    private long a3;\r\n"
                                + "    private long a4;\r\n"
                                + "\r\n"
                                + "\r\n"
                                + "}\r\n");
        assertEquals(
                Arrays.asList(insertedLines(7, 9)),
                diff);
    }

    @Test
    public void testWhenInDoubtMarkAdditionRangeAtMethodBoundaries() throws Exception {
        final List<Pair<PositionInText, PositionInText>> diff = determineDiff(
                "package text.xyz.abc;\r\n" +
                "\r\n" +
                "import java.util.List;\r\n" +
                "import java.util.ArrayList;\r\n" +
                "\r\n" +
                "public class Testklasse {\r\n" +
                "\r\n" +
                "    /**\r\n" +
                "     * Methode 1.\r\n" +
                "     */\r\n" +
                "    public void method1() {\r\n" +
                "    }\r\n" +
                "\r\n" +
                "    /**\r\n" +
                "     * Methode 2.\r\n" +
                "     */\r\n" +
                "    public List<String> method2() {\r\n" +
                "        return new ArrayList<>();\r\n" +
                "    }\r\n" +
                "}\r\n" +
                "",
                        "package text.xyz.abc;\r\n" +
                        "\r\n" +
                        "import java.util.List;\r\n" +
                        "import java.util.ArrayList;\r\n" +
                        "\r\n" +
                        "public class Testklasse {\r\n" +
                        "\r\n" +
                        "    /**\r\n" +
                        "     * Methode 1.\r\n" +
                        "     */\r\n" +
                        "    public void method1() {\r\n" +
                        "    }\r\n" +
                        "\r\n" +
                        "    /**\r\n" +
                        "     * Methode 1 1/2.\r\n" +
                        "     */\r\n" +
                        "    public void methodOneAndAHalf() {\r\n" +
                        "    }\r\n" +
                        "\r\n" +
                        "    /**\r\n" +
                        "     * Methode 2.\r\n" +
                        "     */\r\n" +
                        "    public List<String> method2() {\r\n" +
                        "        return new ArrayList<>();\r\n" +
                        "    }\r\n" +
                        "}\r\n" +
                        "");
        assertEquals(Arrays.asList(insertedLines(14, 19)), diff);
    }


    @Test
    public void testWhenInDoubtMarkAdditionRangeAtMethodBoundariesWithDeletion() throws Exception {
        final List<Pair<PositionInText, PositionInText>> diff = determineDiff(
                "package text.xyz.abc;\r\n" +
                "\r\n" +
                "import java.util.List;\r\n" +
                "import java.util.ArrayList;\r\n" +
                "\r\n" +
                "public class Testklasse {\r\n" +
                "\r\n" +
                "    /**\r\n" +
                "     * Methode 1.\r\n" +
                "     */\r\n" +
                "    public void method1() {\r\n" +
                "    }\r\n" +
                "\r\n" +
                "    /**\r\n" +
                "     * Methode 1 1/2.\r\n" +
                "     */\r\n" +
                "    public void methodOneAndAHalf() {\r\n" +
                "    }\r\n" +
                "\r\n" +
                "    /**\r\n" +
                "     * Methode 2.\r\n" +
                "     */\r\n" +
                "    public List<String> method2() {\r\n" +
                "        return new ArrayList<>();\r\n" +
                "    }\r\n" +
                "}\r\n" +
                "",
                        "package text.xyz.abc;\r\n" +
                        "\r\n" +
                        "import java.util.List;\r\n" +
                        "import java.util.ArrayList;\r\n" +
                        "\r\n" +
                        "public class Testklasse {\r\n" +
                        "\r\n" +
                        "    /**\r\n" +
                        "     * Methode 1.\r\n" +
                        "     */\r\n" +
                        "    public void method1() {\r\n" +
                        "    }\r\n" +
                        "\r\n" +
                        "    /**\r\n" +
                        "     * Methode 2.\r\n" +
                        "     */\r\n" +
                        "    public List<String> method2() {\r\n" +
                        "        return new ArrayList<>();\r\n" +
                        "    }\r\n" +
                        "}\r\n" +
                        "");
        assertEquals(Arrays.asList(deletionAt(14)), diff);
    }

    @Test
    public void testWhenInDoubtMarkAdditionRangeAtMethodBoundariesWithMultipleInserts() throws Exception {
        final List<Pair<PositionInText, PositionInText>> diff = determineDiff(
                "package text.xyz.abc;\r\n" +
                "\r\n" +
                "import java.util.List;\r\n" +
                "import java.util.ArrayList;\r\n" +
                "\r\n" +
                "public class Testklasse {\r\n" +
                "\r\n" +
                "    /**\r\n" +
                "     * Methode 1.\r\n" +
                "     */\r\n" +
                "    public void method1() {\r\n" +
                "    }\r\n" +
                "\r\n" +
                "    /**\r\n" +
                "     * Methode 2.\r\n" +
                "     */\r\n" +
                "    public List<String> method2() {\r\n" +
                "        return new ArrayList<>();\r\n" +
                "    }\r\n" +
                "\r\n" +
                "    /**\r\n" +
                "     * Methode 3.\r\n" +
                "     */\r\n" +
                "    public List<String> method3() {\r\n" +
                "        return new ArrayList<>();\r\n" +
                "    }\r\n" +
                "}\r\n" +
                "",
                        "package text.xyz.abc;\r\n" +
                        "\r\n" +
                        "import java.util.List;\r\n" +
                        "import java.util.ArrayList;\r\n" +
                        "\r\n" +
                        "public class Testklasse {\r\n" +
                        "\r\n" +
                        "    /**\r\n" +
                        "     * Methode 1.\r\n" +
                        "     */\r\n" +
                        "    public void method1() {\r\n" +
                        "    }\r\n" +
                        "\r\n" +
                        "    /**\r\n" +
                        "     * Methode A.\r\n" +
                        "     */\r\n" +
                        "    public void methodA() {\r\n" +
                        "    }\r\n" +
                        "\r\n" +
                        "    /**\r\n" +
                        "     * Methode 2.\r\n" +
                        "     */\r\n" +
                        "    public List<String> method2() {\r\n" +
                        "        return new ArrayList<>();\r\n" +
                        "    }\r\n" +
                        "\r\n" +
                        "    /**\r\n" +
                        "     * Methode B.\r\n" +
                        "     */\r\n" +
                        "    public void methodB() {\r\n" +
                        "    }\r\n" +
                        "\r\n" +
                        "    /**\r\n" +
                        "     * Methode 3.\r\n" +
                        "     */\r\n" +
                        "    public List<String> method3() {\r\n" +
                        "        return new ArrayList<>();\r\n" +
                        "    }\r\n" +
                        "}\r\n" +
                        "");
        assertEquals(Arrays.asList(insertedLines(14, 19), insertedLines(27, 32)), diff);
    }

    @Test
    public void testNoExceptions() throws Exception {
        for (int i = 0; i < 1000; i++) {
            this.doTestNoExceptions(i);
        }
    }

    public void doTestNoExceptions(int seed) throws Exception {
        try {
            final Random r = new Random(seed);
            final List<String> randomLines = createRandomLines(r);
            final List<String> changedLines = createRandomChangedLines(randomLines, r);

            final String doc1 = concatLines(randomLines);
            final String doc2 = concatLines(changedLines);
            assertEquals(Collections.emptyList(), determineDiff(doc1, doc1));
            assertEquals(Collections.emptyList(), determineDiff(doc2, doc2));
            //pure smoke test: just check that no exception occurs
            determineDiff(doc1, doc2);
        } catch (final Exception e) {
            throw new Exception("problem with seed " + seed, e);
        } catch (final AssertionError e) {
            throw new AssertionError("problem with seed " + seed, e);
        }
    }

    private static final String createRandomLine(int value) {
        switch (Math.abs(value % 10)) {
        case 0:
            return "";
        case 1:
            return "}";
        case 2:
            return "public static method() {";
        case 3:
            return "return 'a';";
        case 4:
            return String.format("this.callA%d();", value % 23);
        case 5:
            return String.format("this.callB%d();", value % 67);
        case 6:
            return String.format("this.callC%d();", value % 19);
        case 7:
            return String.format("x = this.callD%d();", value % 41);
        case 8:
            return String.format("return %d;", value);
        case 9:
            return String.format("return \"%d\";", value);
        default:
            throw new AssertionError();
        }
    }

    private static List<String> createRandomLines(Random r) {
        final int len = r.nextInt(300);

        final List<String> ret = new ArrayList<>();
        for (int i = 0; i < len; i++) {
            ret.add(createRandomLine(r.nextInt()));
        }
        return ret;
    }

    private static List<String> createRandomChangedLines(List<String> randomLines, Random r) {
        final List<String> ret = new ArrayList<>(randomLines);
        final int changeCount = r.nextInt(5) + 1;
        for (int i = 0; i < changeCount; i++) {
            final int changeType = r.nextInt(3);
            switch (changeType) {
            case 0:
                final int insertPos = r.nextInt(ret.size() + 1);
                final int insertCount = r.nextInt(10);
                for (int j = 0; j < insertCount; j++) {
                    ret.add(insertPos, createRandomLine(r.nextInt()));
                }
                break;
            case 1:
                final int deleteCount = Math.min(r.nextInt(10), ret.size());
                for (int j = 0; j < deleteCount; j++) {
                    ret.remove(r.nextInt(ret.size()));
                }
                break;
            case 2:
                final int replaceCount = Math.min(r.nextInt(5), ret.size());
                for (int j = 0; j < replaceCount; j++) {
                    ret.set(r.nextInt(ret.size()), createRandomLine(r.nextInt()));
                }
                break;
            default:
                throw new AssertionError();
            }
        }
        return ret;
    }

    private static String concatLines(List<String> lines) {
        final StringBuilder ret = new StringBuilder();
        for (final String line : lines) {
            ret.append(line).append("\r\n");
        }
        return ret.toString();
    }

}
