package de.setsoftware.reviewtool.changesources.svn;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.Test;

import de.setsoftware.reviewtool.model.changestructure.ChangestructureFactory;
import de.setsoftware.reviewtool.model.changestructure.FileInRevision;
import de.setsoftware.reviewtool.model.changestructure.RepoRevision;
import de.setsoftware.reviewtool.model.changestructure.Repository;
import de.setsoftware.reviewtool.model.changestructure.Revision;

/**
 * Tests for {@link SvnFileHistoryGraph}.
 */
public class SvnFileHistoryGraphTest {

    private static final Repository STUB_REPO = new Repository() {
        @Override
        public String toAbsolutePathInWc(String absolutePathInRepo) {
            return absolutePathInRepo;
        }

        @Override
        public Revision getSmallestRevision(Collection<? extends Revision> revisions) {
            final List<Revision> list = new ArrayList<>(revisions);
            Collections.sort(list, new Comparator<Revision>() {
                @Override
                public int compare(Revision o1, Revision o2) {
                    final Long rev1 = (Long) ((RepoRevision) o1).getId();
                    final Long rev2 = (Long) ((RepoRevision) o2).getId();
                    return Long.compare(rev1, rev2);
                }
            });
            return list.get(0);
        }

        @Override
        public byte[] getFileContents(String path, RepoRevision revision) {
            return new byte[0];
        }
    };

    private static FileInRevision file(String path, long revision) {
        return ChangestructureFactory.createFileInRevision(
                path,
                rev(revision),
                STUB_REPO);
    }

    private static RepoRevision rev(long revision) {
        return ChangestructureFactory.createRepoRevision(revision);
    }

    @Test
    public void testUnknownFile() {
        final SvnFileHistoryGraph g = new SvnFileHistoryGraph();
        assertEquals(
                Arrays.asList(file("a", 1)),
                g.getLatestFiles(file("a", 1)));
        assertEquals(
                Arrays.asList(file("bcd", 42)),
                g.getLatestFiles(file("bcd", 42)));
    }

    @Test
    public void testCopy() {
        final SvnFileHistoryGraph g = new SvnFileHistoryGraph();
        g.addChange("a", rev(0), rev(1), STUB_REPO);
        g.addCopy("a", "b", rev(5), rev(6), STUB_REPO);
        assertEquals(
                Arrays.asList(file("a", 5), file("b", 6)),
                g.getLatestFiles(file("a", 1)));
        assertEquals(
                Arrays.asList(file("a", 5), file("b", 6)),
                g.getLatestFiles(file("a", 5)));
        assertEquals(
                Arrays.asList(file("a", 6)),
                g.getLatestFiles(file("a", 6)));
        assertEquals(
                Arrays.asList(file("b", 6)),
                g.getLatestFiles(file("b", 6)));
    }

    @Test
    public void testDeletion() {
        final SvnFileHistoryGraph g = new SvnFileHistoryGraph();
        g.addChange("a", rev(0), rev(1), STUB_REPO);
        g.addDeletion("a", rev(11), rev(12), STUB_REPO);
        assertEquals(
                Arrays.asList(file("a", 11)),
                g.getLatestFiles(file("a", 1)));
    }

    @Test
    public void testDeletionOfUnknown() {
        final SvnFileHistoryGraph g = new SvnFileHistoryGraph();
        g.addDeletion("a", rev(11), rev(12), STUB_REPO);
        assertEquals(
                Arrays.asList(file("a", 1)),
                g.getLatestFiles(file("a", 1)));
    }

    @Test
    public void testMoveOneWay() {
        final SvnFileHistoryGraph g = new SvnFileHistoryGraph();
        g.addChange("a", rev(0), rev(1), STUB_REPO);
        g.addCopy("a", "b", rev(5), rev(6), STUB_REPO);
        g.addDeletion("a", rev(5), rev(6), STUB_REPO);
        assertEquals(
                Arrays.asList(file("b", 6)),
                g.getLatestFiles(file("a", 1)));
        assertEquals(
                Arrays.asList(file("b", 6)),
                g.getLatestFiles(file("a", 5)));
        assertEquals(
                Arrays.asList(file("b", 6)),
                g.getLatestFiles(file("b", 6)));
    }

