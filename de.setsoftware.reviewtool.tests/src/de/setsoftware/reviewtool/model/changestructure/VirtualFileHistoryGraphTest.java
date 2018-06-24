package de.setsoftware.reviewtool.model.changestructure;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import de.setsoftware.reviewtool.base.ComparableWrapper;
import de.setsoftware.reviewtool.model.api.IFileHistoryEdge;
import de.setsoftware.reviewtool.model.api.IFileHistoryNode;
import de.setsoftware.reviewtool.model.api.ILocalRevision;
import de.setsoftware.reviewtool.model.api.IMutableFileHistoryGraph;
import de.setsoftware.reviewtool.model.api.IRepoRevision;
import de.setsoftware.reviewtool.model.api.IRevision;
import de.setsoftware.reviewtool.model.api.IRevisionedFile;

/**
 * Tests {@link VirtualFileHistoryGraph}.
 */
public class VirtualFileHistoryGraphTest {

    private TestRepository repo;
    private TestWorkingCopy wc;
    private IMutableFileHistoryGraph remoteFileHistoryGraph;
    private VirtualFileHistoryGraph virtualFileHistoryGraph;

    private IRepoRevision<ComparableWrapper<Long>> rev(final long id) {
        return new TestRepoRevision(this.repo, id);
    }

    private ILocalRevision localRev() {
        return ChangestructureFactory.createLocalRevision(this.wc);
    }

    private IRevisionedFile file(final String path, final IRevision revision) {
        return ChangestructureFactory.createFileInRevision(path, revision);
    }

    private boolean hasOnlyAlphaAncestor(final IFileHistoryNode node) {
        final Set<? extends IFileHistoryEdge> ancestors = node.getAncestors();
        if (ancestors.size() != 1) {
            return false;
        } else {
            return ancestors.iterator().next().getAncestor().getFile().getRevision().equals(
                    ChangestructureFactory.createUnknownRevision(this.repo));
        }
    }

    @Before
    public void setUp() {
        this.repo = new TestRepository("/some/repo");
        this.wc = new TestWorkingCopy(this.repo, new File("/some/wc"));
        this.remoteFileHistoryGraph = new TestFileHistoryGraph();
        this.virtualFileHistoryGraph = new VirtualFileHistoryGraph(this.remoteFileHistoryGraph);
        // revision 1
        this.remoteFileHistoryGraph.addAddition("/dir1", this.rev(1));
        this.remoteFileHistoryGraph.addAddition("/dir1/dir2", this.rev(1));
        this.remoteFileHistoryGraph.addAddition("/dir1/dir2/a.txt", this.rev(1));
        this.remoteFileHistoryGraph.addAddition("/dir1/dir2/b.txt", this.rev(1));
        this.remoteFileHistoryGraph.addAddition("/dir1/dir2/c.txt", this.rev(1));
        this.remoteFileHistoryGraph.addAddition("/dir1/dir2/d.txt", this.rev(1));
        // revision 2
        this.remoteFileHistoryGraph.addChange("/dir1/dir2/a.txt", this.rev(2), Collections.singleton(this.rev(1)));
        this.remoteFileHistoryGraph.addDeletion("/dir1/dir2/b.txt", this.rev(2));
        this.remoteFileHistoryGraph.addReplacement("/dir1/dir2/c.txt", this.rev(2));
    }

    @Test
    public void testNoLocalChanges() {
        final IMutableFileHistoryGraph localGraph = new TestFileHistoryGraph();
        this.virtualFileHistoryGraph.setLocalFileHistoryGraph(localGraph);

        this.testThatAIsUnchanged();
        this.testThatBIsUnchanged();
        this.testThatCIsUnchanged();
        this.testThatDIsUnchanged();
    }

    private void testThatAIsUnchanged() {
        final IFileHistoryNode aNodeR2 = this.virtualFileHistoryGraph.getNodeFor(this.file("/dir1/dir2/a.txt", this.rev(2)));
        assertThat(aNodeR2, is(not(nullValue())));
        assertThat(aNodeR2.getType(), is(equalTo(IFileHistoryNode.Type.CHANGED)));
        final IFileHistoryNode aNodeL = this.virtualFileHistoryGraph.getNodeFor(this.file("/dir1/dir2/a.txt", this.localRev()));
        assertThat(aNodeL, is(nullValue()));

        assertThat(aNodeR2.getDescendants().isEmpty(), is(equalTo(true)));
    }

    private void testThatBIsUnchanged() {
        final IFileHistoryNode bNodeR2 = this.virtualFileHistoryGraph.getNodeFor(this.file("/dir1/dir2/b.txt", this.rev(2)));
        assertThat(bNodeR2, is(not(nullValue())));
        assertThat(bNodeR2.getType(), is(equalTo(IFileHistoryNode.Type.DELETED)));
        final IFileHistoryNode bNodeL = this.virtualFileHistoryGraph.getNodeFor(this.file("/dir1/dir2/b.txt", this.localRev()));
        assertThat(bNodeL, is(nullValue()));

        assertThat(bNodeR2.getDescendants().isEmpty(), is(equalTo(true)));
    }

