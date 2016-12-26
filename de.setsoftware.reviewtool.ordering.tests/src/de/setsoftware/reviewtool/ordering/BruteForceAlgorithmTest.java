package de.setsoftware.reviewtool.ordering;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.Test;

import de.setsoftware.reviewtool.ordering.basealgorithm.Tour;
import de.setsoftware.reviewtool.ordering.basealgorithm.TourGoodnessOrder;
import de.setsoftware.reviewtool.ordering.relationtypes.RelatednessVector;
import de.setsoftware.reviewtool.ordering.relationtypes.RelationType;

public class BruteForceAlgorithmTest {

    @Test
    public void testSimpleExample() {
        final Set<String> stops = new LinkedHashSet<>(Arrays.asList(
                "s1", "s2", "s3"));

        final SimpleRelatednessFunction r = new SimpleRelatednessFunction(
                RelationType.DECLARATION_USE,
                RelationType.CALL_FLOW,
                RelationType.SIMILARITY);

        r.relatedBinary(RelationType.DECLARATION_USE, "s1", "s2");
        r.relatedBinary(RelationType.CALL_FLOW, "s2", "s3");

        final Set<Tour<String>> bestTours = BruteForceAlgorithm.determineBestTours(stops, r);
        print("simple example", bestTours);
    }

    @Test
    public void testExampleFromExperiment() {
        final Set<String> stops = new LinkedHashSet<>(Arrays.asList(
                "Maus", "Birne", "Stern", "Homer", "Baum", "Strasse", "Buch"));

        final SimpleRelatednessFunction r = new SimpleRelatednessFunction(
                RelationType.SAME_METHOD,
                RelationType.DECLARATION_USE,
                RelationType.DATA_FLOW,
                RelationType.CALL_FLOW,
                RelationType.SIMILARITY);

        r.relatedBinary(RelationType.SAME_METHOD, "Strasse", "Baum");
        r.relatedBinary(RelationType.SAME_METHOD, "Stern", "Birne");
        r.relatedBinary(RelationType.DECLARATION_USE, "Homer", "Strasse");
        r.relatedBinary(RelationType.DECLARATION_USE, "Homer", "Baum");
        r.relatedBinary(RelationType.DECLARATION_USE, "Homer", "Birne");
        r.relatedBinary(RelationType.DECLARATION_USE, "Stern", "Birne");
        r.relatedBinary(RelationType.DATA_FLOW, "Buch", "Maus");
        r.relatedBinary(RelationType.DATA_FLOW, "Maus", "Stern");
        r.relatedBinary(RelationType.DATA_FLOW, "Birne", "Strasse");
        r.relatedBinary(RelationType.DATA_FLOW, "Birne", "Baum");
        r.relatedBinary(RelationType.CALL_FLOW, "Maus", "Stern");
        r.relatedGradual(RelationType.SIMILARITY, "Strasse", "Baum", 0.8);

        final TourGoodnessOrder<String,RelatednessVector> goodnessOrder = new TourGoodnessOrder<>(r);
        System.out.println(goodnessOrder.isLessOrEquals(
                Tour.of(Arrays.asList("Maus", "Birne", "Stern", "Homer", "Baum", "Strasse", "Buch")),
                Tour.of(Arrays.asList("Buch", "Maus", "Birne", "Stern", "Homer", "Baum", "Strasse"))));
        System.out.println(goodnessOrder.isLessOrEquals(
                Tour.of(Arrays.asList("Buch", "Maus", "Birne", "Stern", "Homer", "Baum", "Strasse")),
                Tour.of(Arrays.asList("Maus", "Birne", "Stern", "Homer", "Baum", "Strasse", "Buch"))));
        System.out.println(goodnessOrder.isLessOrEquals(
                Tour.of(Arrays.asList("Buch", "Maus", "Birne", "Stern", "Baum", "Homer", "Strasse")),
                Tour.of(Arrays.asList("Buch", "Maus", "Birne", "Stern", "Homer", "Baum", "Strasse"))));
        System.out.println(goodnessOrder.isLessOrEquals(
                Tour.of(Arrays.asList("Buch", "Maus", "Birne", "Stern", "Homer", "Baum", "Strasse")),
                Tour.of(Arrays.asList("Buch", "Maus", "Birne", "Stern", "Baum", "Homer", "Strasse"))));

        final Set<Tour<String>> bestTours = BruteForceAlgorithm.determineBestTours(stops, r);
        print("from experiment", bestTours);
    }

    private static void print(String head, Set<Tour<String>> bestTours) {
        System.out.println("== " + head);
        for (final Tour<String> t : bestTours) {
            System.out.println(t);
        }
        System.out.println();
    }

}
