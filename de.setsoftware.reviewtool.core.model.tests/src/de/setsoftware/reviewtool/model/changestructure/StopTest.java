package de.setsoftware.reviewtool.model.changestructure;

import static org.junit.Assert.assertEquals;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

import de.setsoftware.reviewtool.base.ComparableWrapper;
import de.setsoftware.reviewtool.model.api.FileChangeType;
import de.setsoftware.reviewtool.model.api.IPositionInText;

/**
 * Tests for {@link Stop}.
 */
public class StopTest {

    private static FileInRevision file(final String name, final int revision) {
        return new FileInRevision(name, new RepoRevision<>(ComparableWrapper.wrap(revision), StubRepo.INSTANCE));
    }

    private static IPositionInText line(final int line) {
        return ChangestructureFactory.createPositionInText(line, 1);
    }

    @Test
    public void testRevisionsInHistoryAreSortedAfterMerge() {
        final Stop s1 = new Stop(
                ChangestructureFactory.createBinaryChange(null, FileChangeType.OTHER, file("a.java", 1), file("a.java", 3)),
                file("a.java", 4));
        final Stop s2 = new Stop(
                ChangestructureFactory.createBinaryChange(null, FileChangeType.OTHER, file("a.java", 2), file("a.java", 4)),
                file("a.java", 4));
        final Stop merged = s1.merge(s2);
        final Stop merged2 = s2.merge(s1);

        final Map<FileInRevision, FileInRevision> expected = new LinkedHashMap<>();
        expected.put(file("a.java", 1), file("a.java", 3));
        expected.put(file("a.java", 2), file("a.java", 4));
        assertEquals(expected, merged.getHistory());
        assertEquals(merged, merged2);
    }

    @Test
    public void testNumberOfRemovedAndAddedLinesSimple() {
        final Stop s = new Stop(
                ChangestructureFactory.createTextualChangeHunk(
                        null,
                        FileChangeType.OTHER,
                        ChangestructureFactory.createFragment(file("a.java", 1), line(3), line(6)),
                        ChangestructureFactory.createFragment(file("a.java", 3), line(8), line(13))),
                ChangestructureFactory.createFragment(file("a.java", 4), line(8), line(13)));
        assertEquals(2, s.getNumberOfFragments());
        assertEquals(5, s.getNumberOfAddedLines());
        assertEquals(3, s.getNumberOfRemovedLines());
    }

    @Test
    public void testNumberOfRemovedAndAddedLinesComplex1() {
        final Stop s1 = new Stop(
                ChangestructureFactory.createTextualChangeHunk(
                        null,
                        FileChangeType.OTHER,
                        ChangestructureFactory.createFragment(file("a.java", 1), line(3), line(6)),
                        ChangestructureFactory.createFragment(file("a.java", 3), line(8), line(13))),
                ChangestructureFactory.createFragment(file("a.java", 4), line(8), line(13)));
        final Stop s2 = new Stop(
                ChangestructureFactory.createTextualChangeHunk(
                        null,
                        FileChangeType.OTHER,
                        ChangestructureFactory.createFragment(file("a.java", 1), line(23), line(26)),
                        ChangestructureFactory.createFragment(file("a.java", 3), line(13), line(17))),
                ChangestructureFactory.createFragment(file("a.java", 4), line(13), line(17)));
        final Stop merged = s1.merge(s2);
        assertEquals(3, merged.getNumberOfFragments());
        assertEquals(9, merged.getNumberOfAddedLines());
        assertEquals(6, merged.getNumberOfRemovedLines());
    }

    @Test
    public void testNumberOfRemovedAndAddedLinesComplex2() {
        final Stop s1 = new Stop(
                ChangestructureFactory.createTextualChangeHunk(
                        null,
                        FileChangeType.OTHER,
                        ChangestructureFactory.createFragment(file("a.java", 1), line(3), line(6)),
                        ChangestructureFactory.createFragment(file("a.java", 3), line(8), line(13))),
                ChangestructureFactory.createFragment(file("a.java", 4), line(12), line(17)));
        final Stop s2 = new Stop(
                ChangestructureFactory.createTextualChangeHunk(
                        null,
                        FileChangeType.OTHER,
                        ChangestructureFactory.createFragment(file("a.java", 3), line(8), line(13)),
                        ChangestructureFactory.createFragment(file("a.java", 4), line(12), line(17))),
                ChangestructureFactory.createFragment(file("a.java", 4), line(12), line(17)));
        final Stop merged = s1.merge(s2);
        assertEquals(3, merged.getNumberOfFragments());
        assertEquals(5, merged.getNumberOfAddedLines());
        assertEquals(3, merged.getNumberOfRemovedLines());
    }
}