    private void testThatCIsUnchanged() {
        final IFileHistoryNode cNodeR2 = this.virtualFileHistoryGraph.getNodeFor(this.file("/dir1/dir2/c.txt", this.rev(2)));
        assertThat(cNodeR2, is(not(nullValue())));
        assertThat(cNodeR2.getType(), is(equalTo(IFileHistoryNode.Type.REPLACED)));
        final IFileHistoryNode cNodeL = this.virtualFileHistoryGraph.getNodeFor(this.file("/dir1/dir2/c.txt", this.localRev()));
        assertThat(cNodeL, is(nullValue()));

        assertThat(cNodeR2.getDescendants().isEmpty(), is(equalTo(true)));
    }

    private void testThatDIsUnchanged() {
        final IFileHistoryNode dNodeR1 = this.virtualFileHistoryGraph.getNodeFor(this.file("/dir1/dir2/d.txt", this.rev(1)));
        assertThat(dNodeR1, is(not(nullValue())));
        assertThat(dNodeR1.getType(), is(equalTo(IFileHistoryNode.Type.ADDED)));
        final IFileHistoryNode dNodeR2 = this.virtualFileHistoryGraph.getNodeFor(this.file("/dir1/dir2/d.txt", this.rev(2)));
        assertThat(dNodeR2, is(nullValue()));
        final IFileHistoryNode dNodeL = this.virtualFileHistoryGraph.getNodeFor(this.file("/dir1/dir2/d.txt", this.localRev()));
        assertThat(dNodeL, is(nullValue()));

        assertThat(dNodeR1.getDescendants().isEmpty(), is(equalTo(true)));
    }

    @Test
    public void testLocalAddition() {
        final IMutableFileHistoryGraph localGraph = new TestFileHistoryGraph();
        localGraph.addAddition("/dir1/dir2/b.txt", this.localRev());
        localGraph.addAddition("/dir1/dir2/e.txt", this.localRev());
        this.virtualFileHistoryGraph.setLocalFileHistoryGraph(localGraph);

        this.testThatAIsUnchanged();

        final IFileHistoryNode bNodeR2 = this.virtualFileHistoryGraph.getNodeFor(this.file("/dir1/dir2/b.txt", this.rev(2)));
        assertThat(bNodeR2, is(not(nullValue())));
        assertThat(bNodeR2.getType(), is(equalTo(IFileHistoryNode.Type.DELETED)));
        final IFileHistoryNode bNodeL = this.virtualFileHistoryGraph.getNodeFor(this.file("/dir1/dir2/b.txt", this.localRev()));
        assertThat(bNodeL, is(not(nullValue())));
        assertThat(bNodeL.getType(), is(equalTo(IFileHistoryNode.Type.ADDED)));

        assertThat(bNodeR2.getDescendants().isEmpty(), is(equalTo(true)));
        assertThat(this.hasOnlyAlphaAncestor(bNodeL), is(equalTo(true)));

        this.testThatCIsUnchanged();
        this.testThatDIsUnchanged();

        final IFileHistoryNode eNodeR2 = this.virtualFileHistoryGraph.getNodeFor(this.file("/dir1/dir2/e.txt", this.rev(2)));
        assertThat(eNodeR2, is(nullValue()));
        final IFileHistoryNode eNodeL = this.virtualFileHistoryGraph.getNodeFor(this.file("/dir1/dir2/e.txt", this.localRev()));
        assertThat(eNodeL, is(not(nullValue())));
        assertThat(eNodeL.getType(), is(equalTo(IFileHistoryNode.Type.ADDED)));

        assertThat(this.hasOnlyAlphaAncestor(eNodeL), is(equalTo(true)));
    }

