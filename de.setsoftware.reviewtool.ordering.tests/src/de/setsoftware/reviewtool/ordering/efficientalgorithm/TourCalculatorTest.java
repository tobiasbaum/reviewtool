package de.setsoftware.reviewtool.ordering.efficientalgorithm;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;

/**
 * Tests for {@link TourCalculator}.
 */
public class TourCalculatorTest {

    /**
     * Helper class to build the test data.
     */
    private static final class TourCalculatorInput {
        private final List<String> parts = new ArrayList<>();
        private final List<MatchSet<String>> matchSets = new ArrayList<>();
        private final List<PositionRequest<String>> positionRequests = new ArrayList<>();

        public static TourCalculatorInput tourCalculatorFor(String... parts) {
            final TourCalculatorInput ret = new TourCalculatorInput();
            ret.parts.addAll(Arrays.asList(parts));
            return ret;
        }

        public TourCalculatorInput matchChained(String... parts) {
            assert parts.length >= 2;
            for (int i = 0; i < parts.length - 1; i++) {
                this.match(parts[i], TargetPosition.FIRST, parts[i + 1]);
            }
            return this;
        }

        public TourCalculatorInput matchSymmetric(String... parts) {
            final Set<String> set = new TreeSet<>();
            set.addAll(Arrays.asList(parts));
            final MatchSet<String> ms = new MatchSet<>(set);
            this.matchSets.add(ms);
            return this;
        }

        public TourCalculatorInput match(String distinguishedElement, TargetPosition pos, String... others) {
            final Set<String> set = new TreeSet<>();
            set.add(distinguishedElement);
            set.addAll(Arrays.asList(others));
            final MatchSet<String> ms = new MatchSet<>(set);
            this.matchSets.add(ms);
            this.positionRequests.add(new PositionRequest<>(ms, distinguishedElement, pos));
            return this;
        }

        public TourCalculator<String> calculate() {
            return TourCalculator.calculateFor(this.parts, this.matchSets, this.positionRequests);
        }
    }

    @Test
    public void testRelatedTogether() {
        final TourCalculator<String> actual = TourCalculatorInput
                .tourCalculatorFor("callee", "other", "caller")
                .match("callee", TargetPosition.SECOND, "caller")
                .calculate();

        assertEquals(Arrays.asList("caller", "callee", "other"), actual.getTour());
    }

    @Test
    public void testRelatedTogether2() {
        final TourCalculator<String> actual = TourCalculatorInput
                .tourCalculatorFor("callee", "other1", "caller", "other2")
                .match("callee", TargetPosition.SECOND, "caller")
                .calculate();

        assertEquals(Arrays.asList("caller", "callee", "other1", "other2"), actual.getTour());
    }

    @Test
    public void testDeclUseAndCallFlow() {
        final TourCalculator<String> actual = TourCalculatorInput
                .tourCalculatorFor("useAndCallee", "decl", "caller")
                .match("decl", TargetPosition.FIRST, "useAndCallee")
                .match("useAndCallee", TargetPosition.SECOND, "caller")
                .calculate();

        assertEquals(Arrays.asList("decl", "useAndCallee", "caller"), actual.getTour());
    }

    @Test
    public void testMatchingWithClustering() {
        final TourCalculator<String> actual = TourCalculatorInput
                .tourCalculatorFor("declA", "useA", "declB", "useB", "declC", "useC", "commonCallee", "other")
                .match("declA", TargetPosition.FIRST, "useA")
                .match("declB", TargetPosition.FIRST, "useB")
                .match("declC", TargetPosition.FIRST, "useC")
                .match("commonCallee", TargetPosition.SECOND, "useA", "useB", "useC")
                .calculate();

        assertEquals(
                Arrays.asList("declB", "useB", "commonCallee", "declA", "useA", "useC", "declC", "other"),
                actual.getTour());
    }

    @Test
    public void testFurther() {
        final TourCalculator<String> actual = TourCalculatorInput
                .tourCalculatorFor("declA", "useA1", "useA2", "declB", "useB1", "useB2")
                .match("declA", TargetPosition.FIRST, "useA1", "useA2")
                .match("declB", TargetPosition.FIRST, "useB1", "useB2")
                .match("useA1", TargetPosition.SECOND, "useB1", "useB2")
                .calculate();

        assertEquals(
                Arrays.asList("declA", "useA2", "useA1", "useB1", "useB2", "declB"),
                actual.getTour());
    }

