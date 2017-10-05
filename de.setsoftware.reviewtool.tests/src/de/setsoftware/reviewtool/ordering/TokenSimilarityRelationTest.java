package de.setsoftware.reviewtool.ordering;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.junit.Test;

import de.setsoftware.reviewtool.model.api.IFragment;
import de.setsoftware.reviewtool.model.api.IRevisionedFile;
import de.setsoftware.reviewtool.model.changestructure.ChangestructureFactory;
import de.setsoftware.reviewtool.model.changestructure.Fragment;
import de.setsoftware.reviewtool.model.changestructure.Stop;
import de.setsoftware.reviewtool.model.changestructure.StubRepo;

/**
 * Test cases for {@link TokenSimilarityRelation}.
 */
public class TokenSimilarityRelationTest {

    private static IRevisionedFile file(String name, int revision) {
        return ChangestructureFactory.createFileInRevision(
                name, ChangestructureFactory.createRepoRevision(revision, StubRepo.INSTANCE));
    }

    private static Stop binaryStop(String filename) {
        return new Stop(
                ChangestructureFactory.createBinaryChange(file(filename, 1), file(filename, 3), false, true),
                file(filename, 4));
    }

    private static Stop stop(String commonPrefix, String oldContent, String newContent, String commonSuffix) {
        final IFragment from = Fragment.createWithContent(file("test.java", 1),
                ChangestructureFactory.createPositionInText(1, 1 + commonPrefix.length()),
                ChangestructureFactory.createPositionInText(1, 1 + commonPrefix.length() + oldContent.length()),
                commonPrefix + oldContent + commonSuffix);
        final IFragment to = Fragment.createWithContent(file("test.java", 2),
                ChangestructureFactory.createPositionInText(1, 1 + commonPrefix.length()),
                ChangestructureFactory.createPositionInText(1, 1 + commonPrefix.length() + newContent.length()),
                commonPrefix + newContent + commonSuffix);
        return new Stop(
                ChangestructureFactory.createTextualChangeHunk(from, to, false, true),
                to);
    }

    private static OrderingInfo oi(Stop s1, Stop s2) {
        return new SimpleUnorderedMatch(false, null, Arrays.asList(s1, s2));
    }

    private static Collection<? extends OrderingInfo> determineRelations(Stop... stops) {
        return new TokenSimilarityRelation().determineMatches(Arrays.asList(stops));
    }

    @Test
    public void testEmptyInput() {
        assertEquals(Collections.emptyList(), determineRelations());
    }

    @Test
    public void testSingleInput() {
        assertEquals(Collections.emptyList(), determineRelations(stop("", "public", "private", " void foo() {")));
    }

    @Test
    public void testBinaryStops() {
        assertEquals(Collections.emptyList(), determineRelations(binaryStop("a"), binaryStop("b")));
    }

    @Test
    public void testTwoEqualStops() {
        final Stop s1 = stop("", "public", "private", " void foo() {");
        final Stop s2 = stop("", "public", "private", " void foo() {");
        assertEquals(
                Collections.singletonList(oi(s1, s2)),
                determineRelations(s1, s2));
    }

    @Test
    public void testOnlyChangedPartIsRelevant() {
        final Stop s1 = stop("", "public", "private", " void foo() {");
        final Stop s2 = stop("", "public", "private", " int bar() {");
        assertEquals(
                Collections.singletonList(oi(s1, s2)),
                determineRelations(s1, s2));
    }

    @Test
    public void testTwoDistinctStops() {
        assertEquals(Collections.emptyList(), determineRelations(
                stop("", "public", "private", " void foo() {"),
                stop("", "System.out.println(\"a\");", "messageHandler.println(\"b\");", "")));
    }

    @Test
    public void testMoveIsRegardedAsSimilar() {
        final Stop s1 = stop("", "this is the code that is moved", "", "");
        final Stop s2 = stop("", "", "this is the code that is moved", "");
        final Stop s3 = stop("", "", "this is some other code that was inserted", "");
        assertEquals(
                Collections.singletonList(oi(s1, s2)),
                determineRelations(s1, s2, s3));
    }

    @Test
    public void testResultIsOrderedBySimilarity() {
        final Stop s1 = stop("", "", "t0 t1 t2 t3 t4 t5 t6 t7 t8 t9", "");
        final Stop s2 = stop("", "", "t0 t1 t2 t3 t4 t5 t6 t7 t8", "");
        final Stop s3 = stop("", "", "t0 t1 t2 t3 t4 t5 t6 t7", "");
        final Stop s4 = stop("", "", "t0", "");
        //s1 s2: 9/10
        //s1 s3: 8/10
        //s1 s4: 1/10
        //s2 s3: 8/9
        //s2 s4: 1/9
        //s3 s4: 1/8
        assertEquals(
                Arrays.asList(oi(s1, s2), oi(s2, s3), oi(s1, s3)),
                determineRelations(s1, s2, s3, s4));
        assertEquals(
                Arrays.asList(oi(s1, s2), oi(s2, s3), oi(s1, s3)),
                determineRelations(s4, s3, s2, s1));
    }

}
