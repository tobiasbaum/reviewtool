package de.setsoftware.reviewtool.ordering;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import de.setsoftware.reviewtool.model.api.FileChangeType;
import de.setsoftware.reviewtool.model.api.IFragment;
import de.setsoftware.reviewtool.model.api.IRevisionedFile;
import de.setsoftware.reviewtool.model.changestructure.ChangestructureFactory;
import de.setsoftware.reviewtool.model.changestructure.Fragment;
import de.setsoftware.reviewtool.model.changestructure.Stop;
import de.setsoftware.reviewtool.ordering.efficientalgorithm.TourCalculatorControl;

public class MethodOverrideRelationTest {

    private static final String CNG = "#CNG#";

    private static IRevisionedFile file(final String name, final int revision, String content) {
        return new StubFile(name, revision, content);
    }

    private static Stop binaryStop(final String filename) {
        return new Stop(
                ChangestructureFactory.createBinaryChange(
                        null, FileChangeType.OTHER, file(filename, 1, ""), file(filename, 3, "")),
                file(filename, 4, ""));
    }

    private static List<Stop> createInput(final String... files) {
        final List<Stop> ret = new ArrayList<>();
        for (int i = 0; i < files.length; i++) {
            final String filename = "File" + i + ".java";
            final IRevisionedFile fileOld = file(filename, 1, files[i]);
            final IRevisionedFile fileNew = file(filename, 2, files[i].replace(CNG, ""));

            final String[] lines = files[i].split("\n");
            int cngCount = 0;
            for (int line = 0; line < lines.length; line++) {
                if (lines[line].startsWith(CNG)) {
                    cngCount++;
                } else {
                    if (cngCount > 0) {
                        final int startLine = line - cngCount;
                        final IFragment from = Fragment.createWithContent(fileOld,
                                ChangestructureFactory.createPositionInText(startLine + 1, 1),
                                ChangestructureFactory.createPositionInText(line + 1, 1),
                                combineLines(lines, startLine, line));
                        final IFragment to = Fragment.createWithContent(fileNew,
                                ChangestructureFactory.createPositionInText(startLine + 1, 1),
                                ChangestructureFactory.createPositionInText(line + 1, 1),
                                combineLines(lines, startLine, line).replace(CNG, ""));
                        ret.add(new Stop(
                                ChangestructureFactory.createTextualChangeHunk(null, FileChangeType.OTHER, from, to),
                                to));
                        cngCount = 0;
                    }
                }
            }
        }
        return ret;
    }

    private static String combineLines(String[] lines, int from, int to) {
        final StringBuilder ret = new StringBuilder();
        for (int i = from; i < to; i++) {
            ret.append(lines[i]).append('\n');
        }
        return ret.toString();
    }

    private static ChangePart wrap(final Stop s) {
        return new ChangePart(Collections.singletonList(s), Collections.emptySet());
    }

    private static List<ChangePart> wrap(final List<Stop> stops) {
        final List<ChangePart> ret = new ArrayList<>();
        for (final Stop s : stops) {
            ret.add(wrap(s));
        }
        return ret;
    }

    private static Collection<? extends OrderingInfo> determineRelations(final List<Stop> stops) throws Exception {
        return new MethodOverrideRelation(HierarchyExplicitness.ONLY_NONTRIVIAL).determineMatches(
                wrap(stops), TourCalculatorControl.NO_CANCEL);
    }

    private static OrderingInfo match(String name, final Stop center, final Stop... others) {
        return OrderingInfoImpl.star(HierarchyExplicitness.ONLY_NONTRIVIAL, name,
                wrap(center), wrap(Arrays.asList(others)));
    }


    @Test
    public void testEmptyInput() throws Exception {
        assertEquals(Collections.emptyList(), determineRelations(Collections.emptyList()));
    }

    @Test
    public void testSingleInput() throws Exception {
        assertEquals(Collections.emptyList(), determineRelations(createInput(
                "package com.example;\n"
                + "\n"
                + "class B extends A {\n"
                + "    public void foo() {\n"
                + CNG + "        int i = 0;\n"
                + "    }\n"
                + "}\n")));
    }