    @Test
    public void testLocalChangeInSameRevision() {
        final IMutableFileHistoryGraph localGraph = new TestFileHistoryGraph();
        localGraph.addChange("/dir1/dir2/a.txt", this.localRev(), Collections.singleton(this.rev(2)));
        localGraph.addChange("/dir1/dir2/c.txt", this.localRev(), Collections.singleton(this.rev(2)));
        localGraph.addChange("/dir1/dir2/d.txt", this.localRev(), Collections.singleton(this.rev(2)));
        this.virtualFileHistoryGraph.setLocalFileHistoryGraph(localGraph);

        final IFileHistoryNode aNodeR2 = this.virtualFileHistoryGraph.getNodeFor(this.file("/dir1/dir2/a.txt", this.rev(2)));
        assertThat(aNodeR2, is(not(nullValue())));
        assertThat(aNodeR2.getType(), is(equalTo(IFileHistoryNode.Type.CHANGED)));
        final IFileHistoryNode aNodeL = this.virtualFileHistoryGraph.getNodeFor(this.file("/dir1/dir2/a.txt", this.localRev()));
        assertThat(aNodeL, is(not(nullValue())));
        assertThat(aNodeL.getType(), is(equalTo(IFileHistoryNode.Type.CHANGED)));

        final IFileHistoryEdge aEdgeR2L = new VirtualFileHistoryEdge(
                this.virtualFileHistoryGraph,
                aNodeR2,
                aNodeL,
                IFileHistoryEdge.Type.NORMAL,
                new FileDiff(aNodeR2.getFile(), aNodeL.getFile()));
        assertEquals(aNodeR2.getDescendants(), Collections.singleton(aEdgeR2L));
        assertEquals(aNodeL.getAncestors(), Collections.singleton(aEdgeR2L));

        this.testThatBIsUnchanged();

        final IFileHistoryNode cNodeR2 = this.virtualFileHistoryGraph.getNodeFor(this.file("/dir1/dir2/c.txt", this.rev(2)));
        assertThat(cNodeR2, is(not(nullValue())));
        assertThat(cNodeR2.getType(), is(equalTo(IFileHistoryNode.Type.REPLACED)));
        final IFileHistoryNode cNodeL = this.virtualFileHistoryGraph.getNodeFor(this.file("/dir1/dir2/c.txt", this.localRev()));
        assertThat(cNodeL, is(not(nullValue())));
        assertThat(cNodeL.getType(), is(equalTo(IFileHistoryNode.Type.CHANGED)));

        final IFileHistoryEdge cEdgeR2L = new VirtualFileHistoryEdge(
                this.virtualFileHistoryGraph,
                cNodeR2,
                cNodeL,
                IFileHistoryEdge.Type.NORMAL,
                new FileDiff(cNodeR2.getFile(), cNodeL.getFile()));
        assertEquals(cNodeR2.getDescendants(), Collections.singleton(cEdgeR2L));
        assertEquals(cNodeL.getAncestors(), Collections.singleton(cEdgeR2L));

        final IFileHistoryNode dNodeR1 = this.virtualFileHistoryGraph.getNodeFor(this.file("/dir1/dir2/d.txt", this.rev(1)));
        assertThat(dNodeR1, is(not(nullValue())));
        assertThat(dNodeR1.getType(), is(equalTo(IFileHistoryNode.Type.ADDED)));
        final IFileHistoryNode dNodeR2 = this.virtualFileHistoryGraph.getNodeFor(this.file("/dir1/dir2/d.txt", this.rev(2)));
        assertThat(dNodeR2, is(not(nullValue())));
        assertThat(dNodeR2.getType(), is(equalTo(IFileHistoryNode.Type.UNCONFIRMED)));
        final IFileHistoryNode dNodeL = this.virtualFileHistoryGraph.getNodeFor(this.file("/dir1/dir2/d.txt", this.localRev()));
        assertThat(dNodeL, is(not(nullValue())));
        assertThat(dNodeL.getType(), is(equalTo(IFileHistoryNode.Type.CHANGED)));

        final IFileHistoryEdge dEdgeR1R2 = new VirtualFileHistoryEdge(
                this.virtualFileHistoryGraph,
                dNodeR1,
                dNodeR2,
                IFileHistoryEdge.Type.NORMAL,
                new FileDiff(dNodeR1.getFile(), dNodeR2.getFile()));
        final IFileHistoryEdge dEdgeR2L = new VirtualFileHistoryEdge(
                this.virtualFileHistoryGraph,
                dNodeR2,
                dNodeL,
                IFileHistoryEdge.Type.NORMAL,
                new FileDiff(dNodeR2.getFile(), dNodeL.getFile()));
        assertEquals(dNodeR1.getDescendants(), Collections.singleton(dEdgeR1R2));
        assertEquals(dNodeR2.getAncestors(), Collections.singleton(dEdgeR1R2));
        assertEquals(dNodeR2.getDescendants(), Collections.singleton(dEdgeR2L));
        assertEquals(dNodeL.getAncestors(), Collections.singleton(dEdgeR2L));
    }

