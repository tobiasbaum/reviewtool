package de.setsoftware.reviewtool.ordering2.base;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import de.setsoftware.reviewtool.ordering2.BruteForceAlgorithm;
import de.setsoftware.reviewtool.ordering2.defaultimpl.SimpleStop;

public class Playground {

    public static void main(String[] args) {
        final StopRelationGraph graph = graphPsy6663();

//        final Tour empiricalTour = new Tour(graph, Arrays.asList(
//                stop("Buch"), stop("Maus"), stop("Homer"), stop("Stern"), stop("Birne"), stop("Strasse"), stop("Baum")));
//
//        System.out.println("Empirical match set: " + empiricalTour.determineMatchSet(patterns()));

        final Set<Tour> best = BruteForceAlgorithm.determineBestTours(graph, patterns());
        System.out.println("Best tours: " + best.size());
        System.out.println(best);
//        System.out.println("Empirical contained: " + best.contains(empiricalTour));
//        for (final Tour oneOfBest : best) {
//            final PartialCompareResult cmp = empiricalTour.compareTo(oneOfBest, patterns());
//            if (cmp != PartialCompareResult.INCOMPARABLE) {
//                System.out.println("Empirical comparison: " + cmp);
//                System.out.println("Opponent: " + oneOfBest);
//                System.out.println("Opponent match set: " + oneOfBest.determineMatchSet(patterns()));
//            }
//        }

        final Set<Tour> worst = BruteForceAlgorithm.determineWorstTours(graph, patterns());
        System.out.println("Worst tours:" + worst.size());
        System.out.println(worst);
    }

    private static StopRelationGraph graph1() {
        final StopRelationGraph g = new StopRelationGraph();
        g.addStop(stop("a"));
        g.addStop(stop("b"));
        g.addStop(stop("c"));
        g.addRelation(rel(TestRelationTypes.CALL_FLOW, "foo", "a", "b"));
        return g;
    }

    private static StopRelationGraph graph2() {
        final StopRelationGraph g = new StopRelationGraph();
        g.addStop(stop("a"));
        g.addStop(stop("b"));
        g.addStop(stop("c"));
        g.addRelation(rel(TestRelationTypes.CALL_FLOW, "foo", "a", "b"));
        g.addRelation(rel(TestRelationTypes.CALL_FLOW, "foo", "a", "c"));
        return g;
    }

    private static StopRelationGraph graph3() {
        final StopRelationGraph g = new StopRelationGraph();
        g.addStop(stop("a"));
        g.addStop(stop("b"));
        g.addStop(stop("c"));
        g.addStop(stop("d"));
        g.addRelation(rel(TestRelationTypes.CALL_FLOW, "foo", "a", "b"));
        return g;
    }

    private static StopRelationGraph graph4() {
        final StopRelationGraph g = new StopRelationGraph();
        g.addStop(stop("Maus"));
        g.addStop(stop("Birne"));
        g.addStop(stop("Stern"));
        g.addStop(stop("Homer"));
        g.addStop(stop("Baum"));
        g.addStop(stop("Strasse"));
        g.addStop(stop("Buch"));

        addRelationStar(g, TestRelationTypes.SAME_METHOD, "determineChange()", "Strasse", "Baum");
        addRelationStar(g, TestRelationTypes.SAME_METHOD, "<init>()", "Stern", "Birne");
        addRelationStar(g, TestRelationTypes.DECLARATION_USE, "att:maxTextDiffThreshold", "Homer", "Strasse", "Baum", "Birne");
        addRelationStar(g, TestRelationTypes.DECLARATION_USE, "par:maxTextDiffThreshold", "Stern", "Birne");
        addRelationChain(g, TestRelationTypes.DATA_FLOW, "xml_param", "Buch", "Maus", "Stern", "Birne", "Strasse", "Baum");
        addRelationStar(g, TestRelationTypes.CALL_FLOW, "SvnChangeSource()", "Maus", "Stern");
        addRelationSymmetric(g, TestRelationTypes.SIMILARITY, "foo", "Strasse", "Baum");
        return g;
    }

