package de.setsoftware.reviewtool.ordering;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import de.setsoftware.reviewtool.model.changestructure.Stop;
import de.setsoftware.reviewtool.ordering.efficientalgorithm.TourCalculatorControl;

public class MethodCallRelationTest {

    private static final String CNG = MethodOverrideRelationTest.CNG;

    private static Stop binaryStop(final String filename) {
        return MethodOverrideRelationTest.binaryStop(filename);
    }

    private static List<Stop> createInput(final String... files) {
        return MethodOverrideRelationTest.createInput(files);
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
        return new MethodCallRelation(HierarchyExplicitness.NONE).determineMatches(
                wrap(stops), TourCalculatorControl.NO_CANCEL);
    }

    private static OrderingInfo match(String name, final Stop center, final Stop... others) {
        return OrderingInfoImpl.star(HierarchyExplicitness.NONE, name,
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
    public void testSimpleCall() throws Exception {
        final List<Stop> stops = createInput(
                "package com.example;\n"
                + "\n"
                + "class A {\n"
                + "    public void foo() {\n"
                + CNG + "        this.bar();\n"
                + "    }\n"
                + "\n"
                + "    private void bar() {\n"
                + CNG + "        System.out.println(\"asdf\");\n"
                + "    }\n"
                + "}\n");
        assertEquals(
            Collections.singletonList(match("bar() calls", stops.get(1), stops.get(0))),
            determineRelations(stops));
    }

    @Test
    public void testOnlyCallsInChangesCount() throws Exception {
        final List<Stop> stops = createInput(
                "package com.example;\n"
                + "\n"
                + "class A {\n"
                + "    public void foo1() {\n"
                + CNG + "        this.bar();\n"
                + "    }\n"
                + "\n"
                + "    private void bar() {\n"
                + CNG + "        System.out.println(\"asdf\");\n"
                + "    }\n"
                + "\n"
                + "    public void foo2() {\n"
                + "        this.bar();\n"
                + "    }\n"
                + "\n"
                + "    public void foo3() {\n"
                + CNG + "        this.bar();\n"
                + "    }\n"
                + "\n"
                + "}\n");
        assertEquals(
            Collections.singletonList(match("bar() calls", stops.get(1), stops.get(0), stops.get(2))),
            determineRelations(stops));
    }

    @Test
    public void testMultipleClasses() throws Exception {
        final List<Stop> stops = createInput(
                "package com.example;\n"
                + "\n"
                + "class A {\n"
                + "    public void doStuff() {\n"
                + CNG + "        new B().stuffDo();\n"
                + "    }\n"
                + "}\n",
                "package com.example;\n"
                + "\n"
                + "class B {\n"
                + CNG + "    public void stuffDo() {\n"
                + "    }\n"
                + "}\n");
        assertEquals(
            Collections.singletonList(match("stuffDo() calls", stops.get(1), stops.get(0))),
            determineRelations(stops));
    }

}