    @Test
    public void testGraphFromInterviews() {
        final TourCalculator<String> actual = TourCalculatorInput
                .tourCalculatorFor("Maus", "Birne", "Stern", "Homer", "Baum", "Strasse", "Buch")
                //addRelationStar(g, TestRelationTypes.SAME_METHOD, "determineChange()", "Strasse", "Baum");
                .match("Strasse", TargetPosition.FIRST, "Baum")
                //addRelationStar(g, TestRelationTypes.SAME_METHOD, "<init>()", "Stern", "Birne");
                .match("Stern", TargetPosition.FIRST, "Birne")
                //addRelationStar(g, TestRelationTypes.DECLARATION_USE, "att:maxTextDiffThreshold",
                //     "Homer", "Strasse", "Baum", "Birne");
                .match("Homer", TargetPosition.FIRST, "Strasse", "Baum", "Birne")
                //addRelationStar(g, TestRelationTypes.DECLARATION_USE, "par:maxTextDiffThreshold", "Stern", "Birne");
                .match("Stern", TargetPosition.FIRST, "Birne")
                //addRelationChain(g, TestRelationTypes.DATA_FLOW, "xml_param", "Buch", "Maus",
                //     "Stern", "Birne", "Strasse", "Baum");
                .matchChained("Buch", "Maus")
                //addRelationStar(g, TestRelationTypes.CALL_FLOW, "SvnChangeSource()", "Maus", "Stern");
                .match("Stern", TargetPosition.SECOND, "Maus")
                //addRelationSymmetric(g, TestRelationTypes.SIMILARITY, "foo", "Strasse", "Baum");
                .matchSymmetric("Strasse", "Baum")
                .calculate();

        assertEquals(
                Arrays.asList("Buch", "Maus", "Stern", "Birne", "Strasse", "Baum", "Homer"),
                actual.getTour());
    }

    private static void doTestWithGeneratedData(Random r) {
        final int size = r.nextInt(30) + 3;
        final int matches = r.nextInt(2 * size) + 1;
        doTestWithGeneratedData(r, size, matches);
    }

    private static void doTestWithGeneratedData(Random r, final int size, final int matches) {
        final List<String> ints = new ArrayList<>();
        for (int i = 1; i <= size; i++) {
            ints.add(Integer.toString(i));
        }
        final TourCalculatorInput b = TourCalculatorInput.tourCalculatorFor(ints.toArray(new String[ints.size()]));
        for (int round = 0; round < matches; round++) {
            Collections.shuffle(ints, r);
            final int setSize = 2 + r.nextInt(ints.size() - 2);
            final String[] set = ints.subList(0, setSize).toArray(new String[setSize]);
            TargetPosition pos;
            switch (r.nextInt(3)) {
            case 0:
                pos = TargetPosition.FIRST;
                break;
            case 1:
                pos = TargetPosition.SECOND;
                break;
            case 2:
                pos = TargetPosition.LAST;
                break;
            default:
                throw new AssertionError();
            }
            b.match(set[0], pos, Arrays.copyOfRange(set, 1, set.length));
        }
        final TourCalculator<String> result = b.calculate();
        assertEquals(result.getTour().size(), ints.size());
    }

    @Test
    public void testSmokeTest() {
        for (int i = 0; i < 100; i++) {
            try {
                doTestWithGeneratedData(new Random(i));
            } catch (final AssertionError e) {
                throw new AssertionError("problem with seed " + i, e);
            }
        }
    }

//TEST
//    @Test
//    public void testPerformance() {
//        final List<String> allChangeParts = new ArrayList<>();
//        final List<MatchSet<String>> matchSets = new ArrayList<>();
//        for (int i = 0; i < 200; i++) {
//            final String a = "p" + i + "_A";
//            final String b = "p" + i + "_B";
//            final String c = "p" + i + "_C";
//            allChangeParts.add(a);
//            allChangeParts.add(b);
//            allChangeParts.add(c);
//            matchSets.add(new MatchSet<>(new HashSet<String>(Arrays.asList(a, b, c))));
//        }
//        for (int i = 0; i < allChangeParts.size() / 3; i++) {
//            for (int j = i + 1; j < allChangeParts.size() / 3; j++) {
//                matchSets.add(new MatchSet<>(new HashSet<String>(Arrays.asList(
//                        allChangeParts.get(3 * i),
//                        allChangeParts.get(3 * j)))));
//            }
//        }
//
//        final List<PositionRequest<String>> positionRequests = new ArrayList<>();
//        System.out.println("init done");
//        final long start = System.currentTimeMillis();
//        final TourCalculator<String> t = TourCalculator.calculateFor(allChangeParts, matchSets, positionRequests);
//        final long end = System.currentTimeMillis();
//        System.out.println("elapsed: " + (end - start));
//    }

}
