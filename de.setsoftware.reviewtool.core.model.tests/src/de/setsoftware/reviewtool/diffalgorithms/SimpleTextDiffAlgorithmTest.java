package de.setsoftware.reviewtool.diffalgorithms;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import de.setsoftware.reviewtool.base.Pair;
import de.setsoftware.reviewtool.model.api.IFragment;
import de.setsoftware.reviewtool.model.api.IPositionInText;
import de.setsoftware.reviewtool.model.changestructure.ChangestructureFactory;

/**
 * Tests for {@link SimpleSourceDiffAlgorithm}.
 */
public class SimpleTextDiffAlgorithmTest {

    private static List<Pair<IPositionInText, IPositionInText>> determineDiff(String oldContent, String newContent)
            throws Exception {
        return toPositionsInNewFile(new MyersSourceDiffAlgorithm().determineDiff(
                ChangestructureFactory.createFileInRevision("", null),
                oldContent.getBytes("UTF-8"),
                ChangestructureFactory.createFileInRevision("", null),
                newContent.getBytes("UTF-8"),
                "UTF-8"));
//        return toPositionsInNewFile(diffUtilsDiff(
//                ChangestructureFactory.createFileInRevision("", null, null),
//                oldContent.getBytes("UTF-8"),
//                ChangestructureFactory.createFileInRevision("", null, null),
//                newContent.getBytes("UTF-8"),
//                "UTF-8"));
    }

    private static List<Pair<IPositionInText, IPositionInText>> toPositionsInNewFile(
            List<Pair<IFragment, IFragment>> diff) {
        final List<Pair<IPositionInText, IPositionInText>> ret = new ArrayList<>();
        for (final Pair<IFragment, IFragment> p : diff) {
            ret.add(Pair.create(p.getSecond().getFrom(), p.getSecond().getTo()));
        }
        return ret;
    }

    private static Pair<IPositionInText, IPositionInText> insertedLines(int startIncl, int endIncl) {
        return changeIn(startIncl, endIncl);
    }

    private static Pair<IPositionInText, IPositionInText> changeIn(int startIncl, int endIncl) {
        return Pair.create(
                ChangestructureFactory.createPositionInText(startIncl, 1),
                ChangestructureFactory.createPositionInText(endIncl + 1, 1));
    }

    private static Pair<IPositionInText, IPositionInText> inLineChange(int line, int startCharIncl, int endCharExcl) {
        return Pair.create(
                ChangestructureFactory.createPositionInText(line, startCharIncl),
                ChangestructureFactory.createPositionInText(line, endCharExcl));
    }

    private static Pair<IPositionInText, IPositionInText> deletionAt(int line) {
        return changeIn(line, line - 1);
    }

    @Test
    public void testEqualEmptyContent() throws Exception {
        final List<Pair<IPositionInText, IPositionInText>> diff = determineDiff("", "");
        assertEquals(Collections.emptyList(), diff);
    }

    @Test
    public void testEqualContent() throws Exception {
        final String text = "package a;\r\n"
                + "\r\n"
                + "class X {\r\n"
                + "}\r\n";
        final List<Pair<IPositionInText, IPositionInText>> diff = determineDiff(text, text);
        assertEquals(Collections.emptyList(), diff);
    }

    @Test
    public void testFullInsertion() throws Exception {
        final List<Pair<IPositionInText, IPositionInText>> diff = determineDiff(
                "",
                "package a;\r\n"
                        + "\r\n"
                        + "class X {\r\n"
                        + "}\r\n");
        assertEquals(Arrays.asList(insertedLines(1, 4)), diff);
    }

    @Test
    public void testFullInsertionMissingEolAtEof() throws Exception {
        final List<Pair<IPositionInText, IPositionInText>> diff = determineDiff(
                "",
                "package a;\r\n"
                        + "\r\n"
                        + "class X {\r\n"
                        + "}");
        assertEquals(Arrays.asList(insertedLines(1, 4)), diff);
    }