    @Test
    public void testMoveOtherWay() {
        final SvnFileHistoryGraph g = new SvnFileHistoryGraph();
        g.addChange("a", rev(0), rev(1), STUB_REPO);
        g.addDeletion("a", rev(5), rev(6), STUB_REPO);
        g.addCopy("a", "b", rev(5), rev(6), STUB_REPO);
        assertEquals(
                Arrays.asList(file("b", 6)),
                g.getLatestFiles(file("a", 1)));
        assertEquals(
                Arrays.asList(file("b", 6)),
                g.getLatestFiles(file("a", 5)));
        assertEquals(
                Arrays.asList(file("b", 6)),
                g.getLatestFiles(file("b", 6)));
    }

    @Test
    public void testMoveWithMultipleCopies() {
        final SvnFileHistoryGraph g = new SvnFileHistoryGraph();
        g.addChange("a", rev(0), rev(1), STUB_REPO);
        g.addCopy("a", "b", rev(5), rev(6), STUB_REPO);
        g.addDeletion("a", rev(5), rev(6), STUB_REPO);
        g.addCopy("a", "c", rev(5), rev(6), STUB_REPO);
        g.addCopy("a", "d", rev(5), rev(6), STUB_REPO);
        assertEquals(
                Arrays.asList(file("b", 6), file("c", 6), file("d", 6)),
                g.getLatestFiles(file("a", 1)));
        assertEquals(
                Arrays.asList(file("b", 6), file("c", 6), file("d", 6)),
                g.getLatestFiles(file("a", 5)));
        assertEquals(
                Arrays.asList(file("b", 6)),
                g.getLatestFiles(file("b", 6)));
        assertEquals(
                Arrays.asList(file("c", 6)),
                g.getLatestFiles(file("c", 6)));
        assertEquals(
                Arrays.asList(file("d", 6)),
                g.getLatestFiles(file("d", 6)));
    }

    @Test
    public void testMoveMultipleTimes() {
        final SvnFileHistoryGraph g = new SvnFileHistoryGraph();
        g.addChange("a", rev(0), rev(1), STUB_REPO);
        g.addDeletion("a", rev(10), rev(11), STUB_REPO);
        g.addCopy("a", "b", rev(10), rev(11), STUB_REPO);
        g.addDeletion("b", rev(20), rev(21), STUB_REPO);
        g.addCopy("b", "c", rev(20), rev(21), STUB_REPO);
        g.addDeletion("c", rev(30), rev(31), STUB_REPO);
        g.addCopy("c", "d", rev(30), rev(31), STUB_REPO);

        assertEquals(
                Arrays.asList(file("d", 31)),
                g.getLatestFiles(file("a", 1)));
        assertEquals(
                Arrays.asList(file("d", 31)),
                g.getLatestFiles(file("a", 10)));
        assertEquals(
                Arrays.asList(file("a", 11)),
                g.getLatestFiles(file("a", 11)));
    }

    @Test
    public void testMoveAndDeleteAfterwards() {
        final SvnFileHistoryGraph g = new SvnFileHistoryGraph();
        g.addChange("a", rev(0), rev(1), STUB_REPO);
        g.addDeletion("a", rev(10), rev(11), STUB_REPO);
        g.addCopy("a", "b", rev(10), rev(11), STUB_REPO);
        g.addDeletion("b", rev(20), rev(21), STUB_REPO);

        assertEquals(
                Arrays.asList(file("b", 20)),
                g.getLatestFiles(file("a", 1)));
        assertEquals(
                Arrays.asList(file("b", 20)),
                g.getLatestFiles(file("a", 10)));
    }

    @Test
    public void testMoveAndDeleteAfterwardsStartWithDeletion() {
        final SvnFileHistoryGraph g = new SvnFileHistoryGraph();
        g.addDeletion("a", rev(10), rev(11), STUB_REPO);
        g.addCopy("a", "b", rev(10), rev(11), STUB_REPO);
        g.addDeletion("b", rev(20), rev(21), STUB_REPO);

        assertEquals(
                Arrays.asList(file("a", 1)),
                g.getLatestFiles(file("a", 1)));
        assertEquals(
                Arrays.asList(file("b", 20)),
                g.getLatestFiles(file("a", 10)));
    }

