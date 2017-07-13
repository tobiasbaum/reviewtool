package de.setsoftware.reviewtool.ordering2.base;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import de.setsoftware.reviewtool.ordering2.defaultimpl.SimpleStop;

public class TourComparisonTest {

    @Test
    public void testSameTourNoPatterns() {
        final StopRelationGraph g = new StopRelationGraph();
        g.addStop(stop("a"));
        g.addStop(stop("b"));
        g.addStop(stop("c"));
        g.addRelation(rel(TestRelationTypes.CALL_FLOW, "foo", "a", "c"));
        g.addRelation(rel(TestRelationTypes.CALL_FLOW, "foo", "b", "c"));

        final Tour t = tour(g, "a", "b", "c");
        assertEquals(t.compareTo(t, Collections.<Pattern>emptySet()), PartialCompareResult.EQUAL);
    }

    @Test
    public void testSameTour() {
        final StopRelationGraph g = new StopRelationGraph();
        g.addStop(stop("a"));
        g.addStop(stop("b"));
        g.addStop(stop("c"));
        g.addRelation(rel(TestRelationTypes.CALL_FLOW, "foo", "a", "c"));
        g.addRelation(rel(TestRelationTypes.CALL_FLOW, "foo", "b", "c"));

        final Tour t = tour(g, "a", "b", "c");
        assertEquals(t.compareTo(t, standardPatterns()), PartialCompareResult.EQUAL);
    }

    @Test
    public void testTourInWrongOrderVsTourInRightOrder() {
        final StopRelationGraph g = new StopRelationGraph();
        g.addStop(stop("a"));
        g.addStop(stop("b"));
        g.addStop(stop("c"));
        g.addRelation(rel(TestRelationTypes.CALL_FLOW, "foo", "a", "c"));
        g.addRelation(rel(TestRelationTypes.CALL_FLOW, "foo", "b", "c"));

        final Tour t1 = tour(g, "c", "a", "b");
        final Tour t2 = tour(g, "a", "c", "b");
        final Tour t3 = tour(g, "c", "b", "a");
        assertEquals(t1.compareTo(t2, standardPatterns()), PartialCompareResult.GREATER);
        assertEquals(t2.compareTo(t1, standardPatterns()), PartialCompareResult.LESS);
        assertEquals(t3.compareTo(t2, standardPatterns()), PartialCompareResult.GREATER);
        assertEquals(t2.compareTo(t3, standardPatterns()), PartialCompareResult.LESS);
        assertEquals(t3.compareTo(t1, standardPatterns()), PartialCompareResult.EQUAL);
        assertEquals(t1.compareTo(t3, standardPatterns()), PartialCompareResult.EQUAL);
    }

    @Test
    public void testDirectlyTogetherVsSpacedApart() {
        final StopRelationGraph g = new StopRelationGraph();
        g.addStop(stop("a"));
        g.addStop(stop("b"));
        g.addStop(stop("c"));
        g.addRelation(rel(TestRelationTypes.CALL_FLOW, "foo", "a", "b"));

        final Tour t1 = tour(g, "b", "a", "c");
        final Tour t2 = tour(g, "b", "c", "a");
        assertEquals(t1.compareTo(t2, standardPatterns()), PartialCompareResult.GREATER);
        assertEquals(t2.compareTo(t1, standardPatterns()), PartialCompareResult.LESS);
    }

    @Test
    public void testIncomparable() {
        final StopRelationGraph g = new StopRelationGraph();
        g.addStop(stop("a"));
        g.addStop(stop("b"));
        g.addStop(stop("c"));
        g.addStop(stop("d"));
        g.addRelation(rel(TestRelationTypes.CALL_FLOW, "foo", "a", "b"));
        g.addRelation(rel(TestRelationTypes.CALL_FLOW, "foo", "c", "d"));

        final Tour t1 = tour(g, "d", "b", "a", "c");
        final Tour t2 = tour(g, "b", "d", "c", "a");
        assertEquals(t1.compareTo(t2, standardPatterns()), PartialCompareResult.INCOMPARABLE);
        assertEquals(t2.compareTo(t1, standardPatterns()), PartialCompareResult.INCOMPARABLE);
    }

