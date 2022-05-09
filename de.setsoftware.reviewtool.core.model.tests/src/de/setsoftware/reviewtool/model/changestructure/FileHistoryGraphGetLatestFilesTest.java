package de.setsoftware.reviewtool.model.changestructure;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import de.setsoftware.reviewtool.base.ComparableWrapper;
import de.setsoftware.reviewtool.diffalgorithms.DiffAlgorithmFactory;
import de.setsoftware.reviewtool.model.api.IMutableFileHistoryGraph;
import de.setsoftware.reviewtool.model.api.IRepoRevision;
import de.setsoftware.reviewtool.model.api.IRevisionedFile;

/**
 * Tests {@link FileHistoryGraph#getLatestFiles(IRevisionedFile, boolean)}.
 */
public class FileHistoryGraphGetLatestFilesTest {

    private static IMutableFileHistoryGraph graph() {
        return new FileHistoryGraph(DiffAlgorithmFactory.createDefault());
    }

    private static IRevisionedFile file(final String path, final long revision) {
        return ChangestructureFactory.createFileInRevision(
                path,
                rev(revision));
    }

    private static IRepoRevision<ComparableWrapper<Long>> rev(final long revision) {
        return ChangestructureFactory.createRepoRevision(ComparableWrapper.wrap(revision), StubRepo.INSTANCE);
    }

    @Test
    public void testUnknownFile() {
        final IMutableFileHistoryGraph g = graph();
        assertEquals(
                Arrays.asList(file("/a", 1)),
                g.getLatestFiles(file("/a", 1), false));
        assertEquals(
                Arrays.asList(file("/bcd", 42)),
                g.getLatestFiles(file("/bcd", 42), false));
    }

    @Test
    public void testCopy() {
        final IMutableFileHistoryGraph g = graph();
        g.addChange("/a", rev(1), Collections.singleton(rev(0)));
        g.addCopy("/a", rev(5), "/b", rev(6));
        assertEquals(
                Arrays.asList(file("/a", 5), file("/b", 6)),
                g.getLatestFiles(file("/a", 1), false));
        assertEquals(
                Arrays.asList(file("/a", 5), file("/b", 6)),
                g.getLatestFiles(file("/a", 5), false));
        assertEquals(
                Arrays.asList(file("/a", 6)),
                g.getLatestFiles(file("/a", 6), false));
        assertEquals(
                Arrays.asList(file("/b", 6)),
                g.getLatestFiles(file("/b", 6), false));
    }

    @Test
    public void testDeletion() {
        final IMutableFileHistoryGraph g = graph();
        g.addChange("/a", rev(1), Collections.singleton(rev(0)));
        g.addDeletion("/a", rev(12));
        assertEquals(
                Arrays.asList(file("/a", 1)),
                g.getLatestFiles(file("/a", 1), false));
    }

    @Test
    public void testDeletionOfUnknown() {
        final IMutableFileHistoryGraph g = graph();
        g.addDeletion("/a", rev(12));
        assertEquals(
                Arrays.asList(file("/a", 1)),
                g.getLatestFiles(file("/a", 1), false));
    }

    @Test
    public void testMoveOneWay() {
        final IMutableFileHistoryGraph g = graph();
        g.addChange("/a", rev(1), Collections.singleton(rev(0)));
        g.addCopy("/a", rev(5), "/b", rev(6));
        g.addDeletion("/a", rev(6));
        assertEquals(
                Arrays.asList(file("/b", 6)),
                g.getLatestFiles(file("/a", 1), false));
        assertEquals(
                Arrays.asList(file("/b", 6)),
                g.getLatestFiles(file("/a", 5), false));
        assertEquals(
                Arrays.asList(file("/b", 6)),
                g.getLatestFiles(file("/b", 6), false));
    }

    @Test
    public void testMoveOtherWay() {
        final IMutableFileHistoryGraph g = graph();
        g.addChange("/a", rev(1), Collections.singleton(rev(0)));
        g.addDeletion("/a", rev(6));
        g.addCopy("/a", rev(5), "/b", rev(6));
        assertEquals(
                Arrays.asList(file("/b", 6)),
                g.getLatestFiles(file("/a", 1), false));
        assertEquals(
                Arrays.asList(file("/b", 6)),
                g.getLatestFiles(file("/a", 5), false));
        assertEquals(
                Arrays.asList(file("/b", 6)),
                g.getLatestFiles(file("/b", 6), false));
    }

    @Test
    public void testMoveWithMultipleCopies() {
        final IMutableFileHistoryGraph g = graph();
        g.addChange("/a", rev(1), Collections.singleton(rev(0)) );
        g.addCopy("/a", rev(5), "/b", rev(6));
        g.addDeletion("/a", rev(6));
        g.addCopy("/a", rev(5), "/c", rev(6));
        g.addCopy("/a", rev(5), "/d", rev(6));
        assertEquals(
                Arrays.asList(file("/b", 6), file("/c", 6), file("/d", 6)),
                g.getLatestFiles(file("/a", 1), false));
        assertEquals(
                Arrays.asList(file("/b", 6), file("/c", 6), file("/d", 6)),
                g.getLatestFiles(file("/a", 5), false));
        assertEquals(
                Arrays.asList(file("/b", 6)),
                g.getLatestFiles(file("/b", 6), false));
        assertEquals(
                Arrays.asList(file("/c", 6)),
                g.getLatestFiles(file("/c", 6), false));
        assertEquals(
                Arrays.asList(file("/d", 6)),
                g.getLatestFiles(file("/d", 6), false));
    }

