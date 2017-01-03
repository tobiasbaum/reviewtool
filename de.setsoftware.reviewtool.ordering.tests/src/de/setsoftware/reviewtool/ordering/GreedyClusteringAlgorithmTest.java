package de.setsoftware.reviewtool.ordering;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.Test;

import de.setsoftware.reviewtool.ordering.basealgorithm.Tour;
import de.setsoftware.reviewtool.ordering.relationtypes.RelationType;

public class GreedyClusteringAlgorithmTest {

    private static SimpleRelatednessInfo getMaxDiffThresholdTestData(RelationType... relationPreferences) {
        final Set<String> stops = new LinkedHashSet<>(Arrays.asList(
                "Maus", "Birne", "Stern", "Homer", "Baum", "Strasse", "Buch"));

        final SimpleRelatednessInfo r = new SimpleRelatednessInfo(stops, relationPreferences);

        r.addReason(RelationType.SAME_METHOD, "determineChange()", "Strasse|1", "Baum|2");
        r.addReason(RelationType.SAME_METHOD, "<init>()", "Stern|1", "Birne|2");
        r.addReason(RelationType.DECLARATION_USE, "att:maxTextDiffThreshold", "Homer|1", "Strasse|2", "Baum|2", "Birne|2");
        r.addReason(RelationType.DECLARATION_USE, "par:maxTextDiffThreshold", "Stern|1", "Birne|2");
        r.addReason(RelationType.DATA_FLOW, "xml_param", "Buch|1", "Maus|2", "Stern|4", "Birne|8", "Strasse|16", "Baum|32");
        r.addReason(RelationType.CALL_FLOW, "SvnChangeSource()", "Maus|1", "Stern|2");
        r.addReason(RelationType.SIMILARITY, "foo", "Strasse|1", "Baum|1");
        return r;
    }

    private static SimpleRelatednessInfo getPsy6663TestData(RelationType... relationPreferences) {
        final Set<String> stops = new LinkedHashSet<>(Arrays.asList(
                "trumpet", "time", "rock", "raven", "house", "golf", "dog", "city", "apple", "air"));

        final SimpleRelatednessInfo r = new SimpleRelatednessInfo(stops, relationPreferences);

        r.addReason(RelationType.SAME_FILE, "DocumentPoolClassSelectionMapper", "apple|1", "dog|2", "trumpet|4", "time|8");
        r.addReason(RelationType.SAME_FILE, "DocumentPoolClassSelectionMapperRegistry", "city|1");
        r.addReason(RelationType.SAME_FILE, "FromPoolPullOutMapper", "rock|1");
        r.addReason(RelationType.SAME_FILE, "PullOutDocumentsFromPoolRunMapper", "raven|1");
        r.addReason(RelationType.SAME_FILE, "pool.xsd", "house|1");
        r.addReason(RelationType.SAME_FILE, "basicModules.xml", "golf|1");
        r.addReason(RelationType.SAME_FILE, "SchemaLicence", "air|1");
        r.addReason(RelationType.LOGICAL_DEPENDENCY, "Activatable", "apple|1", "air|2", "dog|2", "time|2");
        r.addReason(RelationType.LOGICAL_DEPENDENCY, "Mapper use", "golf|1", "apple|2");
        //going bottom-up in call flow
        r.addReason(RelationType.CALL_FLOW, "Mapper.mapSelection", "city|2", "trumpet|1");
        r.addReason(RelationType.CALL_FLOW, "Registry.mapSelection", "raven|2", "rock|2", "city|1");
        r.addReason(RelationType.CALL_FLOW, "Registry.register", "time|2", "city|1");
        //no direction preference in data flow because data flows in as well as out
        r.addReason(RelationType.DATA_FLOW, "Mapper.mapSelection", "city|1", "trumpet|1");
        r.addReason(RelationType.DATA_FLOW, "Registry.mapSelection", "raven|1", "rock|1", "city|1");
        r.addReason(RelationType.DATA_FLOW, "Registry.register", "time|1", "city|1");
        r.addReason(RelationType.SIMILARITY, "Mapper->Registry", "raven|1", "rock|1");
        return r;
    }

