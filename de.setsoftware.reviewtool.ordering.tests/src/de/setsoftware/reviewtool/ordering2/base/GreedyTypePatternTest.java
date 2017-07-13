package de.setsoftware.reviewtool.ordering2.base;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.Test;

import de.setsoftware.reviewtool.ordering2.defaultimpl.SimpleStop;

public class GreedyTypePatternTest {

    @Test
    public void testNoMatchInEmptyGraph() {
        final StopRelationGraph g = new StopRelationGraph();
        final GreedyTypePattern p = new GreedyTypePattern(TestRelationTypes.CALL_FLOW);

        assertEquals(Collections.emptySet(), p.patternMatches(g));
    }

    @Test
    public void testNoMatchInOneElementGraph() {
        final StopRelationGraph g = new StopRelationGraph();
        g.addStop(stop("a"));
        final GreedyTypePattern p = new GreedyTypePattern(TestRelationTypes.CALL_FLOW);

        assertEquals(Collections.emptySet(), p.patternMatches(g));
    }

    @Test
    public void testNoMatchInTwoElementGraphWithWrongRelation() {
        final StopRelationGraph g = new StopRelationGraph();
        g.addStop(stop("a"));
        g.addStop(stop("b"));
        g.addRelation(rel(TestRelationTypes.CLASS_REFERENCE, "Foo", "a", "b"));
        final GreedyTypePattern p = new GreedyTypePattern(TestRelationTypes.CALL_FLOW);

        assertEquals(Collections.emptySet(), p.patternMatches(g));
    }

    @Test
    public void testMatchInTwoElementGraph() {
        final StopRelationGraph g = new StopRelationGraph();
        g.addStop(stop("a"));
        g.addStop(stop("b"));
        g.addRelation(rel(TestRelationTypes.CALL_FLOW, "foo", "a", "b"));
        final GreedyTypePattern p = new GreedyTypePattern(TestRelationTypes.CALL_FLOW);

        assertEquals(setOf(pm(p, "foo", "b", "a")), p.patternMatches(g));
    }

    @Test
    public void testMatchInThreeElementGraphWithTwoIDs() {
        final StopRelationGraph g = new StopRelationGraph();
        g.addStop(stop("a"));
        g.addStop(stop("b"));
        g.addStop(stop("c"));
        g.addRelation(rel(TestRelationTypes.CALL_FLOW, "foo", "a", "c"));
        g.addRelation(rel(TestRelationTypes.CALL_FLOW, "foo", "b", "c"));
        g.addRelation(rel(TestRelationTypes.CALL_FLOW, "bar", "a", "c"));
        final GreedyTypePattern p = new GreedyTypePattern(TestRelationTypes.CALL_FLOW);

        assertEquals(setOf(pm(p, "foo", "c", "a", "b"), pm(p, "bar", "c", "a")), p.patternMatches(g));
    }

    @SafeVarargs
    private static Set<PatternMatch> setOf(PatternMatch... lists) {
        final LinkedHashSet<PatternMatch> ret = new LinkedHashSet<>();
        for (final PatternMatch list : lists) {
            ret.add(list);
        }
        return ret;
    }

    private static PatternMatch pm(Pattern p, String label, String... ids) {
        final ArrayList<Stop> stops = new ArrayList<>();
        for (final String id : ids) {
            stops.add(stop(id));
        }
        final PatternMatchWithLabel ret = new PatternMatchWithLabel(p, label, stops);
        ret.fixStopAtPosition(0);
        return ret;
    }

    private static Relation rel(RelationType type, String id, String from, String to) {
        return new Relation(type, id, stop(from), stop(to));
    }

    private static Stop stop(String id) {
        return new SimpleStop(id);
    }

}