    @Test
    public void testLocalChangeInLaterRevision() {
        final IMutableFileHistoryGraph localGraph = new TestFileHistoryGraph();
        localGraph.addChange("/dir1/dir2/a.txt", this.localRev(), Collections.singleton(this.rev(3)));
        localGraph.addChange("/dir1/dir2/c.txt", this.localRev(), Collections.singleton(this.rev(3)));
        localGraph.addChange("/dir1/dir2/d.txt", this.localRev(), Collections.singleton(this.rev(3)));
        this.virtualFileHistoryGraph.setLocalFileHistoryGraph(localGraph);

        final IFileHistoryNode aNodeR2 = this.virtualFileHistoryGraph.getNodeFor(this.file("/dir1/dir2/a.txt", this.rev(2)));
        assertThat(aNodeR2, is(not(nullValue())));
        assertThat(aNodeR2.getType(), is(equalTo(IFileHistoryNode.Type.CHANGED)));
        final IFileHistoryNode aNodeR3 = this.virtualFileHistoryGraph.getNodeFor(this.file("/dir1/dir2/a.txt", this.rev(3)));
        assertThat(aNodeR3, is(not(nullValue())));
        assertThat(aNodeR3.getType(), is(equalTo(IFileHistoryNode.Type.UNCONFIRMED)));
        final IFileHistoryNode aNodeL = this.virtualFileHistoryGraph.getNodeFor(this.file("/dir1/dir2/a.txt", this.localRev()));
        assertThat(aNodeL, is(not(nullValue())));
        assertThat(aNodeL.getType(), is(equalTo(IFileHistoryNode.Type.CHANGED)));

        final IFileHistoryEdge aEdgeR2R3 = new VirtualFileHistoryEdge(
                this.virtualFileHistoryGraph,
                aNodeR2,
                aNodeR3,
                IFileHistoryEdge.Type.NORMAL,
                new FileDiff(aNodeR2.getFile(), aNodeR3.getFile()));
        assertEquals(aNodeR2.getDescendants(), Collections.singleton(aEdgeR2R3));
        assertEquals(aNodeR3.getAncestors(), Collections.singleton(aEdgeR2R3));
        final IFileHistoryEdge aEdgeR3L = new VirtualFileHistoryEdge(
                this.virtualFileHistoryGraph,
                aNodeR3,
                aNodeL,
                IFileHistoryEdge.Type.NORMAL,
                new FileDiff(aNodeR3.getFile(), aNodeL.getFile()));
        assertEquals(aNodeR3.getDescendants(), Collections.singleton(aEdgeR3L));
        assertEquals(aNodeL.getAncestors(), Collections.singleton(aEdgeR3L));

        this.testThatBIsUnchanged();

        final IFileHistoryNode cNodeR2 = this.virtualFileHistoryGraph.getNodeFor(this.file("/dir1/dir2/c.txt", this.rev(2)));
        assertThat(cNodeR2, is(not(nullValue())));
        assertThat(cNodeR2.getType(), is(equalTo(IFileHistoryNode.Type.REPLACED)));
        final IFileHistoryNode cNodeR3 = this.virtualFileHistoryGraph.getNodeFor(this.file("/dir1/dir2/c.txt", this.rev(3)));
        assertThat(cNodeR3, is(not(nullValue())));
        assertThat(cNodeR3.getType(), is(equalTo(IFileHistoryNode.Type.UNCONFIRMED)));
        final IFileHistoryNode cNodeL = this.virtualFileHistoryGraph.getNodeFor(this.file("/dir1/dir2/c.txt", this.localRev()));
        assertThat(cNodeL, is(not(nullValue())));
        assertThat(cNodeL.getType(), is(equalTo(IFileHistoryNode.Type.CHANGED)));

        final IFileHistoryEdge cEdgeR2R3 = new VirtualFileHistoryEdge(
                this.virtualFileHistoryGraph,
                cNodeR2,
                cNodeR3,
                IFileHistoryEdge.Type.NORMAL,
                new FileDiff(cNodeR2.getFile(), cNodeR3.getFile()));
        assertEquals(cNodeR2.getDescendants(), Collections.singleton(cEdgeR2R3));
        assertEquals(cNodeR3.getAncestors(), Collections.singleton(cEdgeR2R3));
        final IFileHistoryEdge cEdgeR3L = new VirtualFileHistoryEdge(
                this.virtualFileHistoryGraph,
                cNodeR3,
                cNodeL,
                IFileHistoryEdge.Type.NORMAL,
                new FileDiff(cNodeR3.getFile(), cNodeL.getFile()));
        assertEquals(cNodeR3.getDescendants(), Collections.singleton(cEdgeR3L));
        assertEquals(cNodeL.getAncestors(), Collections.singleton(cEdgeR3L));

        final IFileHistoryNode dNodeR1 = this.virtualFileHistoryGraph.getNodeFor(this.file("/dir1/dir2/d.txt", this.rev(1)));
        assertThat(dNodeR1, is(not(nullValue())));
        assertThat(dNodeR1.getType(), is(equalTo(IFileHistoryNode.Type.ADDED)));
        final IFileHistoryNode dNodeR3 = this.virtualFileHistoryGraph.getNodeFor(this.file("/dir1/dir2/d.txt", this.rev(3)));
        assertThat(dNodeR3, is(not(nullValue())));
        assertThat(dNodeR3.getType(), is(equalTo(IFileHistoryNode.Type.UNCONFIRMED)));
        final IFileHistoryNode dNodeL = this.virtualFileHistoryGraph.getNodeFor(this.file("/dir1/dir2/d.txt", this.localRev()));
        assertThat(dNodeL, is(not(nullValue())));
        assertThat(dNodeL.getType(), is(equalTo(IFileHistoryNode.Type.CHANGED)));

        final IFileHistoryEdge dEdgeR1R3 = new VirtualFileHistoryEdge(
                this.virtualFileHistoryGraph,
                dNodeR1,
                dNodeR3,
                IFileHistoryEdge.Type.NORMAL,
                new FileDiff(dNodeR1.getFile(), dNodeR3.getFile()));
        assertEquals(dNodeR1.getDescendants(), Collections.singleton(dEdgeR1R3));
        assertEquals(dNodeR3.getAncestors(), Collections.singleton(dEdgeR1R3));
        final IFileHistoryEdge dEdgeR3L = new VirtualFileHistoryEdge(
                this.virtualFileHistoryGraph,
                dNodeR3,
                dNodeL,
                IFileHistoryEdge.Type.NORMAL,
                new FileDiff(dNodeR3.getFile(), dNodeL.getFile()));
        assertEquals(dNodeR3.getDescendants(), Collections.singleton(dEdgeR3L));
        assertEquals(dNodeL.getAncestors(), Collections.singleton(dEdgeR3L));
    }

