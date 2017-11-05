package de.setsoftware.reviewtool.ordering.efficientalgorithm;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Ignore;
import org.junit.Test;

/**
 * Not a real test: Used to determine the optimal orders for the experiment.
 */
@Ignore
public class ExperimentDataTest {

    /**
     * Helper class to build input for the tour calculator.
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
            if (parts.length <= 1) {
                return this;
            }
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

        public TourCalculator<String> calculate() throws Exception {
            return TourCalculator.calculateFor(
                    this.parts,
                    this.matchSets,
                    this.positionRequests,
                    new CancelCallback() {
                        @Override
                        public boolean isCanceled() {
                            return false;
                        }
                    });
        }
    }

    private static void fileRelationsC1(TourCalculatorInput t) {
        t
            //in same file
            .matchSymmetric("a", "b", "c", "d") //VFSBrowser
            .matchSymmetric("e", "f", "g") //AbstractBrowserTask
            .matchSymmetric("h") //DeleteBrowserTask
            .matchSymmetric("i") //MkDirBrowserTask
            .matchSymmetric("j") //RenameBrowserTask
            .matchSymmetric("k", "l", "m", "n") //ListDirectoryBrowserTask
            .matchSymmetric("o"); //BrowserIORequest
    }

    private static void relationsC1(TourCalculatorInput t) {
        t
            //similarity
            .matchChained("o", "h") //moved code from BrowserIORequest to DeleteBrowserTask
            .matchChained("o", "i") //moved code from BrowserIORequest to MkDirBrowserTask
            .matchChained("o", "j") //moved code from BrowserIORequest to RenameBrowserTask
            .matchChained("o", "m") //moved code from BrowserIORequest to ListDirectoryBrowserTask?
            .matchChained("i", "j") //token jaccard similarity > 0.7
            .matchChained("h", "j") //token jaccard similarity > 0.7
            .matchChained("i", "h") //token jaccard similarity > 0.7
            .matchChained("b", "c") //token jaccard similarity > 0.7

            //declare & use
            .match("l", TargetPosition.FIRST, "k", "m") //loadInfo attribute
            .match("g", TargetPosition.FIRST, "f", "h", "j", "m") //path attribute
            .match("g", TargetPosition.FIRST, "f", "m") //removed load info attribute in old code

            //class hierarchy
            .matchSymmetric("h", "i", "j", "m") //overriding _run() (the abstract method itself is not contained)

            //call flow
            .match("h", TargetPosition.SECOND, "a") //to DeleteBrowserTask constructor
            .match("j", TargetPosition.SECOND, "b", "c") //to RenameBrowserTask constructor
            .match("i", TargetPosition.SECOND, "d") //to MkDirBrowserTask constructor
            .match("f", TargetPosition.SECOND, "h", "i", "j", "k") //to AbstractBrowserTask constructor
            .match("f", TargetPosition.SECOND, "e") //to other AbstractBrowserTask constructor in old code
            .match("o", TargetPosition.SECOND, "a", "b", "c", "d") //to BrowserIORequest constructor in old code

            //data flow
            //        .matchChained("a", "h") //to DeleteBrowserTask constructor
            //        .matchChained("a", "o") //to BrowserIORequest constructor in old code
            //        .matchChained("b", "j") //to RenameBrowserTask constructor
            //        .matchChained("b", "o") //to BrowserIORequest constructor in old code
            //        .matchChained("c", "j") //to RenameBrowserTask constructor
            //        .matchChained("c", "o") //to BrowserIORequest constructor in old code
            //        .matchChained("d", "i") //to MkDirBrowserTask constructor
            //        .matchChained("d", "o") //to BrowserIORequest constructor in old code
            //        .matchChained("e", "f") //to other AbstractBrowserTask constructor in old code
            //        .matchChained("h", "f") //to AbstractBrowserTask constructor (and setting atts)
            //        .matchChained("i", "f") //to AbstractBrowserTask constructor (and setting atts)
            //        .matchChained("j", "f") //to AbstractBrowserTask constructor (and setting atts)
            //        .matchChained("k", "f") //to AbstractBrowserTask constructor
            //        .matchChained("k", "m") //setting loadInfo attribute
            //        .matchChained("f", "m") //setting path attribute and others
            //        .matchChained("f", "n") //setting path attribute and others

            //order inside files
            .matchChained("a", "b", "c", "d") //VFSBrowser
            .matchChained("e", "f", "g") //AbstractBrowserTask
            .matchChained("l", "k", "m", "n"); //ListDirectoryBrowserTask

            //common identifier: not used
            //logical dependency: cannot use, we don't know about it
            //file type: does not apply, all java files
            //development flow: cannot use, we don't know about it
    }

    @Test
    public void testC1OptimalFiles() throws Exception {
        final TourCalculatorInput input = TourCalculatorInput
                .tourCalculatorFor("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o");

        fileRelationsC1(input);
        relationsC1(input);

        System.out.println(input.calculate().getTour());
    }

    @Test
    public void testC1OptimalNoFiles() throws Exception {
        final TourCalculatorInput input = TourCalculatorInput
                .tourCalculatorFor("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o");

        relationsC1(input);

        System.out.println(input.calculate().getTour());
    }

    @Test
    public void testC1WorstNoFiles() throws Exception {
        final TourCalculatorInput input = TourCalculatorInput
                .tourCalculatorFor("k", "c", "e", "d", "g", "n", "h", "i", "j", "l", "o", "a", "f", "b", "m");

        fileRelationsC1(input);
        relationsC1(input);

        System.out.println(this.determineWorst(input));
    }

    @Test
    public void testC1WorstFiles() throws Exception {
        final TourCalculatorInput input = TourCalculatorInput
                .tourCalculatorFor("i", "j", "k", "n", "l", "m", "b", "d", "a", "c", "e", "g", "f", "o", "h");
        relationsC1(input);

        final TourCalculatorInput toKeep = TourCalculatorInput
                .tourCalculatorFor("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o");
        fileRelationsC1(toKeep);

        System.out.println(this.determineWorst(input, toKeep.matchSets));
    }

    private static void fileRelationsC2(TourCalculatorInput t) {
        t
            //in same file
            .matchSymmetric("a") //SearchDialog
            .matchSymmetric("b", "c", "d") //HyperSearchOperationNode
            .matchSymmetric("e", "f", "g") //SearchAndReplace
            .matchSymmetric("h", "i", "j") //PatternSearchMatcher
            .matchSymmetric("k", "l", "m") //BoyerMooreSearchMatcher
            .matchSymmetric("n") //HyperSearchRequest
            .matchSymmetric("o", "p", "q", "r", "s", "t", "u") //SearchMatcher
            ;
    }

    private static void relationsC2(TourCalculatorInput t) {
        t
            //similarity
            .matchSymmetric("f", "g", "n") //setting of noWordSep in matcher

            //declare & use
            .match("b", TargetPosition.FIRST, "c", "d")
            .match("s", TargetPosition.FIRST, "m", "l", "j", "h") //wholeWord
            .match("u", TargetPosition.FIRST, "r", "p", "o") //noWordSep

            //class hierarchy
            .matchSymmetric("j", "m") //overriding nextMatch() (the abstract method itself is not contained in change)

            //call flow
            .match("h", TargetPosition.SECOND, "e", "i") //PatternSearchMatcher constructor with flag
            .match("l", TargetPosition.SECOND, "e", "k") //BoyerMooreSearchMatcher constructor with flag
            .match("p", TargetPosition.SECOND, "c") //matcher.getNoWordSep()
            .match("o", TargetPosition.SECOND, "d", "f", "g", "n") //matcher.setNoWordSep()
            .match("q", TargetPosition.SECOND, "m") //isWholeWord()
            .match("r", TargetPosition.SECOND, "q") //isEndWord()

            //order inside files
            .matchChained("b", "c", "d") //HyperSearchOperationNode
            .matchChained("e", "f", "g") //SearchAndReplace
            .matchChained("h", "i", "j") //PatternSearchMatcher
            .matchChained("k", "l", "m") //BoyerMooreSearchMatcher
            .matchChained("o", "p", "q", "r", "s", "u", "t") //SearchMatcher

            //data flow: not used, mostly covered by data flow
            //common identifier: not used
            //logical dependency: cannot use, we don't know about it
            //file type: does not apply, all java files
            //development flow: cannot use, we don't know about it
            ;
    }

    @Test
    public void testC2OptimalFiles() throws Exception {
        final TourCalculatorInput input = TourCalculatorInput.tourCalculatorFor(
                "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m",
                "n", "o", "p", "q", "r", "s", "t", "u");

        fileRelationsC2(input);
        relationsC2(input);

        System.out.println(input.calculate().getTour());
    }

    @Test
    public void testC2OptimalNoFiles() throws Exception {
        final TourCalculatorInput input = TourCalculatorInput.tourCalculatorFor(
                "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m",
                "n", "o", "p", "q", "r", "s", "t", "u");

        relationsC2(input);

        System.out.println(input.calculate().getTour());
    }

    @Test
    public void testC2WorstNoFiles() throws Exception {
        final TourCalculatorInput input = TourCalculatorInput.tourCalculatorFor(
                "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m",
                "n", "o", "p", "q", "r", "s", "t", "u");

        fileRelationsC2(input);
        relationsC2(input);

        System.out.println(this.determineWorst(input));
    }

    @Test
    public void testC2WorstFiles() throws Exception {
        final TourCalculatorInput input = TourCalculatorInput.tourCalculatorFor(
                "g", "f", "e", "r", "o", "q", "p", "s", "t", "u", "a", "d", "b", "c",
                "h", "j", "i", "n", "k", "m", "l");
        relationsC2(input);

        final TourCalculatorInput toKeep = TourCalculatorInput.tourCalculatorFor(
                "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n",
                "o", "p", "q", "r", "s", "t", "u");
        fileRelationsC2(toKeep);

        System.out.println(this.determineWorst(input, toKeep.matchSets));
    }




    private List<String> determineWorst(TourCalculatorInput input) throws IOException {
        return this.determineWorst(input, Collections.<MatchSet<String>>emptyList());
    }

    private List<String> determineWorst(TourCalculatorInput input, List<MatchSet<String>> toKeep) throws IOException {
        List<String> cur = input.parts;
        int cnt = 0;
        while (true) {
            final MatchSet<String> toInvalidate = this.determineMatchingSet(cur, input.matchSets, cnt);
            if (toInvalidate == null) {
                return cur;
            }
            final List<String> next = this.invalidate(cur, toInvalidate, input.matchSets, toKeep);
            if (next == null) {
                cnt++;
            } else {
                cnt = 0;
                cur = next;
            }
        }
    }

    private List<String> invalidate(
            List<String> cur,
            MatchSet<String> toInvalidate,
            List<MatchSet<String>> matchSets,
            List<MatchSet<String>> toKeep)
        throws IOException {

        final List<MatchSet<String>> nonMatches = new ArrayList<>();
        for (final MatchSet<String> ms : matchSets) {
            if (!this.matches(ms, cur)) {
                nonMatches.add(ms);
            }
        }
        loop: while (true) {
            System.out.println("Please invalidate " + toInvalidate.getChangeParts() + " in " + cur);
            System.out.println("without matching " + nonMatches);
            final BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            final String newOrder = br.readLine();
            if (newOrder.isEmpty()) {
                return null;
            }
            final List<String> l = Arrays.asList(newOrder.replace(" ", "").split(","));

            if (l.size() != cur.size()) {
                System.out.println("invalid size of input " + l);
                continue loop;
            }
            if (this.matches(toInvalidate, l)) {
                System.out.println("does not invalidate");
                continue loop;
            }
            for (final MatchSet<String> ms : nonMatches) {
                if (this.matches(ms, l)) {
                    System.out.println("made " + ms.getChangeParts() + " match");
                    continue loop;
                }
            }
            for (final MatchSet<String> ms : toKeep) {
                if (!this.matches(ms, l)) {
                    System.out.println("did not keep " + ms.getChangeParts());
                    continue loop;
                }
            }
            return l;
        }
    }

    private MatchSet<String> determineMatchingSet(List<String> cur, List<MatchSet<String>> matchSets, int cnt) {
        for (final MatchSet<String> ms : matchSets) {
            if (this.matches(ms, cur)) {
                if (cnt == 0) {
                    return ms;
                } else {
                    cnt--;
                }
            }
        }
        return null;
    }

    private boolean matches(MatchSet<String> ms, List<String> cur) {
        boolean foundStart = false;
        boolean foundEnd = false;
        for (final String s : cur) {
            if (foundStart) {
                if (foundEnd) {
                    if (ms.getChangeParts().contains(s)) {
                        return false;
                    }
                } else {
                    if (!ms.getChangeParts().contains(s)) {
                        foundEnd = true;
                    }
                }
            } else {
                if (ms.getChangeParts().contains(s)) {
                    foundStart = true;
                }
            }
        }
        return true;
    }

    @Test
    public void testOrdersC1() {
        final TourCalculatorInput input = TourCalculatorInput
                .tourCalculatorFor("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o");

        fileRelationsC1(input);
        relationsC1(input);

        final List<String> onf = Arrays.asList(
                "d", "c", "b", "j", "i", "o", "h", "a", "e", "f", "g", "m", "k", "l", "n");
        final List<String> of = Arrays.asList(
                "e", "f", "g", "m", "k", "l", "n", "j", "i", "o", "h", "a", "b", "c", "d");
        final List<String> wf = Arrays.asList(
                "i", "k", "n", "l", "m", "j", "b", "d", "a", "c", "h", "e", "g", "f", "o");
        final List<String> wnf = Arrays.asList(
                "i", "n", "h", "g", "d", "e", "c", "k", "j", "l", "o", "a", "f", "b", "m");

        assertTrue(Relation.isBetterThanOrEqual(onf, onf, input.matchSets, input.positionRequests));
        assertFalse(Relation.isBetterThanOrEqual(onf, of, input.matchSets, input.positionRequests));
        assertFalse(Relation.isBetterThanOrEqual(onf, wf, input.matchSets, input.positionRequests));
        assertTrue(Relation.isBetterThanOrEqual(onf, wnf, input.matchSets, input.positionRequests));

        assertFalse(Relation.isBetterThanOrEqual(of, onf, input.matchSets, input.positionRequests));
        assertTrue(Relation.isBetterThanOrEqual(of, of, input.matchSets, input.positionRequests));
        assertTrue(Relation.isBetterThanOrEqual(of, wf, input.matchSets, input.positionRequests));
        assertTrue(Relation.isBetterThanOrEqual(of, wnf, input.matchSets, input.positionRequests));

        assertFalse(Relation.isBetterThanOrEqual(wf, onf, input.matchSets, input.positionRequests));
        assertFalse(Relation.isBetterThanOrEqual(wf, of, input.matchSets, input.positionRequests));
        assertTrue(Relation.isBetterThanOrEqual(wf, wf, input.matchSets, input.positionRequests));
        assertTrue(Relation.isBetterThanOrEqual(wf, wnf, input.matchSets, input.positionRequests));

        assertFalse(Relation.isBetterThanOrEqual(wnf, onf, input.matchSets, input.positionRequests));
        assertFalse(Relation.isBetterThanOrEqual(wnf, of, input.matchSets, input.positionRequests));
        assertFalse(Relation.isBetterThanOrEqual(wnf, wf, input.matchSets, input.positionRequests));
        assertTrue(Relation.isBetterThanOrEqual(wnf, wnf, input.matchSets, input.positionRequests));
    }

    @Test
    public void testOrdersC2() {
        final TourCalculatorInput input = TourCalculatorInput.tourCalculatorFor(
                "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n",
                "o", "p", "q", "r", "s", "t", "u");

        fileRelationsC2(input);
        relationsC2(input);

        final List<String> onf = Arrays.asList(
                "a", "k", "t", "d", "b", "c", "p", "o", "u", "r", "q", "m", "j", "l",
                "s", "h", "i", "e", "f", "g", "n");
        final List<String> of = Arrays.asList(
                "d", "b", "c", "p", "o", "u", "r", "q", "t", "s", "k", "l", "m", "j",
                "i", "h", "e", "f", "g", "n", "a");
        final List<String> wf = Arrays.asList(
                "g", "e", "f", "r", "o", "q", "u", "p", "s", "t", "a", "d", "b", "c",
                "h", "j", "i", "n", "k", "m", "l");
        final List<String> wnf = Arrays.asList(
                "r", "o", "q", "a", "d", "g", "p", "b", "h", "j", "k", "n", "s", "t",
                "c", "f", "i", "m", "e", "l", "u");

        assertTrue(Relation.isBetterThanOrEqual(onf, onf, input.matchSets, input.positionRequests));
        assertFalse(Relation.isBetterThanOrEqual(onf, of, input.matchSets, input.positionRequests));
        assertFalse(Relation.isBetterThanOrEqual(onf, wf, input.matchSets, input.positionRequests));
        assertTrue(Relation.isBetterThanOrEqual(onf, wnf, input.matchSets, input.positionRequests));

        assertFalse(Relation.isBetterThanOrEqual(of, onf, input.matchSets, input.positionRequests));
        assertTrue(Relation.isBetterThanOrEqual(of, of, input.matchSets, input.positionRequests));
        assertTrue(Relation.isBetterThanOrEqual(of, wf, input.matchSets, input.positionRequests));
        assertTrue(Relation.isBetterThanOrEqual(of, wnf, input.matchSets, input.positionRequests));

        assertFalse(Relation.isBetterThanOrEqual(wf, onf, input.matchSets, input.positionRequests));
        assertFalse(Relation.isBetterThanOrEqual(wf, of, input.matchSets, input.positionRequests));
        assertTrue(Relation.isBetterThanOrEqual(wf, wf, input.matchSets, input.positionRequests));
        assertTrue(Relation.isBetterThanOrEqual(wf, wnf, input.matchSets, input.positionRequests));

        assertFalse(Relation.isBetterThanOrEqual(wnf, onf, input.matchSets, input.positionRequests));
        assertFalse(Relation.isBetterThanOrEqual(wnf, of, input.matchSets, input.positionRequests));
        assertFalse(Relation.isBetterThanOrEqual(wnf, wf, input.matchSets, input.positionRequests));
        assertTrue(Relation.isBetterThanOrEqual(wnf, wnf, input.matchSets, input.positionRequests));
    }

}
