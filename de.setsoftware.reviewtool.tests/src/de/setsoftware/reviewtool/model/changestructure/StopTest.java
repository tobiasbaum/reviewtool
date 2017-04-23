package de.setsoftware.reviewtool.model.changestructure;

import static org.junit.Assert.assertEquals;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

/**
 * Tests for {@link Stop}.
 */
public class StopTest {

    private static FileInRevision file(String name, int revision) {
        return new FileInRevision(name, new RepoRevision(revision), StubRepo.INSTANCE);
    }

    @Test
    public void testRevisionsInHistoryAreSortedAfterMerge() {
        final Stop s1 = new Stop(
                ChangestructureFactory.createBinaryChange(file("a.java", 1), file("a.java", 3), false, true),
                file("a.java", 4));
        final Stop s2 = new Stop(
                ChangestructureFactory.createBinaryChange(file("a.java", 2), file("a.java", 4), false, true),
                file("a.java", 4));
        final Stop merged = s1.merge(s2);
        final Stop merged2 = s2.merge(s1);

        final Map<FileInRevision, FileInRevision> expected = new LinkedHashMap<>();
        expected.put(file("a.java", 1), file("a.java", 3));
        expected.put(file("a.java", 2), file("a.java", 4));
        assertEquals(expected, merged.getHistory());
        assertEquals(merged, merged2);
    }

}
