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
 * Tests for {@link FileHistoryGraph}.
 */
public class FileHistoryGraphTest {

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
    public void testUnknownFile() throws Exception {
        final FileHistoryGraph g = new FileHistoryGraph();
        assertEquals(
                Arrays.asList(file("a", 1)),
                g.getLatestFiles(file("a", 1)));
        assertEquals(
                Arrays.asList(file("bcd", 42)),
                g.getLatestFiles(file("bcd", 42)));
    }

    @Test
    public void testCopy() throws Exception {
        final FileHistoryGraph g = new FileHistoryGraph();
        g.addCopy("a", "b", rev(5), rev(6), STUB_REPO);
        assertEquals(
                Arrays.asList(file("a", 6), file("b", 6)),
                g.getLatestFiles(file("a", 1)));
        assertEquals(
                Arrays.asList(file("a", 6), file("b", 6)),
                g.getLatestFiles(file("a", 5)));
        assertEquals(
                Arrays.asList(file("a", 6)),
                g.getLatestFiles(file("a", 6)));
        assertEquals(
                Arrays.asList(file("b", 6)),
                g.getLatestFiles(file("b", 6)));
    }

    @Test
    public void testDeletion() throws Exception {
        final FileHistoryGraph g = new FileHistoryGraph();
        g.addDeletion("a", rev(12), STUB_REPO);
        assertEquals(
                Arrays.asList(file("a", 11)),
                g.getLatestFiles(file("a", 1)));
    }

    @Test
    public void testMoveOneWay() throws Exception {
        final FileHistoryGraph g = new FileHistoryGraph();
        g.addCopy("a", "b", rev(5), rev(6), STUB_REPO);
        g.addDeletion("a", rev(6), STUB_REPO);
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
    public void testMoveOtherWay() throws Exception {
        final FileHistoryGraph g = new FileHistoryGraph();
        g.addDeletion("a", rev(6), STUB_REPO);
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
    public void testMoveWithMultipleCopies() throws Exception {
        final FileHistoryGraph g = new FileHistoryGraph();
        g.addCopy("a", "b", rev(5), rev(6), STUB_REPO);
        g.addDeletion("a", rev(6), STUB_REPO);
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
    public void testMoveMultipleTimes() throws Exception {
        final FileHistoryGraph g = new FileHistoryGraph();
        g.addDeletion("a", rev(11), STUB_REPO);
        g.addCopy("a", "b", rev(10), rev(11), STUB_REPO);
        g.addDeletion("b", rev(21), STUB_REPO);
        g.addCopy("b", "c", rev(20), rev(21), STUB_REPO);
        g.addDeletion("c", rev(31), STUB_REPO);
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
    public void testMoveAndDeleteAfterwards() throws Exception {
        final FileHistoryGraph g = new FileHistoryGraph();
        g.addDeletion("a", rev(11), STUB_REPO);
        g.addCopy("a", "b", rev(10), rev(11), STUB_REPO);
        g.addDeletion("b", rev(21), STUB_REPO);

        assertEquals(
                Arrays.asList(file("b", 20)),
                g.getLatestFiles(file("a", 1)));
    }

    @Test
    public void testCopyWithRevisionSkip() throws Exception {
        final FileHistoryGraph g = new FileHistoryGraph();
        g.addCopy("a", "b", rev(5), rev(23), STUB_REPO);
        g.addDeletion("a", rev(20), STUB_REPO);
        assertEquals(
                Arrays.asList(file("b", 23)),
                g.getLatestFiles(file("a", 1)));
        assertEquals(
                Arrays.asList(file("a", 19)),
                g.getLatestFiles(file("a", 6)));
        assertEquals(
                Arrays.asList(file("b", 6)),
                g.getLatestFiles(file("b", 6))); //b@6 is non-existing
        assertEquals(
                Arrays.asList(file("b", 23)),
                g.getLatestFiles(file("b", 23)));
    }

}