    @Test
    public void testCopyWithRevisionSkip() {
        final SvnFileHistoryGraph g = new SvnFileHistoryGraph();
        g.addChange("a", rev(0), rev(1), STUB_REPO);
        g.addChange("a", rev(5), rev(6), STUB_REPO);
        g.addDeletion("a", rev(19), rev(20), STUB_REPO);
        g.addCopy("a", "b", rev(5), rev(23), STUB_REPO);
        assertEquals(
                Arrays.asList(file("b", 23)),
                g.getLatestFiles(file("a", 1)));
        assertEquals(
                Arrays.asList(file("a", 19)),
                g.getLatestFiles(file("a", 6)));
        assertEquals(
                Arrays.asList(file("b", 23)),
                g.getLatestFiles(file("b", 23)));
        assertEquals(
                Arrays.asList(file("b", 6)),
                g.getLatestFiles(file("b", 6))); //b@6 is non-existing
    }

    @Test
    public void testCopyWithRevisionSkipStartWithDeletion() {
        final SvnFileHistoryGraph g = new SvnFileHistoryGraph();
        g.addDeletion("a", rev(19), rev(20), STUB_REPO);
        g.addCopy("a", "b", rev(5), rev(23), STUB_REPO);
        assertEquals(
                Arrays.asList(file("a", 1)),
                g.getLatestFiles(file("a", 1)));
        assertEquals(
                Arrays.asList(file("a", 5), file("b", 23)),
                g.getLatestFiles(file("a", 5)));
        assertEquals(
                Arrays.asList(file("a", 6)),
                g.getLatestFiles(file("a", 6)));
        assertEquals(
                Arrays.asList(file("b", 23)),
                g.getLatestFiles(file("b", 23)));
        assertEquals(
                Arrays.asList(file("b", 6)),
                g.getLatestFiles(file("b", 6))); //b@6 is non-existing
    }

    @Test
    public void testCopyMultipleTimesWithRevisionSkip() {
        final SvnFileHistoryGraph g = new SvnFileHistoryGraph();
        g.addChange("a", rev(0), rev(1), STUB_REPO);
        g.addChange("a", rev(5), rev(6), STUB_REPO);
        g.addDeletion("a", rev(19), rev(20), STUB_REPO);
        g.addCopy("a", "b", rev(5), rev(23), STUB_REPO);
        g.addCopy("b", "c", rev(23), rev(24), STUB_REPO);
        assertEquals(
                Arrays.asList(file("b", 23), file("c", 24)),
                g.getLatestFiles(file("a", 1)));
        assertEquals(
                Arrays.asList(file("a", 19)),
                g.getLatestFiles(file("a", 6)));
        assertEquals(
                Arrays.asList(file("b", 23), file("c", 24)),
                g.getLatestFiles(file("b", 23)));
        assertEquals(
                Arrays.asList(file("b", 6)),
                g.getLatestFiles(file("b", 6))); //b@6 is non-existing
    }

    @Test
    public void testMoveParentDirectory() {
        final SvnFileHistoryGraph g = new SvnFileHistoryGraph();
        g.addChange("a", rev(0), rev(1), STUB_REPO);
        g.addChange("a/x", rev(1), rev(2), STUB_REPO);
        g.addCopy("a", "b", rev(10), rev(11), STUB_REPO);
        g.addChange("a/x", rev(10), rev(11), STUB_REPO);
        g.addDeletion("a", rev(12), rev(13), STUB_REPO);
        g.addDeletion("b/x", rev(20), rev(21), STUB_REPO);

        assertEquals(
                Arrays.asList(file("a/x", 12)),
                g.getLatestFiles(file("a/x", 11)));
        assertEquals(
                Arrays.asList(file("a/x", 13)),
                g.getLatestFiles(file("a/x", 13))); // a/x@13 does not exist
        assertEquals(
                Arrays.asList(file("a/x", 12), file("b/x", 20)),
                g.getLatestFiles(file("a/x", 2)));
    }
}
