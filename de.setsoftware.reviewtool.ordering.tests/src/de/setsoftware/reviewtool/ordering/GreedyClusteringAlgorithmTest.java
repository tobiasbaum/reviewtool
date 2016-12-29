package de.setsoftware.reviewtool.ordering;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.Test;

import de.setsoftware.reviewtool.ordering.basealgorithm.Tour;
import de.setsoftware.reviewtool.ordering.relationtypes.RelationType;

public class GreedyClusteringAlgorithmTest {

    @Test
    public void testExampleFromExperiment() {
        final Set<String> stops = new LinkedHashSet<>(Arrays.asList(
                "Maus", "Birne", "Stern", "Homer", "Baum", "Strasse", "Buch"));

        final SimpleRelatednessInfo r = new SimpleRelatednessInfo(
                stops,
                RelationType.SAME_METHOD,
                RelationType.DECLARATION_USE,
                RelationType.DATA_FLOW,
                RelationType.CALL_FLOW,
                RelationType.SIMILARITY);

        r.addReason(RelationType.SAME_METHOD, "determineChange()", "Strasse|1", "Baum|2");
        r.addReason(RelationType.SAME_METHOD, "<init>()", "Stern|1", "Birne|2");
        r.addReason(RelationType.DECLARATION_USE, "att:maxTextDiffThreshold", "Homer|1", "Strasse|2", "Baum|2", "Birne|2");
        r.addReason(RelationType.DECLARATION_USE, "par:maxTextDiffThreshold", "Stern|1", "Birne|2");
        r.addReason(RelationType.DATA_FLOW, "xml_param", "Buch|1", "Maus|2", "Stern|4", "Birne|8", "Strasse|16", "Baum|32");
        r.addReason(RelationType.CALL_FLOW, "SvnChangeSource()", "Maus|1", "Stern|2");
        r.addReason(RelationType.SIMILARITY, "foo", "Strasse|1", "Baum|1");

        final Tour<String> bestTour = GreedyClusteringAlgorithm.determineBestTour(stops, r);
        System.out.println(bestTour);
        assertEquals("[Buch, Maus, Homer, Stern, Birne, Strasse, Baum]", bestTour.toString());
    }

}
