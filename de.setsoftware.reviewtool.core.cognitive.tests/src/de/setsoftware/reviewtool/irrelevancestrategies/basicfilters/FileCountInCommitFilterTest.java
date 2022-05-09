package de.setsoftware.reviewtool.irrelevancestrategies.basicfilters;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Test;

import de.setsoftware.reviewtool.base.ComparableWrapper;
import de.setsoftware.reviewtool.irrelevancestrategies.pathfilters.FileCountInCommitFilter;
import de.setsoftware.reviewtool.model.api.FileChangeType;
import de.setsoftware.reviewtool.model.api.IChange;
import de.setsoftware.reviewtool.model.api.ICommit;
import de.setsoftware.reviewtool.model.api.IRevision;
import de.setsoftware.reviewtool.model.changestructure.ChangestructureFactory;
import de.setsoftware.reviewtool.model.changestructure.StubRepo;

public class FileCountInCommitFilterTest {

    private static final StubRepo STUB_REPO = new StubRepo();

    private static ICommit commit(int id, String... filenames) {
        final List<IChange> changes = new ArrayList<>();
        final IRevision cur = revision(id);
        final IRevision prev = revision(id - 1);
        for (final String filename : filenames) {
            changes.add(ChangestructureFactory.createBinaryChange(null,
                    FileChangeType.OTHER,
                    ChangestructureFactory.createFileInRevision(filename, prev),
                    ChangestructureFactory.createFileInRevision(filename, cur)));
        }
        return ChangestructureFactory.createCommit(null, "msg" + id, changes , cur, new Date(id * 1000));
    }

    private static IRevision revision(int id) {
        return ChangestructureFactory.createRepoRevision(
                ComparableWrapper.wrap(id), STUB_REPO);
    }

    @Test
    public void testTresholdNotReached() {
        final ICommit c = commit(42, "a.txt", "b.txt", "b.txt", "b.txt");
        final FileCountInCommitFilter f = new FileCountInCommitFilter(0, "asdf", 3);
        assertFalse(f.isIrrelevant(c, c.getChanges().get(0)));
        assertFalse(f.isIrrelevant(c, c.getChanges().get(1)));
        assertFalse(f.isIrrelevant(c, c.getChanges().get(2)));
        assertFalse(f.isIrrelevant(c, c.getChanges().get(3)));
    }

    @Test
    public void testTresholdReached() {
        final ICommit c = commit(42, "a.txt", "b.txt", "b.txt", "b.txt", "c.txt");
        final FileCountInCommitFilter f = new FileCountInCommitFilter(0, "asdf", 3);
        assertTrue(f.isIrrelevant(c, c.getChanges().get(0)));
        assertTrue(f.isIrrelevant(c, c.getChanges().get(1)));
        assertTrue(f.isIrrelevant(c, c.getChanges().get(2)));
        assertTrue(f.isIrrelevant(c, c.getChanges().get(3)));
        assertTrue(f.isIrrelevant(c, c.getChanges().get(4)));
    }

    @Test
    public void testMultipleCommits() {
        final ICommit c1 = commit(42, "a.txt");
        final ICommit c2 = commit(43, "b.txt", "c.txt");
        final ICommit c3 = commit(43, "d.txt", "e.txt", "f.txt");
        final FileCountInCommitFilter f = new FileCountInCommitFilter(0, "asdf", 2);
        assertFalse(f.isIrrelevant(c1, c1.getChanges().get(0)));
        assertTrue(f.isIrrelevant(c2, c2.getChanges().get(0)));
        assertTrue(f.isIrrelevant(c2, c2.getChanges().get(1)));
        assertTrue(f.isIrrelevant(c3, c3.getChanges().get(0)));
        assertTrue(f.isIrrelevant(c3, c3.getChanges().get(1)));
        assertTrue(f.isIrrelevant(c3, c3.getChanges().get(2)));
        assertFalse(f.isIrrelevant(c1, c1.getChanges().get(0)));
        assertTrue(f.isIrrelevant(c2, c2.getChanges().get(0)));
        assertTrue(f.isIrrelevant(c2, c2.getChanges().get(1)));
        assertTrue(f.isIrrelevant(c3, c3.getChanges().get(0)));
        assertTrue(f.isIrrelevant(c3, c3.getChanges().get(1)));
        assertTrue(f.isIrrelevant(c3, c3.getChanges().get(2)));
    }

}