    @Test
    public void testLocalDeletion() {
        final IMutableFileHistoryGraph localGraph = new TestFileHistoryGraph();
        localGraph.addDeletion("/dir1/dir2/a.txt", this.localRev());
        localGraph.addDeletion("/dir1/dir2/c.txt", this.localRev());
        localGraph.addDeletion("/dir1/dir2/d.txt", this.localRev());
        this.virtualFileHistoryGraph.setLocalFileHistoryGraph(localGraph);

        final IFileHistoryNode aNodeR2 = this.virtualFileHistoryGraph.getNodeFor(this.file("/dir1/dir2/a.txt", this.rev(2)));
        assertThat(aNodeR2, is(not(nullValue())));
        assertThat(aNodeR2.getType(), is(equalTo(IFileHistoryNode.Type.CHANGED)));
        final IFileHistoryNode aNodeL = this.virtualFileHistoryGraph.getNodeFor(this.file("/dir1/dir2/a.txt", this.localRev()));
        assertThat(aNodeL, is(not(nullValue())));
        assertThat(aNodeL.getType(), is(equalTo(IFileHistoryNode.Type.DELETED)));

        final IFileHistoryEdge aEdgeR2L = new VirtualFileHistoryEdge(
                this.virtualFileHistoryGraph,
                aNodeR2,
                aNodeL,
                IFileHistoryEdge.Type.NORMAL,
                new FileDiff(aNodeR2.getFile(), aNodeL.getFile()));
        assertEquals(aNodeR2.getDescendants(), Collections.singleton(aEdgeR2L));
        assertEquals(aNodeL.getAncestors(), Collections.singleton(aEdgeR2L));

        this.testThatBIsUnchanged();

        final IFileHistoryNode cNodeR2 = this.virtualFileHistoryGraph.getNodeFor(this.file("/dir1/dir2/c.txt", this.rev(2)));
        assertThat(cNodeR2, is(not(nullValue())));
        assertThat(cNodeR2.getType(), is(equalTo(IFileHistoryNode.Type.REPLACED)));
        final IFileHistoryNode cNodeL = this.virtualFileHistoryGraph.getNodeFor(this.file("/dir1/dir2/c.txt", this.localRev()));
        assertThat(cNodeL, is(not(nullValue())));
        assertThat(cNodeL.getType(), is(equalTo(IFileHistoryNode.Type.DELETED)));

        final IFileHistoryEdge cEdgeR2L = new VirtualFileHistoryEdge(
                this.virtualFileHistoryGraph,
                cNodeR2,
                cNodeL,
                IFileHistoryEdge.Type.NORMAL,
                new FileDiff(cNodeR2.getFile(), cNodeL.getFile()));
        assertEquals(cNodeR2.getDescendants(), Collections.singleton(cEdgeR2L));
        assertEquals(cNodeL.getAncestors(), Collections.singleton(cEdgeR2L));

        final IFileHistoryNode dNodeR1 = this.virtualFileHistoryGraph.getNodeFor(this.file("/dir1/dir2/d.txt", this.rev(1)));
        assertThat(dNodeR1, is(not(nullValue())));
        assertThat(dNodeR1.getType(), is(equalTo(IFileHistoryNode.Type.ADDED)));
        final IFileHistoryNode dNodeL = this.virtualFileHistoryGraph.getNodeFor(this.file("/dir1/dir2/d.txt", this.localRev()));
        assertThat(dNodeL, is(not(nullValue())));
        assertThat(dNodeL.getType(), is(equalTo(IFileHistoryNode.Type.DELETED)));

        final IFileHistoryEdge dEdgeR1L = new VirtualFileHistoryEdge(
                this.virtualFileHistoryGraph,
                dNodeR1,
                dNodeL,
                IFileHistoryEdge.Type.NORMAL,
                new FileDiff(dNodeR1.getFile(), dNodeL.getFile()));
        assertEquals(dNodeR1.getDescendants(), Collections.singleton(dEdgeR1L));
        assertEquals(dNodeL.getAncestors(), Collections.singleton(dEdgeR1L));
    }