    @Test
    public void testDirVsDist() {
        final StopRelationGraph g = new StopRelationGraph();
        g.addStop(stop("a"));
        g.addStop(stop("b"));
        g.addStop(stop("c"));
        g.addRelation(rel(TestRelationTypes.CALL_FLOW, "foo", "a", "b"));

        final Tour t1 = tour(g, "b", "a", "c");
        final Tour t2 = tour(g, "a", "b", "c");
        final Tour t3 = tour(g, "b", "c", "a");
        assertEquals(t1.compareTo(t1, standardPatterns()), PartialCompareResult.EQUAL);
        assertEquals(t1.compareTo(t2, standardPatterns()), PartialCompareResult.GREATER);
        assertEquals(t1.compareTo(t3, standardPatterns()), PartialCompareResult.GREATER);
        assertEquals(t2.compareTo(t1, standardPatterns()), PartialCompareResult.LESS);
        assertEquals(t2.compareTo(t2, standardPatterns()), PartialCompareResult.EQUAL);
        assertEquals(t2.compareTo(t3, standardPatterns()), PartialCompareResult.GREATER);
        assertEquals(t3.compareTo(t1, standardPatterns()), PartialCompareResult.LESS);
        assertEquals(t3.compareTo(t2, standardPatterns()), PartialCompareResult.LESS);
        assertEquals(t3.compareTo(t3, standardPatterns()), PartialCompareResult.EQUAL);
    }

    @Test
    public void testCluster() {
        final StopRelationGraph g = new StopRelationGraph();
        g.addStop(stop("a"));
        g.addStop(stop("b"));
        g.addStop(stop("c"));
        g.addStop(stop("d"));
        g.addRelation(rel(TestRelationTypes.CALL_FLOW, "foo", "a", "b"));
        g.addRelation(rel(TestRelationTypes.CALL_FLOW, "bar", "a", "c"));

        final Tour t1 = tour(g, "d", "c", "b", "a");
        final Tour t2 = tour(g, "c", "d", "b", "a");
        assertEquals(t1.compareTo(t2, standardPatterns()), PartialCompareResult.GREATER);
        assertEquals(t2.compareTo(t1, standardPatterns()), PartialCompareResult.LESS);
    }

    @Test
    public void testClusterVsFlow() {
        final StopRelationGraph g = new StopRelationGraph();
        g.addStop(stop("a"));
        g.addStop(stop("b"));
        g.addStop(stop("c"));
        g.addRelation(rel(TestRelationTypes.CALL_FLOW, "foo", "a", "b"));
        g.addRelation(rel(TestRelationTypes.CALL_FLOW, "bar", "a", "c"));

        final Tour cluster = tour(g, "b", "c", "a");
        final Tour flow = tour(g, "b", "a", "c");
        //TODO is incomparable correct here, or should it be something else
        assertEquals(cluster.compareTo(flow, standardPatterns()), PartialCompareResult.INCOMPARABLE);
        assertEquals(flow.compareTo(cluster, standardPatterns()), PartialCompareResult.INCOMPARABLE);
    }

    private static Tour tour(StopRelationGraph g, String... stopIDs) {
        final List<Stop> stops = new ArrayList<>();
        for (final String s : stopIDs) {
            stops.add(stop(s));
        }
        return new Tour(g, stops);
    }

    private static Set<Pattern> standardPatterns() {
        return new LinkedHashSet<Pattern>(Arrays.asList(
                new GreedyTypePattern(TestRelationTypes.CALL_FLOW)));
    }

    private static Relation rel(RelationType type, String id, String from, String to) {
        return new Relation(type, id, stop(from), stop(to));
    }

    private static Stop stop(String id) {
        return new SimpleStop(id);
    }

}