    @Test
    public void testMoveMultipleTimes() {
        final IMutableFileHistoryGraph g = graph();
        g.addChange("/a", rev(1), Collections.singleton(rev(0)));
        g.addDeletion("/a", rev(11));
        g.addCopy("/a", rev(10), "/b", rev(11));
        g.addDeletion("/b", rev(21));
        g.addCopy("/b", rev(20), "/c", rev(21));
        g.addDeletion("/c", rev(31));
        g.addCopy("/c", rev(30), "/d", rev(31));

        assertEquals(
                Arrays.asList(file("/d", 31)),
                g.getLatestFiles(file("/a", 1), false));
        assertEquals(
                Arrays.asList(file("/d", 31)),
                g.getLatestFiles(file("/a", 10), false));
        assertEquals(
                Arrays.asList(file("/a", 11)),
                g.getLatestFiles(file("/a", 11), false));
    }

    @Test
    public void testMoveAndDeleteAfterwards() {
        final IMutableFileHistoryGraph g = graph();
        g.addChange("/a", rev(1), Collections.singleton(rev(0)));
        g.addDeletion("/a", rev(11));
        g.addCopy("/a", rev(10), "/b", rev(11));
        g.addDeletion("/b", rev(21));

        assertEquals(
                Arrays.asList(file("/b", 11)),
                g.getLatestFiles(file("/a", 1), false));
        assertEquals(
                Arrays.asList(file("/b", 11)),
                g.getLatestFiles(file("/a", 10), false));
    }

    @Test
    public void testMoveAndDeleteAfterwardsStartWithDeletion() {
        final IMutableFileHistoryGraph g = graph();
        g.addDeletion("/a", rev(11));
        g.addCopy("/a", rev(10), "/b", rev(11));
        g.addDeletion("/b", rev(21));

        assertEquals(
                Arrays.asList(file("/a", 1)),
                g.getLatestFiles(file("/a", 1), false));
        assertEquals(
                Arrays.asList(file("/a", 10)), // (a,10)-->(a,11) is not known as the graph starts at revision 11
                g.getLatestFiles(file("/a", 10), false));
    }

    @Test
    public void testCopyWithRevisionSkip() {
        final IMutableFileHistoryGraph g = graph();
        g.addChange("/a", rev(1), Collections.singleton(rev(0)));
        g.addChange("/a", rev(6), Collections.singleton(rev(5)));
        g.addDeletion("/a", rev(20));
        g.addCopy("/a", rev(5), "/b", rev(23));
        assertEquals(
                Arrays.asList(file("/b", 23)),
                g.getLatestFiles(file("/a", 1), false));
        assertEquals(
                Arrays.asList(file("/a", 6)),
                g.getLatestFiles(file("/a", 6), false));
        assertEquals(
                Arrays.asList(file("/b", 23)),
                g.getLatestFiles(file("/b", 23), false));
        assertEquals(
                Arrays.asList(file("/b", 6)),
                g.getLatestFiles(file("/b", 6), false)); //b@6 is non-existing
    }

    @Test
    public void testCopyWithRevisionSkipStartWithDeletion() {
        final IMutableFileHistoryGraph g = graph();
        g.addDeletion("/a", rev(20));
        g.addCopy("/a", rev(5), "/b", rev(23));
        assertEquals(
                Arrays.asList(file("/a", 1)),
                g.getLatestFiles(file("/a", 1), false));
        assertEquals(
                Arrays.asList(file("/a", 5), file("/b", 23)),
                g.getLatestFiles(file("/a", 5), false));
        assertEquals(
                Arrays.asList(file("/a", 6)),
                g.getLatestFiles(file("/a", 6), false));
        assertEquals(
                Arrays.asList(file("/b", 23)),
                g.getLatestFiles(file("/b", 23), false));
        assertEquals(
                Arrays.asList(file("/b", 6)),
                g.getLatestFiles(file("/b", 6), false)); //b@6 is non-existing
    }

    @Test
    public void testCopyMultipleTimesWithRevisionSkip() {
        final IMutableFileHistoryGraph g = graph();
        g.addChange("/a", rev(1), Collections.singleton(rev(0)));
        g.addChange("/a", rev(6), Collections.singleton(rev(5)));
        g.addDeletion("/a", rev(20));
        g.addCopy("/a", rev(5), "/b", rev(23));
        g.addCopy("/b", rev(23), "/c", rev(24));
        assertEquals(
                Arrays.asList(file("/b", 23), file("/c", 24)),
                g.getLatestFiles(file("/a", 1), false));
        assertEquals(
                Arrays.asList(file("/a", 6)),
                g.getLatestFiles(file("/a", 6), false));
        assertEquals(
                Arrays.asList(file("/b", 23), file("/c", 24)),
                g.getLatestFiles(file("/b", 23), false));
        assertEquals(
                Arrays.asList(file("/b", 6)),
                g.getLatestFiles(file("/b", 6), false)); //b@6 is non-existing
    }
}