    @Test
    public void testLocalCopyOfLatestRevision() {
        final IMutableFileHistoryGraph localGraph = new TestFileHistoryGraph();
        localGraph.addCopy("/dir1/dir2/a.txt", "/dir1/dir2/e.txt", this.rev(2), this.localRev());
        this.virtualFileHistoryGraph.setLocalFileHistoryGraph(localGraph);

        final IFileHistoryNode aNodeR2 = this.virtualFileHistoryGraph.getNodeFor(this.file("/dir1/dir2/a.txt", this.rev(2)));
        assertThat(aNodeR2, is(not(nullValue())));
        assertThat(aNodeR2.getType(), is(equalTo(IFileHistoryNode.Type.CHANGED)));
        final IFileHistoryNode eNodeL = this.virtualFileHistoryGraph.getNodeFor(this.file("/dir1/dir2/e.txt", this.localRev()));
        assertThat(eNodeL, is(not(nullValue())));
        assertThat(eNodeL.getType(), is(equalTo(IFileHistoryNode.Type.CHANGED)));

        final IFileHistoryEdge aEdgeR2L = new VirtualFileHistoryEdge(
                this.virtualFileHistoryGraph,
                aNodeR2,
                eNodeL,
                IFileHistoryEdge.Type.COPY,
                new FileDiff(aNodeR2.getFile(), eNodeL.getFile()));
        assertEquals(aNodeR2.getDescendants(), Collections.singleton(aEdgeR2L));
        assertEquals(eNodeL.getAncestors(), Collections.singleton(aEdgeR2L));

        this.testThatBIsUnchanged();
        this.testThatCIsUnchanged();
        this.testThatDIsUnchanged();
    }

    @Test
    public void testLocalCopyOfOlderRevision() {
        final IMutableFileHistoryGraph localGraph = new TestFileHistoryGraph();
        localGraph.addCopy("/dir1/dir2/a.txt", "/dir1/dir2/e.txt", this.rev(1), this.localRev());
        this.virtualFileHistoryGraph.setLocalFileHistoryGraph(localGraph);

        final IFileHistoryNode aNodeR1 = this.virtualFileHistoryGraph.getNodeFor(this.file("/dir1/dir2/a.txt", this.rev(1)));
        assertThat(aNodeR1, is(not(nullValue())));
        assertThat(aNodeR1.getType(), is(equalTo(IFileHistoryNode.Type.ADDED)));
        final IFileHistoryNode aNodeR2 = this.virtualFileHistoryGraph.getNodeFor(this.file("/dir1/dir2/a.txt", this.rev(2)));
        assertThat(aNodeR2, is(not(nullValue())));
        assertThat(aNodeR2.getType(), is(equalTo(IFileHistoryNode.Type.CHANGED)));
        final IFileHistoryNode eNodeL = this.virtualFileHistoryGraph.getNodeFor(this.file("/dir1/dir2/e.txt", this.localRev()));
        assertThat(eNodeL, is(not(nullValue())));
        assertThat(eNodeL.getType(), is(equalTo(IFileHistoryNode.Type.CHANGED)));

        final IFileHistoryEdge aEdgeR1R2 = new VirtualFileHistoryEdge(
                this.virtualFileHistoryGraph,
                aNodeR1,
                aNodeR2,
                IFileHistoryEdge.Type.NORMAL,
                new FileDiff(aNodeR1.getFile(), aNodeR2.getFile()));
        final IFileHistoryEdge aEdgeR1L = new VirtualFileHistoryEdge(
                this.virtualFileHistoryGraph,
                aNodeR1,
                eNodeL,
                IFileHistoryEdge.Type.COPY,
                new FileDiff(aNodeR1.getFile(), eNodeL.getFile()));
        assertEquals(aNodeR1.getDescendants(),
                new HashSet<>(Arrays.asList(aEdgeR1R2, aEdgeR1L)));
        assertEquals(aNodeR2.getAncestors(), Collections.singleton(aEdgeR1R2));
        assertThat(aNodeR2.getDescendants().isEmpty(), is(equalTo(true)));
        assertEquals(eNodeL.getAncestors(), Collections.singleton(aEdgeR1L));

        this.testThatBIsUnchanged();
        this.testThatCIsUnchanged();
        this.testThatDIsUnchanged();
    }

