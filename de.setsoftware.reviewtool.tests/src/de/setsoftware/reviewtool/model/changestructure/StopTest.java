package de.setsoftware.reviewtool.model.changestructure;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

public class StopTest {

    private static FileInRevision file(String name, int revision) {
        return new FileInRevision(name, new RepoRevision(revision), StubRepo.INSTANCE);
    }

    @Test
    public void testRevisionsInHistoryAreSortedAfterMerge() {
        final Stop s1 = new Stop(file("a.java", 1), file("a.java", 3), file("a.java", 4));
        final Stop s2 = new Stop(file("a.java", 2), file("a.java", 4), file("a.java", 4));
        final Stop merged = s1.merge(s2);
        final Stop merged2 = s2.merge(s1);

        assertEquals(
                Arrays.asList(file("a.java", 1), file("a.java", 2), file("a.java", 3), file("a.java", 4)),
                merged.getHistory());
        assertEquals(merged, merged2);
    }

}