    private static StopRelationGraph graphPsy6663() {
        final StopRelationGraph g = new StopRelationGraph();
        g.addStop(stop("trumpet"));
        g.addStop(stop("time"));
        g.addStop(stop("rock"));
        g.addStop(stop("raven"));
        g.addStop(stop("house"));
        g.addStop(stop("golf"));
        g.addStop(stop("dog"));
        g.addStop(stop("city"));
        g.addStop(stop("apple"));
        g.addStop(stop("air"));


        addRelationChain(g, TestRelationTypes.SAME_FILE, "DocumentPoolClassSelectionMapper", "apple", "dog", "trumpet", "time");
        addRelationChain(g, TestRelationTypes.SAME_FILE, "DocumentPoolClassSelectionMapperRegistry", "city");
        addRelationChain(g, TestRelationTypes.SAME_FILE, "FromPoolPullOutMapper", "rock");
        addRelationChain(g, TestRelationTypes.SAME_FILE, "PullOutDocumentsFromPoolRunMapper", "raven");
        addRelationChain(g, TestRelationTypes.SAME_FILE, "pool.xsd", "house");
        addRelationChain(g, TestRelationTypes.SAME_FILE, "basicModules.xml", "golf");
        addRelationChain(g, TestRelationTypes.SAME_FILE, "SchemaLicence", "air");
        addRelationStar(g, TestRelationTypes.LOGICAL_DEPENDENCY, "Activatable", "apple", "air", "dog", "time");
        addRelationStar(g, TestRelationTypes.LOGICAL_DEPENDENCY, "Mapper use", "golf", "apple");
        //going bottom-up in call flow
        addRelationStar(g, TestRelationTypes.CALL_FLOW, "Mapper.mapSelection", "trumpet", "city");
        addRelationStar(g, TestRelationTypes.CALL_FLOW, "Registry.mapSelection", "city", "raven", "rock");
        addRelationStar(g, TestRelationTypes.CALL_FLOW, "Registry.register", "city", "time");
        //no direction preference in data flow because data flows in as well as out
        addRelationSymmetric(g, TestRelationTypes.DATA_FLOW, "Mapper.mapSelection", "city", "trumpet");
        addRelationSymmetric(g, TestRelationTypes.DATA_FLOW, "Registry.mapSelection", "raven", "city");
        addRelationSymmetric(g, TestRelationTypes.DATA_FLOW, "Registry.mapSelection", "rock", "city");
        addRelationSymmetric(g, TestRelationTypes.DATA_FLOW, "Registry.register", "time", "city");
        addRelationSymmetric(g, TestRelationTypes.SIMILARITY, "Mapper->Registry", "raven", "rock");
        return g;
    }

//    private static SimpleRelatednessInfo getPsy6561TestData(RelationType... relationPreferences) {
//        final Set<String> stops = new LinkedHashSet<>(Arrays.asList(
//                "rubin", "rugby", "bee", "track", "orange", "river", "lemon", "coin", "atlantic", "isle"));
//
//        final SimpleRelatednessInfo r = new SimpleRelatednessInfo(stops, relationPreferences);
//
//        r.addReason(RelationType.SAME_FILE, "XMLWriterTest", "rubin|1");
//        r.addReason(RelationType.SAME_FILE, "XMLNodesFromVariableNestingElement", "bee|1", "rugby|2");
//        r.addReason(RelationType.SAME_FILE, "CompositeNestingElement", "track|1", "orange|2", "river|4", "lemon|8");
//        r.addReason(RelationType.SAME_FILE, "LeafNestingElement", "coin|1", "atlantic|2");
//        r.addReason(RelationType.SAME_FILE, "NestingElement", "isle|1");
//        r.addReason(RelationType.OVERRIDES, "triggerStart", "isle|1", "coin|2", "river|2", "rugby|2");
//        r.addReason(RelationType.SAME_METHOD, "CompositeNestingElement.triggerStart", "river|1", "lemon|2");
//        //going top-down in call flow
//        r.addReason(RelationType.CALL_FLOW, "LeafNestingElement.triggerStart", "atlantic|1", "coin|2");
//        r.addReason(RelationType.CALL_FLOW, "CompositeNestingElement.triggerStart", "track|1", "river|2");
//        r.addReason(RelationType.CALL_FLOW, "XMLNodesFromVariableNestingElement.triggerStart", "bee|1", "rugby|2");
//        r.addReason(RelationType.CALL_FLOW, "NestingElement.triggerStart", "lemon|1", "isle|2");
//        r.addReason(RelationType.CALL_FLOW, "NestingElement.hasCompositeChildren", "lemon|1", "orange|2");
//        r.addReason(RelationType.DATA_FLOW, "LeafNestingElement.triggerStart", "atlantic|1", "coin|2");
//        r.addReason(RelationType.DATA_FLOW, "CompositeNestingElement.triggerStart", "track|1", "river|2");
//        r.addReason(RelationType.DATA_FLOW, "XMLNodesFromVariableNestingElement.triggerStart", "bee|1", "rugby|2");
//        r.addReason(RelationType.DATA_FLOW, "NestingElement.triggerStart", "lemon|1", "isle|2");
//        r.addReason(RelationType.DATA_FLOW, "NestingElement.hasCompositeChildren", "lemon|2", "orange|1");
//        r.addReason(RelationType.SIMILARITY, "added element param", "rugby|1", "isle|1", "river|1", "coin|1");
//        r.addReason(RelationType.SIMILARITY, "added param to call", "atlantic|1", "track|1", "bee|1");
//        r.addReason(RelationType.CLASS_REFERENCE, "CompositeNestingElement", "rubin|1", "track|2", "orange|2", "river|2", "lemon|2");
//        r.addReason(RelationType.CLASS_REFERENCE, "LeafNestingElement", "rubin|1", "coin|2", "atlantic|2");
//        return r;
//    }

    private static void addRelationChain(StopRelationGraph g, TestRelationTypes type, String label, String... items) {
        for (int i = 1; i < items.length; i++) {
            g.addRelation(rel(type, label, items[i], items[i - 1]));
        }
    }

    private static void addRelationStar(StopRelationGraph g, TestRelationTypes type, String label, String center,
            String... rays) {
        for (final String ray : rays) {
            g.addRelation(rel(type, label, ray, center));
        }
    }

    private static void addRelationSymmetric(StopRelationGraph g, TestRelationTypes type, String label,
            String s1, String s2) {
        g.addRelation(rel(type, label, s2, s1));
        g.addRelation(rel(type, label, s1, s2));
    }

    private static Set<? extends Pattern> patterns() {
        return new LinkedHashSet<>(Arrays.asList(
                new GreedyTypePattern(TestRelationTypes.SAME_METHOD),
                new GreedyTypePattern(TestRelationTypes.DECLARATION_USE),
                new GreedyTypePattern(TestRelationTypes.DATA_FLOW),
                new GreedyTypePattern(TestRelationTypes.CALL_FLOW),
                new GreedyTypePattern(TestRelationTypes.SIMILARITY)));
    }

    private static Relation rel(RelationType type, String id, String from, String to) {
        return new Relation(type, id, stop(from), stop(to));
    }

    private static Stop stop(String id) {
        return new SimpleStop(id);
    }

}