    @Test
    public void testLocalReplacement() {
        final IMutableFileHistoryGraph localGraph = new TestFileHistoryGraph();
        localGraph.addReplacement("/dir1/dir2/a.txt", this.localRev());
        localGraph.addReplacement("/dir1/dir2/c.txt", this.localRev());
        localGraph.addReplacement("/dir1/dir2/d.txt", this.localRev());
        this.virtualFileHistoryGraph.setLocalFileHistoryGraph(localGraph);

        final IFileHistoryNode aNodeR2 = this.virtualFileHistoryGraph.getNodeFor(this.file("/dir1/dir2/a.txt", this.rev(2)));
        assertThat(aNodeR2, is(not(nullValue())));
        assertThat(aNodeR2.getType(), is(equalTo(IFileHistoryNode.Type.CHANGED)));
        final IFileHistoryNode aNodeL = this.virtualFileHistoryGraph.getNodeFor(this.file("/dir1/dir2/a.txt", this.localRev()));
        assertThat(aNodeL, is(not(nullValue())));
        assertThat(aNodeL.getType(), is(equalTo(IFileHistoryNode.Type.REPLACED)));

        final IFileHistoryEdge aEdgeR2L = new VirtualFileHistoryEdge(
                this.virtualFileHistoryGraph,
                aNodeR2,
                aNodeL,
                IFileHistoryEdge.Type.NORMAL,
                new FileDiff(aNodeR2.getFile(), aNodeL.getFile()));
        assertEquals(aNodeR2.getDescendants(), Collections.singleton(aEdgeR2L));
        assertEquals(aNodeL.getAncestors(), Collections.singleton(aEdgeR2L));

        this.testThatBIsUnchanged();

        final IFileHistoryNode cNodeR2 = this.virtualFileHistoryGraph.getNodeFor(this.file("/dir1/dir2/c.txt", this.rev(2)));
        assertThat(cNodeR2, is(not(nullValue())));
        assertThat(cNodeR2.getType(), is(equalTo(IFileHistoryNode.Type.REPLACED)));
        final IFileHistoryNode cNodeL = this.virtualFileHistoryGraph.getNodeFor(this.file("/dir1/dir2/c.txt", this.localRev()));
        assertThat(cNodeL, is(not(nullValue())));
        assertThat(cNodeL.getType(), is(equalTo(IFileHistoryNode.Type.REPLACED)));

        final IFileHistoryEdge cEdgeR2L = new VirtualFileHistoryEdge(
                this.virtualFileHistoryGraph,
                cNodeR2,
                cNodeL,
                IFileHistoryEdge.Type.NORMAL,
                new FileDiff(cNodeR2.getFile(), cNodeL.getFile()));
        assertEquals(cNodeR2.getDescendants(), Collections.singleton(cEdgeR2L));
        assertEquals(cNodeL.getAncestors(), Collections.singleton(cEdgeR2L));

        final IFileHistoryNode dNodeR1 = this.virtualFileHistoryGraph.getNodeFor(this.file("/dir1/dir2/d.txt", this.rev(1)));
        assertThat(dNodeR1, is(not(nullValue())));
        assertThat(dNodeR1.getType(), is(equalTo(IFileHistoryNode.Type.ADDED)));
        final IFileHistoryNode dNodeL = this.virtualFileHistoryGraph.getNodeFor(this.file("/dir1/dir2/d.txt", this.localRev()));
        assertThat(dNodeL, is(not(nullValue())));
        assertThat(dNodeL.getType(), is(equalTo(IFileHistoryNode.Type.REPLACED)));

        final IFileHistoryEdge dEdgeR1L = new VirtualFileHistoryEdge(
                this.virtualFileHistoryGraph,
                dNodeR1,
                dNodeL,
                IFileHistoryEdge.Type.NORMAL,
                new FileDiff(dNodeR1.getFile(), dNodeL.getFile()));
        assertEquals(dNodeR1.getDescendants(), Collections.singleton(dEdgeR1L));
        assertEquals(dNodeL.getAncestors(), Collections.singleton(dEdgeR1L));
    }