    @Test
    public void testBinaryStops() throws Exception {
        assertEquals(Collections.emptyList(), determineRelations(Arrays.asList(binaryStop("a"), binaryStop("b"))));
    }

    @Test
    public void testSimpleOverride() throws Exception {
        final List<Stop> stops = createInput(
                "package com.example;\n"
                + "\n"
                + "class A {\n"
                + "    public void foo() {\n"
                + CNG + "        int i = 2;\n"
                + "    }\n"
                + "}\n",
                "package com.example;\n"
                + "\n"
                + "class B extends A {\n"
                + "    @Override\n"
                + "    public void foo() {\n"
                + CNG + "        int i = 0;\n"
                + "    }\n"
                + "}\n");
        assertEquals(
            Collections.singletonList(match("foo() hierarchy", stops.get(0), stops.get(1))),
            determineRelations(stops));
    }

    @Test
    public void testParamCountIsTakenIntoAccount() throws Exception {
        final List<Stop> stops = createInput(
                "package com.example;\n"
                + "\n"
                + "class B extends A {\n"
                + "    @Override\n"
                + "    public void foo(int j) {\n"
                + CNG + "        int i = 0;\n"
                + "    }\n"
                + "}\n",                "package com.example;\n"
                + "\n"
                + "class A {\n"
                + "    public void foo() {\n"
                + CNG + "        int i = 2;\n"
                + "    }\n"
                + "    public void foo(int j) {\n"
                + CNG + "        int i = j;\n"
                + "    }\n"
                + "}\n");
        assertEquals(
            Collections.singletonList(match("foo(_) hierarchy", stops.get(2), stops.get(0))),
            determineRelations(stops));
    }

    @Test
    public void testFinalClassDoesNotCount() throws Exception {
        final List<Stop> stops = createInput(
                "package com.example;\n"
                + "\n"
                + "final class A {\n"
                + "    public void foo() {\n"
                + CNG + "        int i = 2;\n"
                + "    }\n"
                + "}\n",
                "package com.example;\n"
                + "\n"
                + "class B extends C {\n"
                + "    @Override\n"
                + "    public void foo() {\n"
                + CNG + "        int i = 0;\n"
                + "    }\n"
                + "}\n");
        assertEquals(
            Collections.emptyList(),
            determineRelations(stops));
    }

    @Test
    public void testTwoOverrides() throws Exception {
        final List<Stop> stops = createInput(
                "package com.example;\n"
                + "\n"
                + "class A {\n"
                + CNG + "    protected void bar(int i, int j) {\n"
                + "    }\n"
                + "}\n",
                "package com.example;\n"
                + "\n"
                + "class B extends A {\n"
                + "    @Override\n"
                + CNG + "    protected void bar(int i, int j) {\n"
                + "    }\n"
                + "}\n",
                "package com.example;\n"
                + "\n"
                + "class C extends A {\n"
                + "    @Override\n"
                + CNG + "    protected void bar(int i, int j) {\n"
                + "    }\n"
                + "}\n");
        assertEquals(
            Collections.singletonList(match("bar(_,_) hierarchy", stops.get(0), stops.get(1), stops.get(2))),
            determineRelations(stops));
    }

    @Test
    public void testInnerClass() throws Exception {
        final List<Stop> stops = createInput(
                "package com.example;\n"
                + "\n"
                + "class Outer {\n"
                + "    class A {\n"
                + CNG + "        protected void bar(int i, int j) {\n"
                + "        }\n"
                + "    }\n"
                + "\n"
                + "    class B extends A {\n"
                + "        @Override\n"
                + CNG + "        protected void bar(int i, int j) {\n"
                + "        }\n"
                + "    }\n"
                + "}\n");
        assertEquals(
            Collections.singletonList(match("bar(_,_) hierarchy", stops.get(0), stops.get(1))),
            determineRelations(stops));
    }

}
