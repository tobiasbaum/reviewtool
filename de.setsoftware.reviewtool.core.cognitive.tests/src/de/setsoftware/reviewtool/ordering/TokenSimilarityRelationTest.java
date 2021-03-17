package de.setsoftware.reviewtool.ordering;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import de.setsoftware.reviewtool.base.ComparableWrapper;
import de.setsoftware.reviewtool.model.api.FileChangeType;
import de.setsoftware.reviewtool.model.api.IFragment;
import de.setsoftware.reviewtool.model.api.IRevisionedFile;
import de.setsoftware.reviewtool.model.changestructure.ChangestructureFactory;
import de.setsoftware.reviewtool.model.changestructure.Fragment;
import de.setsoftware.reviewtool.model.changestructure.Stop;
import de.setsoftware.reviewtool.model.changestructure.StubRepo;
import de.setsoftware.reviewtool.ordering.efficientalgorithm.TourCalculatorControl;

/**
 * Test cases for {@link TokenSimilarityRelation}.
 */
public class TokenSimilarityRelationTest {

    private static IRevisionedFile file(final String name, final int revision) {
        return ChangestructureFactory.createFileInRevision(
                name, ChangestructureFactory.createRepoRevision(ComparableWrapper.wrap(revision), StubRepo.INSTANCE));
    }

    private static Stop binaryStop(final String filename) {
        return new Stop(
                ChangestructureFactory.createBinaryChange(
                        null, FileChangeType.OTHER, file(filename, 1), file(filename, 3)),
                file(filename, 4));
    }

    private static Stop stop(final String commonPrefix, final String oldContent, final String newContent, final String commonSuffix) {
        final IFragment from = Fragment.createWithContent(file("test.java", 1),
                ChangestructureFactory.createPositionInText(1, 1 + commonPrefix.length()),
                ChangestructureFactory.createPositionInText(1, 1 + commonPrefix.length() + oldContent.length()),
                commonPrefix + oldContent + commonSuffix);
        final IFragment to = Fragment.createWithContent(file("test.java", 2),
                ChangestructureFactory.createPositionInText(1, 1 + commonPrefix.length()),
                ChangestructureFactory.createPositionInText(1, 1 + commonPrefix.length() + newContent.length()),
                commonPrefix + newContent + commonSuffix);
        return new Stop(
                ChangestructureFactory.createTextualChangeHunk(null, FileChangeType.OTHER, from, to),
                to);
    }

    private static OrderingInfo oi(final Stop s1, final Stop s2) {
        return OrderingInfoImpl.unordered(HierarchyExplicitness.NONE, null, Arrays.asList(wrap(s1), wrap(s2)));
    }

    private static ChangePart wrap(final Stop s) {
        return new ChangePart(Collections.singletonList(s), Collections.emptySet());
    }

    private static List<ChangePart> wrap(final Stop[] stops) {
        final List<ChangePart> ret = new ArrayList<>();
        for (final Stop s : stops) {
            ret.add(wrap(s));
        }
        return ret;
    }

    private static Collection<? extends OrderingInfo> determineRelations(final Stop... stops) throws Exception {
        return new TokenSimilarityRelation().determineMatches(wrap(stops), TourCalculatorControl.NO_CANCEL);
    }

    @Test
    public void testEmptyInput() throws Exception {
        assertEquals(Collections.emptyList(), determineRelations());
    }

    @Test
    public void testSingleInput() throws Exception {
        assertEquals(Collections.emptyList(), determineRelations(stop("", "public", "private", " void foo() {")));
    }

    @Test
    public void testBinaryStops() throws Exception {
        assertEquals(Collections.emptyList(), determineRelations(binaryStop("a"), binaryStop("b")));
    }

    @Test
    public void testTwoEqualStops() throws Exception {
        final Stop s1 = stop("", "public", "private", " void foo() {");
        final Stop s2 = stop("", "public", "private", " void foo() {");
        assertEquals(
                Collections.singletonList(oi(s1, s2)),
                determineRelations(s1, s2));
    }

    @Test
    public void testOnlyChangedPartIsRelevant() throws Exception {
        final Stop s1 = stop("", "public", "private", " void foo() {");
        final Stop s2 = stop("", "public", "private", " int bar() {");
        assertEquals(
                Collections.singletonList(oi(s1, s2)),
                determineRelations(s1, s2));
    }

    @Test
    public void testTwoDistinctStops() throws Exception {
        assertEquals(Collections.emptyList(), determineRelations(
                stop("", "public", "private", " void foo() {"),
                stop("", "System.out.println(\"a\");", "messageHandler.println(\"b\");", "")));
    }

    @Test
    public void testMoveIsRegardedAsSimilar() throws Exception {
        final Stop s1 = stop("", "this is the code that is moved", "", "");
        final Stop s2 = stop("", "", "this is the code that is moved", "");
        final Stop s3 = stop("", "", "this is some other code that was inserted", "");
        assertEquals(
                Collections.singletonList(oi(s1, s2)),
                determineRelations(s1, s2, s3));
    }

    @Test
    public void testResultIsOrderedBySimilarity() throws Exception {
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