    @Test
    public void testFullInsertionWithMultipleEqualLines() throws Exception {
        final List<Pair<IPositionInText, IPositionInText>> diff = determineDiff(
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
        final List<Pair<IPositionInText, IPositionInText>> diff = determineDiff(
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
        final List<Pair<IPositionInText, IPositionInText>> diff = determineDiff(
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
        assertEquals(Arrays.asList(inLineChange(4, 13, 18)), diff);
    }

    @Test
    public void testTwoChanges() throws Exception {
        final List<Pair<IPositionInText, IPositionInText>> diff = determineDiff(
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
        assertEquals(Arrays.asList(inLineChange(4, 13, 16), inLineChange(6, 13, 16)), diff);
    }

    @Test
    public void testAsymmetricChanges() throws Exception {
        final List<Pair<IPositionInText, IPositionInText>> diff = determineDiff(
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
        assertEquals(Arrays.asList(inLineChange(4, 13, 16), changeIn(6, 7)), diff);
    }

    @Test
    public void testPartialDeletion() throws Exception {
        final List<Pair<IPositionInText, IPositionInText>> diff = determineDiff(
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
        final List<Pair<IPositionInText, IPositionInText>> diff = determineDiff(
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
        final List<Pair<IPositionInText, IPositionInText>> diff = determineDiff(
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
        final List<Pair<IPositionInText, IPositionInText>> diff = determineDiff(
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
        final List<Pair<IPositionInText, IPositionInText>> diff = determineDiff(
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
        final List<Pair<IPositionInText, IPositionInText>> diff = determineDiff(
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
        final List<Pair<IPositionInText, IPositionInText>> diff = determineDiff(
                "package text.xyz.abc;\r\n"
                + "\r\n"
                + "import java.util.List;\r\n"
                + "import java.util.ArrayList;\r\n"
                + "\r\n"
                + "public class Testklasse {\r\n"
                + "\r\n"
                + "    /**\r\n"
                + "     * Methode 1.\r\n"
                + "     */\r\n"
                + "    public void method1() {\r\n"
                + "    }\r\n"
                + "\r\n"
                + "    /**\r\n"
                + "     * Methode 2.\r\n"
                + "     */\r\n"
                + "    public List<String> method2() {\r\n"
                + "        return new ArrayList<>();\r\n"
                + "    }\r\n"
                + "}\r\n"
                + "",
                        "package text.xyz.abc;\r\n"
                        + "\r\n"
                        + "import java.util.List;\r\n"
                        + "import java.util.ArrayList;\r\n"
                        + "\r\n"
                        + "public class Testklasse {\r\n"
                        + "\r\n"
                        + "    /**\r\n"
                        + "     * Methode 1.\r\n"
                        + "     */\r\n"
                        + "    public void method1() {\r\n"
                        + "    }\r\n"
                        + "\r\n"
                        + "    /**\r\n"
                        + "     * Methode 1 1/2.\r\n"
                        + "     */\r\n"
                        + "    public void methodOneAndAHalf() {\r\n"
                        + "    }\r\n"
                        + "\r\n"
                        + "    /**\r\n"
                        + "     * Methode 2.\r\n"
                        + "     */\r\n"
                        + "    public List<String> method2() {\r\n"
                        + "        return new ArrayList<>();\r\n"
                        + "    }\r\n"
                        + "}\r\n"
                        + "");
        assertEquals(Arrays.asList(insertedLines(14, 19)), diff);
    }


    @Test
    public void testWhenInDoubtMarkAdditionRangeAtMethodBoundariesWithDeletion() throws Exception {
        final List<Pair<IPositionInText, IPositionInText>> diff = determineDiff(
                "package text.xyz.abc;\r\n"
                + "\r\n"
                + "import java.util.List;\r\n"
                + "import java.util.ArrayList;\r\n"
                + "\r\n"
                + "public class Testklasse {\r\n"
                + "\r\n"
                + "    /**\r\n"
                + "     * Methode 1.\r\n"
                + "     */\r\n"
                + "    public void method1() {\r\n"
                + "    }\r\n"
                + "\r\n"
                + "    /**\r\n"
                + "     * Methode 1 1/2.\r\n"
                + "     */\r\n"
                + "    public void methodOneAndAHalf() {\r\n"
                + "    }\r\n"
                + "\r\n"
                + "    /**\r\n"
                + "     * Methode 2.\r\n"
                + "     */\r\n"
                + "    public List<String> method2() {\r\n"
                + "        return new ArrayList<>();\r\n"
                + "    }\r\n"
                + "}\r\n"
                + "",
                        "package text.xyz.abc;\r\n"
                        + "\r\n"
                        + "import java.util.List;\r\n"
                        + "import java.util.ArrayList;\r\n"
                        + "\r\n"
                        + "public class Testklasse {\r\n"
                        + "\r\n"
                        + "    /**\r\n"
                        + "     * Methode 1.\r\n"
                        + "     */\r\n"
                        + "    public void method1() {\r\n"
                        + "    }\r\n"
                        + "\r\n"
                        + "    /**\r\n"
                        + "     * Methode 2.\r\n"
                        + "     */\r\n"
                        + "    public List<String> method2() {\r\n"
                        + "        return new ArrayList<>();\r\n"
                        + "    }\r\n"
                        + "}\r\n"
                        + "");
        assertEquals(Arrays.asList(deletionAt(14)), diff);
    }

    @Test
    public void testWhenInDoubtMarkAdditionRangeAtMethodBoundariesWithMultipleInserts() throws Exception {
        final List<Pair<IPositionInText, IPositionInText>> diff = determineDiff(
                "package text.xyz.abc;\r\n"
                + "\r\n"
                + "import java.util.List;\r\n"
                + "import java.util.ArrayList;\r\n"
                + "\r\n"
                + "public class Testklasse {\r\n"
                + "\r\n"
                + "    /**\r\n"
                + "     * Methode 1.\r\n"
                + "     */\r\n"
                + "    public void method1() {\r\n"
                + "    }\r\n"
                + "\r\n"
                + "    /**\r\n"
                + "     * Methode 2.\r\n"
                + "     */\r\n"
                + "    public List<String> method2() {\r\n"
                + "        return new ArrayList<>();\r\n"
                + "    }\r\n"
                + "\r\n"
                + "    /**\r\n"
                + "     * Methode 3.\r\n"
                + "     */\r\n"
                + "    public List<String> method3() {\r\n"
                + "        return new ArrayList<>();\r\n"
                + "    }\r\n"
                + "}\r\n"
                + "",
                        "package text.xyz.abc;\r\n"
                        + "\r\n"
                        + "import java.util.List;\r\n"
                        + "import java.util.ArrayList;\r\n"
                        + "\r\n"
                        + "public class Testklasse {\r\n"
                        + "\r\n"
                        + "    /**\r\n"
                        + "     * Methode 1.\r\n"
                        + "     */\r\n"
                        + "    public void method1() {\r\n"
                        + "    }\r\n"
                        + "\r\n"
                        + "    /**\r\n"
                        + "     * Methode A.\r\n"
                        + "     */\r\n"
                        + "    public void methodA() {\r\n"
                        + "    }\r\n"
                        + "\r\n"
                        + "    /**\r\n"
                        + "     * Methode 2.\r\n"
                        + "     */\r\n"
                        + "    public List<String> method2() {\r\n"
                        + "        return new ArrayList<>();\r\n"
                        + "    }\r\n"
                        + "\r\n"
                        + "    /**\r\n"
                        + "     * Methode B.\r\n"
                        + "     */\r\n"
                        + "    public void methodB() {\r\n"
                        + "    }\r\n"
                        + "\r\n"
                        + "    /**\r\n"
                        + "     * Methode 3.\r\n"
                        + "     */\r\n"
                        + "    public List<String> method3() {\r\n"
                        + "        return new ArrayList<>();\r\n"
                        + "    }\r\n"
                        + "}\r\n"
                        + "");
        assertEquals(Arrays.asList(insertedLines(14, 19), insertedLines(27, 32)), diff);
    }

    @Test
    public void testNoExceptions() throws Exception {
        for (int i = 0; i < 1000; i++) {
            this.doTestNoExceptions(i);
        }
    }

    private void doTestNoExceptions(int seed) throws Exception {
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

    @Test
    public void testXMove() throws Exception {
        final List<Pair<IPositionInText, IPositionInText>> diff = determineDiff(
                "line 01\r\n"
                        + "line 02\r\n"
                        + "line 03\r\n"
                        + "line 04\r\n"
                        + "line 05\r\n"
                        + "line 06\r\n"
                        + "line 07\r\n"
                        + "line 08\r\n"
                        + "line 09\r\n",
                        "line 01\r\n"
                                + "line 02\r\n"
                                + "line 07\r\n"
                                + "line 04\r\n"
                                + "line 05\r\n"
                                + "line 06\r\n"
                                + "line 03\r\n"
                                + "line 08\r\n"
                                + "line 09\r\n");
        assertEquals(Arrays.asList(inLineChange(3, 7, 8), inLineChange(7, 7, 8)), diff);
    }

    @Test
    public void testNoUniqueLines() throws Exception {
        final List<Pair<IPositionInText, IPositionInText>> diff = determineDiff(
                "<?xml version=\"1.0\"?>\r\n"
                        + "<test>\r\n"
                        + "  <sendung dicke=\"1mm\" gewicht=\"2g\">\r\n"
                        + "    <var name=\"a\" value=\"b\"/>\r\n"
                        + "    <var name=\"A\" value=\"X\"/>\r\n"
                        + "  </sendung>\r\n"
                        + "  <sendung dicke=\"1mm\" gewicht=\"2g\">\r\n"
                        + "    <var name=\"a\" value=\"b\"/>\r\n"
                        + "    <var name=\"A\" value=\"X\"/>\r\n"
                        + "  </sendung>\r\n"
                        + "  <sendung dicke=\"1mm\" gewicht=\"2g\">\r\n"
                        + "    <var name=\"a\" value=\"b\"/>\r\n"
                        + "    <var name=\"A\" value=\"X\"/>\r\n"
                        + "  </sendung>\r\n"
                        + "  <sendung dicke=\"1mm\" gewicht=\"2g\">\r\n"
                        + "    <var name=\"a\" value=\"b\"/>\r\n"
                        + "    <var name=\"A\" value=\"X\"/>\r\n"
                        + "  </sendung>\r\n"
                        + "  <sendung dicke=\"1mm\" gewicht=\"2g\">\r\n"
                        + "    <var name=\"a\" value=\"b\"/>\r\n"
                        + "    <var name=\"A\" value=\"X\"/>\r\n"
                        + "  </sendung>\r\n"
                        + "  <sendung dicke=\"1mm\" gewicht=\"2g\">\r\n"
                        + "    <var name=\"a\" value=\"b\"/>\r\n"
                        + "    <var name=\"A\" value=\"X\"/>\r\n"
                        + "  </sendung>\r\n"
                        + "  <sendung dicke=\"1mm\" gewicht=\"2g\">\r\n"
                        + "    <var name=\"a\" value=\"b\"/>\r\n"
                        + "    <var name=\"A\" value=\"X\"/>\r\n"
                        + "  </sendung>\r\n"
                        + "  <sendung dicke=\"1mm\" gewicht=\"2g\">\r\n"
                        + "    <var name=\"a\" value=\"b\"/>\r\n"
                        + "    <var name=\"A\" value=\"X\"/>\r\n"
                        + "  </sendung>\r\n"
                        + "  <sendung dicke=\"1mm\" gewicht=\"2g\">\r\n"
                        + "    <var name=\"a\" value=\"b\"/>\r\n"
                        + "    <var name=\"A\" value=\"X\"/>\r\n"
                        + "  </sendung>\r\n"
                        + "</test>\r\n",
                        "<?xml version=\"1.0\"?>\r\n"
                                + "<test>\r\n"
                                + "  <sendung dicke=\"1mm\" gewicht=\"2g\">\r\n"
                                + "    <var name=\"a\" value=\"b\"/>\r\n"
                                + "    <var name=\"A\" value=\"X\"/>\r\n"
                                + "  </sendung>\r\n"
                                + "  <sendung dicke=\"1mm\" gewicht=\"2g\">\r\n"
                                + "    <var name=\"a\" value=\"b\"/>\r\n"
                                + "    <var name=\"A\" value=\"X\"/>\r\n"
                                + "  </sendung>\r\n"
                                + "  <sendung dicke=\"1.5mm\" gewicht=\"2g\">\r\n"
                                + "    <var name=\"a\" value=\"b\"/>\r\n"
                                + "    <var name=\"A\" value=\"X\"/>\r\n"
                                + "  </sendung>\r\n"
                                + "  <sendung dicke=\"1.5mm\" gewicht=\"2g\">\r\n"
                                + "    <var name=\"a\" value=\"b\"/>\r\n"
                                + "    <var name=\"A\" value=\"X\"/>\r\n"
                                + "  </sendung>\r\n"
                                + "  <sendung dicke=\"1mm\" gewicht=\"2g\">\r\n"
                                + "    <var name=\"a\" value=\"b\"/>\r\n"
                                + "    <var name=\"A\" value=\"X\"/>\r\n"
                                + "  </sendung>\r\n"
                                + "  <sendung dicke=\"1mm\" gewicht=\"2g\">\r\n"
                                + "    <var name=\"a\" value=\"b\"/>\r\n"
                                + "    <var name=\"A\" value=\"X\"/>\r\n"
                                + "  </sendung>\r\n"
                                + "  <sendung dicke=\"1mm\" gewicht=\"2g\">\r\n"
                                + "    <var name=\"a\" value=\"b\"/>\r\n"
                                + "    <var name=\"A\" value=\"X\"/>\r\n"
                                + "  </sendung>\r\n"
                                + "  <sendung dicke=\"10mm\" gewicht=\"20g\">\r\n"
                                + "    <var name=\"a0\" value=\"b\"/>\r\n"
                                + "    <var name=\"A0\" value=\"X\"/>\r\n"
                                + "  </sendung>\r\n"
                                + "  <sendung dicke=\"1mm\" gewicht=\"2g\">\r\n"
                                + "    <var name=\"a\" value=\"b\"/>\r\n"
                                + "    <var name=\"A\" value=\"X\"/>\r\n"
                                + "  </sendung>\r\n"
                                + "  <sendung dicke=\"1mm\" gewicht=\"2g\">\r\n"
                                + "    <var name=\"a\" value=\"b\"/>\r\n"
                                + "    <var name=\"A\" value=\"X\"/>\r\n"
                                + "  </sendung>\r\n"
                                + "</test>\r\n");
        assertEquals(Arrays.asList(inLineChange(11, 20, 22), inLineChange(15, 20, 22), insertedLines(31, 34)), diff);
    }

    @Test
    public void testInsertMethod() throws Exception {
        final List<Pair<IPositionInText, IPositionInText>> diff = determineDiff(
                "/**\r\n"
                        + " * asdf\r\n"
                        + " */\r\n"
                        + "foo() {\r\n"
                        + "}\r\n",
                        "/**\r\n"
                                + " * asdf\r\n"
                                + " * jklö\r\n"
                                + " */\r\n"
                                + "bar() {\r\n"
                                + "}\r\n"
                                + "\r\n"
                                + "/**\r\n"
                                + " * asdf\r\n"
                                + " */\r\n"
                                + "foo() {\r\n"
                                + "}\r\n"
                                );
        assertEquals(Arrays.asList(insertedLines(1, 7)), diff);
    }

    @Test
    public void testInsertXml() throws Exception {
        //in this example common suffix stripping has to be undone by moving the diff down
        final List<Pair<IPositionInText, IPositionInText>> diff = determineDiff(
                        "</abc>\r\n",
                        "</abc>\r\n"
                                + "<abc>\r\n"
                                + "  <child/>\r\n"
                                + "</abc>\r\n"
                                );
        assertEquals(Arrays.asList(insertedLines(2, 4)), diff);
    }

    @Test
    public void testInsertIf() throws Exception {
        final List<Pair<IPositionInText, IPositionInText>> diff = determineDiff(
                "public class Test {\r\n"
                + "\r\n"
                + "    public static function test() {\r\n"
                + "        byte[] triplet = doInitStuff();\r\n"
                + "\r\n"
                + "        if (format == ImageFormat.JPG) {\r\n"
                + "            doJpgStuff();\r\n"
                + "            return triplet;\r\n"
                + "        }\r\n"
                + "\r\n"
                + "        if (format == ImageFormat.TIF) {\r\n"
                + "            doTifStuff();\r\n"
                + "            return triplet;\r\n"
                + "        }\r\n"
                + "\r\n"
                + "        throw new NotYetSupportedException();\r\n"
                + "    }\r\n"
                + "\r\n"
                + "}\r\n",

                "public class Test {\r\n"
                + "\r\n"
                + "    public static function test() {\r\n"
                + "        byte[] triplet = doInitStuff();\r\n"
                + "\r\n"
                + "        if (format == ImageFormat.JPG) {\r\n"
                + "            doJpgStuff();\r\n"
                + "            return triplet;\r\n"
                + "        }\r\n"
                + "\r\n"
                + "        if (format == ImageFormat.TIF) {\r\n"
                + "            doTifStuff();\r\n"
                + "            return triplet;\r\n"
                + "        }\r\n"
                + "\r\n"
                + "        if (format == ImageFormat.PNG) {\r\n"
                + "            doPngStuff();\r\n"
                + "            return triplet;\r\n"
                + "        }\r\n"
                + "\r\n"
                + "        throw new NotYetSupportedException();\r\n"
                + "    }\r\n"
                + "\r\n"
                + "}\r\n"
        );
        assertEquals(Arrays.asList(insertedLines(16, 20)), diff);
    }


    @Test
    public void testInsertWhereEmptyLineIsBestStart() throws Exception {
        final List<Pair<IPositionInText, IPositionInText>> diff = determineDiff(
                "public class Test {\r\n"
                + "\r\n"
                + "    public static function test1() {\r\n"
                + "        doStuff();\r\n"
                + "    }\r\n"
                + "}\r\n",

                "public class Test {\r\n"
                + "\r\n"
                + "    public static function test1() {\r\n"
                + "        doStuff();\r\n"
                + "    }\r\n"
                + "\r\n"
                + "    public static function test2() {\r\n"
                + "        doOtherStuff();\r\n"
                + "    }\r\n"
                + "}\r\n"
        );
        assertEquals(Arrays.asList(insertedLines(6, 9)), diff);
    }

    /**
     * There was a problem that upwards shifting led to overlapping fragments in some cases. This testcase
     * triggered that problem.
     */
    @Test
    public void testShiftingDoesNotCauseOverlap() throws Exception {
        final List<Pair<IPositionInText, IPositionInText>> diff = determineDiff(
                "/**\r\n"
                + " * Verwaltet einheitlich den Zugriff auf Verwerfungsanfragen (temporäre und persistente).\r\n"
                + " */\r\n"
                + "public final class DBTerminationRequestStore extends AbstractTerminationRequestStore {\r\n"
                + "\r\n"
                + "    private final StorageEnvironmentConnection connection;\r\n"
                + "    private final DocumentTerminationRequestDatabase database;\r\n"
                + "    private final DBStorageTransaction storageTransaction;\r\n"
                + "    private final FlyweightDocumentCreator documentCreator;\r\n"
                + "    private final FlyweightDocumentTerminationRequestCreator documentTerminationRequestCreator;\r\n"
                + "\r\n"
                + "    /**\r\n"
                + "     * Erstellt ein neues {@link DBTerminationRequestStore} Objekt.\r\n"
                + "     */\r\n"
                + "    DBTerminationRequestStore(\r\n"
                + "            final StorageEnvironmentConnection connection,\r\n"
                + "            final DBStorageTransaction storageTransaction,\r\n"
                + "            final FlyweightDocumentTerminationRequestCreator documentTerminationRequestCreator,\r\n"
                + "            final FlyweightDocumentCreator documentCreator) {\r\n"
                + "        this.database = connection.getDatabase(DocumentTerminationRequestDatabase.class);\r\n"
                + "        this.connection = connection;\r\n"
                + "        this.storageTransaction = storageTransaction;\r\n"
                + "        this.documentTerminationRequestCreator = documentTerminationRequestCreator;\r\n"
                + "        this.documentCreator = documentCreator;\r\n"
                + "    }\r\n"
                + "}\r\n",

                "/**\r\n"
                + " * Verwaltet einheitlich den Zugriff auf Verwerfungsanfragen (temporäre und persistente).\r\n"
                + " */\r\n"
                + "public final class DBTerminationRequestStore extends AbstractTerminationRequestStore {\r\n"
                + "\r\n"
                + "    private final StorageEnvironmentConnection connection;\r\n"
                + "    private final DocumentTerminationRequestDatabase database;\r\n"
                + "    private final DBStorageTransaction storageTransaction;\r\n"
                + "    private final FlyweightDocumentCreator documentCreator;\r\n"
                + "    private final FlyweightDocumentTerminationRequestCreator documentTerminationRequestCreator;\r\n"
                + "\r\n"
                + "    /**\r\n"
                + "     * Erstellt ein neues {@link DBTerminationRequestStore} Objekt.\r\n"
                + "     * Ermöglicht das Stornieren von Produktionsaufträgen.\r\n"
                + "     */\r\n"
                + "    public DBTerminationRequestStore(\r\n"
                + "            final StorageEnvironmentConnection connection,\r\n"
                + "            final DBStorageTransaction storageTransaction,\r\n"
                + "            final FlyweightDocumentTerminationRequestCreator documentTerminationRequestCreator,\r\n"
                + "            final FlyweightDocumentCreator documentCreator,\r\n"
                + "            final Set<MonitoringId> mailingsWithProductionJobs) {\r\n"
                + "        super(mailingsWithProductionJobs);\r\n"
                + "        this.database = connection.getDatabase(DocumentTerminationRequestDatabase.class);\r\n"
                + "        this.connection = connection;\r\n"
                + "        this.storageTransaction = storageTransaction;\r\n"
                + "        this.documentTerminationRequestCreator = documentTerminationRequestCreator;\r\n"
                + "        this.documentCreator = documentCreator;\r\n"
                + "    }\r\n"
                + "\r\n"
                + "    /**\r\n"
                + "     * Erstellt ein neues {@link DBTerminationRequestStore} Objekt.\r\n"
                + "     * Mit diesem Store können keine Produktionsaufträge verworfen werden.\r\n"
                + "     */\r\n"
                + "    public DBTerminationRequestStore(\r\n"
                + "            final StorageEnvironmentConnection connection,\r\n"
                + "            final DBStorageTransaction storageTransaction,\r\n"
                + "            final FlyweightDocumentTerminationRequestCreator documentTerminationRequestCreator,\r\n"
                + "            final FlyweightDocumentCreator documentCreator) {\r\n"
                + "        super();\r\n"
                + "        this.database = connection.getDatabase(DocumentTerminationRequestDatabase.class);\r\n"
                + "        this.connection = connection;\r\n"
                + "        this.storageTransaction = storageTransaction;\r\n"
                + "        this.documentTerminationRequestCreator = documentTerminationRequestCreator;\r\n"
                + "        this.documentCreator = documentCreator;\r\n"
                + "    }\r\n"
                + "}\r\n"
        );
        assertEquals(Arrays.asList(insertedLines(14, 14), insertedLines(16, 34), insertedLines(39, 39)), diff);
    }

}