    @Test
    public void testLocalReplacementByCopyOfLatestRevision() {
        final IMutableFileHistoryGraph localGraph = new TestFileHistoryGraph();
        localGraph.addReplacement("/dir1/dir2/c.txt", this.localRev(), "/dir1/dir2/a.txt", this.rev(2));
        this.virtualFileHistoryGraph.setLocalFileHistoryGraph(localGraph);

        final IFileHistoryNode aNodeR2 = this.virtualFileHistoryGraph.getNodeFor(this.file("/dir1/dir2/a.txt", this.rev(2)));
        assertThat(aNodeR2, is(not(nullValue())));
        assertThat(aNodeR2.getType(), is(equalTo(IFileHistoryNode.Type.CHANGED)));
        final IFileHistoryNode cNodeR2 = this.virtualFileHistoryGraph.getNodeFor(this.file("/dir1/dir2/c.txt", this.rev(2)));
        assertThat(cNodeR2, is(not(nullValue())));
        assertThat(cNodeR2.getType(), is(equalTo(IFileHistoryNode.Type.REPLACED)));
        final IFileHistoryNode cNodeL = this.virtualFileHistoryGraph.getNodeFor(this.file("/dir1/dir2/c.txt", this.localRev()));
        assertThat(cNodeL, is(not(nullValue())));
        assertThat(cNodeL.getType(), is(equalTo(IFileHistoryNode.Type.REPLACED)));

        final IFileHistoryEdge aEdgeR2L = new VirtualFileHistoryEdge(
                this.virtualFileHistoryGraph,
                aNodeR2,
                cNodeL,
                IFileHistoryEdge.Type.COPY,
                new FileDiff(aNodeR2.getFile(), cNodeL.getFile()));
        final IFileHistoryEdge cEdgeR2L = new VirtualFileHistoryEdge(
                this.virtualFileHistoryGraph,
                cNodeR2,
                cNodeL,
                IFileHistoryEdge.Type.NORMAL,
                new FileDiff(cNodeR2.getFile(), cNodeL.getFile()));
        assertEquals(aNodeR2.getDescendants(), Collections.singleton(aEdgeR2L));
        assertEquals(cNodeR2.getDescendants(), Collections.singleton(cEdgeR2L));
        assertEquals(cNodeL.getAncestors(),
                new HashSet<>(Arrays.asList(cEdgeR2L, aEdgeR2L)));

        this.testThatBIsUnchanged();
        this.testThatDIsUnchanged();
    }

    @Test
    public void testLocalReplacementByCopyOfOlderRevision() {
        final IMutableFileHistoryGraph localGraph = new TestFileHistoryGraph();
        localGraph.addReplacement("/dir1/dir2/c.txt", this.localRev(), "/dir1/dir2/a.txt", this.rev(1));
        this.virtualFileHistoryGraph.setLocalFileHistoryGraph(localGraph);

        final IFileHistoryNode aNodeR1 = this.virtualFileHistoryGraph.getNodeFor(this.file("/dir1/dir2/a.txt", this.rev(1)));
        assertThat(aNodeR1, is(not(nullValue())));
        assertThat(aNodeR1.getType(), is(equalTo(IFileHistoryNode.Type.ADDED)));
        final IFileHistoryNode aNodeR2 = this.virtualFileHistoryGraph.getNodeFor(this.file("/dir1/dir2/a.txt", this.rev(2)));
        assertThat(aNodeR2, is(not(nullValue())));
        assertThat(aNodeR2.getType(), is(equalTo(IFileHistoryNode.Type.CHANGED)));
        final IFileHistoryNode cNodeR2 = this.virtualFileHistoryGraph.getNodeFor(this.file("/dir1/dir2/c.txt", this.rev(2)));
        assertThat(cNodeR2, is(not(nullValue())));
        assertThat(cNodeR2.getType(), is(equalTo(IFileHistoryNode.Type.REPLACED)));
        final IFileHistoryNode cNodeL = this.virtualFileHistoryGraph.getNodeFor(this.file("/dir1/dir2/c.txt", this.localRev()));
        assertThat(cNodeL, is(not(nullValue())));
        assertThat(cNodeL.getType(), is(equalTo(IFileHistoryNode.Type.REPLACED)));

        final IFileHistoryEdge aEdgeR1R2 = new VirtualFileHistoryEdge(
                this.virtualFileHistoryGraph,
                aNodeR1,
                aNodeR2,
                IFileHistoryEdge.Type.NORMAL,
                new FileDiff(aNodeR1.getFile(), aNodeR2.getFile()));
        final IFileHistoryEdge aEdgeR1L = new VirtualFileHistoryEdge(
                this.virtualFileHistoryGraph,
                aNodeR1,
                cNodeL,
                IFileHistoryEdge.Type.COPY,
                new FileDiff(aNodeR1.getFile(), cNodeL.getFile()));
        final IFileHistoryEdge cEdgeR2L = new VirtualFileHistoryEdge(
                this.virtualFileHistoryGraph,
                cNodeR2,
                cNodeL,
                IFileHistoryEdge.Type.NORMAL,
                new FileDiff(cNodeR2.getFile(), cNodeL.getFile()));

        assertEquals(aNodeR1.getDescendants(),
                new HashSet<>(Arrays.asList(aEdgeR1R2, aEdgeR1L)));
        assertEquals(aNodeR2.getAncestors(), Collections.singleton(aEdgeR1R2));
        assertThat(aNodeR2.getDescendants().isEmpty(), is(equalTo(true)));
        assertEquals(cNodeR2.getDescendants(), Collections.singleton(cEdgeR2L));
        assertEquals(cNodeL.getAncestors(),
                new HashSet<>(Arrays.asList(cEdgeR2L, aEdgeR1L)));

        this.testThatBIsUnchanged();
        this.testThatDIsUnchanged();
    }
}
