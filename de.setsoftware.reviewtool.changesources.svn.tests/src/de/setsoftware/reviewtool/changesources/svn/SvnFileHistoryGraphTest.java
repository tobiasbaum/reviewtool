package de.setsoftware.reviewtool.changesources.svn;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.Test;

import de.setsoftware.reviewtool.model.api.IRepoRevision;
import de.setsoftware.reviewtool.model.api.IRepository;
import de.setsoftware.reviewtool.model.api.IRevision;
import de.setsoftware.reviewtool.model.api.IRevisionedFile;
import de.setsoftware.reviewtool.model.changestructure.AbstractRepository;
import de.setsoftware.reviewtool.model.changestructure.ChangestructureFactory;

/**
 * Tests for {@link SvnFileHistoryGraph}.
 */
public class SvnFileHistoryGraphTest {

    private static final IRepository STUB_REPO = new AbstractRepository() {
        @Override
        public String getId() {
            return "stub";
        }

        @Override
        public File getLocalRoot() {
            return null;
        }

        @Override
        public IRepoRevision toRevision(final String revisionId) {
            return ChangestructureFactory.createRepoRevision(revisionId, this);
        }

        @Override
        public String toAbsolutePathInWc(String absolutePathInRepo) {
            return absolutePathInRepo;
        }

        @Override
        public String fromAbsolutePathInWc(String absolutePathInWc) {
            return absolutePathInWc;
        }

        @Override
        public IRevision getSmallestRevision(Collection<? extends IRevision> revisions) {
            final List<IRevision> list = new ArrayList<>(revisions);
            Collections.sort(list, new Comparator<IRevision>() {
                @Override
                public int compare(IRevision o1, IRevision o2) {
                    final Long rev1 = (Long) ((IRepoRevision) o1).getId();
                    final Long rev2 = (Long) ((IRepoRevision) o2).getId();
                    return Long.compare(rev1, rev2);
                }
            });
            return list.get(0);
        }

        @Override
        public byte[] getFileContents(String path, IRepoRevision revision) {
            return new byte[0];
        }
    };

    private static IRevisionedFile file(String path, long revision) {
        return ChangestructureFactory.createFileInRevision(
                path,
                rev(revision));
    }

    private static IRepoRevision rev(long revision) {
        return ChangestructureFactory.createRepoRevision(revision, STUB_REPO);
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
        g.addChange("a", rev(1), Collections.singleton(rev(0)));
        g.addCopy("a", "b", rev(5), rev(6));
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
        g.addChange("a", rev(1), Collections.singleton(rev(0)));
        g.addDeletion("a", rev(12));
        assertEquals(
                Arrays.asList(file("a", 1)),
                g.getLatestFiles(file("a", 1)));
    }

    @Test
    public void testDeletionOfUnknown() {
        final SvnFileHistoryGraph g = new SvnFileHistoryGraph();
        g.addDeletion("a", rev(12));
        assertEquals(
                Arrays.asList(file("a", 1)),
                g.getLatestFiles(file("a", 1)));
    }

    @Test
    public void testMoveOneWay() {
        final SvnFileHistoryGraph g = new SvnFileHistoryGraph();
        g.addChange("a", rev(1), Collections.singleton(rev(0)));
        g.addCopy("a", "b", rev(5), rev(6));
        g.addDeletion("a", rev(6));
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
        g.addChange("a", rev(1), Collections.singleton(rev(0)));
        g.addDeletion("a", rev(6));
        g.addCopy("a", "b", rev(5), rev(6));
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
        g.addChange("a", rev(1), Collections.singleton(rev(0)) );
        g.addCopy("a", "b", rev(5), rev(6));
        g.addDeletion("a", rev(6));
        g.addCopy("a", "c", rev(5), rev(6));
        g.addCopy("a", "d", rev(5), rev(6));
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
        g.addChange("a", rev(1), Collections.singleton(rev(0)));
        g.addDeletion("a", rev(11));
        g.addCopy("a", "b", rev(10), rev(11));
        g.addDeletion("b", rev(21));
        g.addCopy("b", "c", rev(20), rev(21));
        g.addDeletion("c", rev(31));
        g.addCopy("c", "d", rev(30), rev(31));

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
        g.addChange("a", rev(1), Collections.singleton(rev(0)));
        g.addDeletion("a", rev(11));
        g.addCopy("a", "b", rev(10), rev(11));
        g.addDeletion("b", rev(21));

        assertEquals(
                Arrays.asList(file("b", 11)),
                g.getLatestFiles(file("a", 1)));
        assertEquals(
                Arrays.asList(file("b", 11)),
                g.getLatestFiles(file("a", 10)));
    }

    @Test
    public void testMoveAndDeleteAfterwardsStartWithDeletion() {
        final SvnFileHistoryGraph g = new SvnFileHistoryGraph();
        g.addDeletion("a", rev(11));
        g.addCopy("a", "b", rev(10), rev(11));
        g.addDeletion("b", rev(21));

        assertEquals(
                Arrays.asList(file("a", 1)),
                g.getLatestFiles(file("a", 1)));
        assertEquals(
                Arrays.asList(file("a", 10)), // (a,10)-->(a,11) is not known as the graph starts at revision 11
                g.getLatestFiles(file("a", 10)));
    }

    @Test
    public void testCopyWithRevisionSkip() {
        final SvnFileHistoryGraph g = new SvnFileHistoryGraph();
        g.addChange("a", rev(1), Collections.singleton(rev(0)));
        g.addChange("a", rev(6), Collections.singleton(rev(5)));
        g.addDeletion("a", rev(20));
        g.addCopy("a", "b", rev(5), rev(23));
        assertEquals(
                Arrays.asList(file("b", 23)),
                g.getLatestFiles(file("a", 1)));
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
    public void testCopyWithRevisionSkipStartWithDeletion() {
        final SvnFileHistoryGraph g = new SvnFileHistoryGraph();
        g.addDeletion("a", rev(20));
        g.addCopy("a", "b", rev(5), rev(23));
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
        g.addChange("a", rev(1), Collections.singleton(rev(0)));
        g.addChange("a", rev(6), Collections.singleton(rev(5)));
        g.addDeletion("a", rev(20));
        g.addCopy("a", "b", rev(5), rev(23));
        g.addCopy("b", "c", rev(23), rev(24));
        assertEquals(
                Arrays.asList(file("b", 23), file("c", 24)),
                g.getLatestFiles(file("a", 1)));
        assertEquals(
                Arrays.asList(file("a", 6)),
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
        g.addChange("a", rev(1), Collections.singleton(rev(0)));
        g.addChange("a/x", rev(2), Collections.singleton(rev(1)));
        g.addCopy("a", "b", rev(10), rev(11));
        g.addChange("a/x", rev(11), Collections.singleton(rev(10)));
        g.addChange("b/x", rev(11), Collections.singleton(rev(10)));
        g.addDeletion("a", rev(13));
        g.addDeletion("b/x", rev(21));

        assertEquals(
                Arrays.asList(file("a/x", 11)),
                g.getLatestFiles(file("a/x", 11)));
        assertEquals(
                Arrays.asList(file("a/x", 13)),
                g.getLatestFiles(file("a/x", 13))); // a/x@13 does not exist
        assertEquals(
                Arrays.asList(file("a/x", 11), file("b/x", 11)),
                g.getLatestFiles(file("a/x", 2)));
    }
}