    private static SimpleRelatednessInfo getPsy6561TestData(RelationType... relationPreferences) {
        final Set<String> stops = new LinkedHashSet<>(Arrays.asList(
                "rubin", "rugby", "bee", "track", "orange", "river", "lemon", "coin", "atlantic", "isle"));

        final SimpleRelatednessInfo r = new SimpleRelatednessInfo(stops, relationPreferences);

        r.addReason(RelationType.SAME_FILE, "XMLWriterTest", "rubin|1");
        r.addReason(RelationType.SAME_FILE, "XMLNodesFromVariableNestingElement", "bee|1", "rugby|2");
        r.addReason(RelationType.SAME_FILE, "CompositeNestingElement", "track|1", "orange|2", "river|4", "lemon|8");
        r.addReason(RelationType.SAME_FILE, "LeafNestingElement", "coin|1", "atlantic|2");
        r.addReason(RelationType.SAME_FILE, "NestingElement", "isle|1");
        r.addReason(RelationType.OVERRIDES, "triggerStart", "isle|1", "coin|2", "river|2", "rugby|2");
        r.addReason(RelationType.SAME_METHOD, "CompositeNestingElement.triggerStart", "river|1", "lemon|2");
        //going top-down in call flow
        r.addReason(RelationType.CALL_FLOW, "LeafNestingElement.triggerStart", "atlantic|1", "coin|2");
        r.addReason(RelationType.CALL_FLOW, "CompositeNestingElement.triggerStart", "track|1", "river|2");
        r.addReason(RelationType.CALL_FLOW, "XMLNodesFromVariableNestingElement.triggerStart", "bee|1", "rugby|2");
        r.addReason(RelationType.CALL_FLOW, "NestingElement.triggerStart", "lemon|1", "isle|2");
        r.addReason(RelationType.CALL_FLOW, "NestingElement.hasCompositeChildren", "lemon|1", "orange|2");
        r.addReason(RelationType.DATA_FLOW, "LeafNestingElement.triggerStart", "atlantic|1", "coin|2");
        r.addReason(RelationType.DATA_FLOW, "CompositeNestingElement.triggerStart", "track|1", "river|2");
        r.addReason(RelationType.DATA_FLOW, "XMLNodesFromVariableNestingElement.triggerStart", "bee|1", "rugby|2");
        r.addReason(RelationType.DATA_FLOW, "NestingElement.triggerStart", "lemon|1", "isle|2");
        r.addReason(RelationType.DATA_FLOW, "NestingElement.hasCompositeChildren", "lemon|2", "orange|1");
        r.addReason(RelationType.SIMILARITY, "added element param", "rugby|1", "isle|1", "river|1", "coin|1");
        r.addReason(RelationType.SIMILARITY, "added param to call", "atlantic|1", "track|1", "bee|1");
        r.addReason(RelationType.CLASS_REFERENCE, "CompositeNestingElement", "rubin|1", "track|2", "orange|2", "river|2", "lemon|2");
        r.addReason(RelationType.CLASS_REFERENCE, "LeafNestingElement", "rubin|1", "coin|2", "atlantic|2");
        return r;
    }

    @Test
    public void testExampleFromExperimentMostCommon() {
        final SimpleRelatednessInfo r = getMaxDiffThresholdTestData(
                RelationType.SAME_METHOD,
                RelationType.DECLARATION_USE,
                RelationType.CALL_FLOW,
                RelationType.DATA_FLOW);

        final Tour<String> bestTour = GreedyClusteringAlgorithm.determineBestTour(r.getStops(), r);
        assertEquals("[Buch, Maus, Homer, Stern, Birne, Strasse, Baum]", bestTour.toString());
    }

    @Test
    public void testExampleFromExperimentExp08() {
        final SimpleRelatednessInfo r = getMaxDiffThresholdTestData(
                RelationType.GLOBAL_ORDER,
                RelationType.SAME_METHOD,
                RelationType.DECLARATION_USE,
                RelationType.DATA_FLOW);
        r.addReason(RelationType.GLOBAL_ORDER, "attributeFirst",
                "Maus|2", "Homer|1", "Stern|2", "Birne|2", "Strasse|2", "Baum|2", "Buch|2");

        final Tour<String> bestTour = GreedyClusteringAlgorithm.determineBestTour(r.getStops(), r);
        assertEquals("[Homer, Stern, Birne, Strasse, Baum, Buch, Maus]", bestTour.toString());
    }

    @Test
    public void testExampleFromPsy6663() {
        final SimpleRelatednessInfo r = getPsy6663TestData(
                RelationType.SAME_METHOD,
                RelationType.DECLARATION_USE,
                RelationType.CALL_FLOW,
                RelationType.DATA_FLOW,
                RelationType.LOGICAL_DEPENDENCY,
                RelationType.SAME_FILE);

        final Tour<String> bestTour = GreedyClusteringAlgorithm.determineBestTour(r.getStops(), r);
        //this is not one of the orderings from the experiment, but it is a reasonable ordering imho
        assertEquals("[house, golf, apple, dog, trumpet, city, rock, raven, time, air]", bestTour.toString());
    }

    @Test
    public void testExampleFromPsy6561() {
        final SimpleRelatednessInfo r = getPsy6561TestData(
                RelationType.SAME_METHOD,
                RelationType.DECLARATION_USE,
                RelationType.CALL_FLOW,
                RelationType.DATA_FLOW,
                RelationType.OVERRIDES,
                RelationType.LOGICAL_DEPENDENCY,
                RelationType.SIMILARITY,
                RelationType.SAME_FILE,
                RelationType.CLASS_REFERENCE);

        final Tour<String> bestTour = GreedyClusteringAlgorithm.determineBestTour(r.getStops(), r);
        //this is not one of the orderings from the experiment, but it is a reasonable ordering imho
        assertEquals("[rubin, track, river, lemon, orange, isle, atlantic, coin, bee, rugby]", bestTour.toString());
    }

}
