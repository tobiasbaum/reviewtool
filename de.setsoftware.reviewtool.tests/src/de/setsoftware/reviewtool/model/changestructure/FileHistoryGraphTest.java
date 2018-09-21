package de.setsoftware.reviewtool.model.changestructure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import de.setsoftware.reviewtool.diffalgorithms.DiffAlgorithmFactory;
import de.setsoftware.reviewtool.model.api.IFileHistoryEdge;
import de.setsoftware.reviewtool.model.api.IFileHistoryNode;
import de.setsoftware.reviewtool.model.api.IRepository;
import de.setsoftware.reviewtool.model.api.IRevisionedFile;

/**
 * Tests for {@link FileHistoryGraph}.
 */
public final class FileHistoryGraphTest {

    private static FileHistoryGraph graph() {
        return new FileHistoryGraph(DiffAlgorithmFactory.createDefault());
    }

    private static FileHistoryEdge createAlphaNode(
            final IRepository repo,
            final FileHistoryGraph g,
            final ProxyableFileHistoryNode node) {
        final ProxyableFileHistoryNode alphaNode = g.getNodeFor(
                ChangestructureFactory.createFileInRevision(node.getFile().getPath(),
                        ChangestructureFactory.createUnknownRevision(repo)));
        final FileHistoryEdge alphaEdge = new FileHistoryEdge(g, alphaNode, node, IFileHistoryEdge.Type.NORMAL);
        return alphaEdge;
    }

    @Test
    public void testAdditionOfFile() {
        final IRepository repo = new TestRepository("123");
        final FileHistoryGraph g = graph();

        final IRevisionedFile aRev =
                ChangestructureFactory.createFileInRevision("/trunk/a", new TestRepoRevision(repo, 2L));
        g.addAddition(aRev.getPath(), aRev.getRevision());

        final ProxyableFileHistoryNode aNode = g.getNodeFor(aRev);
        assertEquals(aRev, aNode.getFile());
        assertEquals(IFileHistoryNode.Type.ADDED, aNode.getType());
        assertEquals(false, aNode.isCopyTarget());

        assertEquals(Collections.singleton(createAlphaNode(repo, g, aNode)), aNode.getAncestors());
        assertEquals(Collections.emptySet(), aNode.getDescendants());
    }

    @Test
    public void testDeletionOfKnownFile() {
        final IRepository repo = new TestRepository("123");
        final FileHistoryGraph g = graph();

        final IRevisionedFile aRevPrev =
                ChangestructureFactory.createFileInRevision("/trunk/a", new TestRepoRevision(repo, 1L));

        g.addAddition(aRevPrev.getPath(), aRevPrev.getRevision());

        final IRevisionedFile aRevDel =
                ChangestructureFactory.createFileInRevision("/trunk/a", new TestRepoRevision(repo, 2L));

        g.addDeletion(aRevDel.getPath(), aRevDel.getRevision());

        final ProxyableFileHistoryNode aNode = g.getNodeFor(aRevPrev);
        assertEquals(aRevPrev, aNode.getFile());
        assertEquals(IFileHistoryNode.Type.ADDED, aNode.getType());
        assertEquals(false, aNode.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, aNode)), aNode.getAncestors());

        final ProxyableFileHistoryNode aNodeDel = g.getNodeFor(aRevDel);
        assertEquals(aRevDel, aNodeDel.getFile());
        assertEquals(IFileHistoryNode.Type.DELETED, aNodeDel.getType());
        assertEquals(false, aNodeDel.isCopyTarget());
        assertEquals(Collections.emptySet(), aNodeDel.getDescendants());

        final FileHistoryEdge aEdge = new FileHistoryEdge(g, aNode, aNodeDel, IFileHistoryEdge.Type.NORMAL);
        assertEquals(Collections.singleton(aEdge), aNode.getDescendants());
        assertEquals(Collections.singleton(aEdge), aNodeDel.getAncestors());
    }

    @Test
    public void testDeletionOfUnknownFile() {
        final IRepository repo = new TestRepository("123");
        final FileHistoryGraph g = graph();

        final IRevisionedFile aRevPrev =
                ChangestructureFactory.createFileInRevision("/trunk/a",
                        ChangestructureFactory.createUnknownRevision(repo));
        final IRevisionedFile aRevDel =
                ChangestructureFactory.createFileInRevision("/trunk/a", new TestRepoRevision(repo, 2L));

        g.addDeletion(aRevDel.getPath(), aRevDel.getRevision());

        final ProxyableFileHistoryNode aNodeDel = g.getNodeFor(aRevDel);
        assertEquals(aRevDel, aNodeDel.getFile());
        assertEquals(IFileHistoryNode.Type.DELETED, aNodeDel.getType());
        assertEquals(false, aNodeDel.isCopyTarget());
        assertEquals(Collections.emptySet(), aNodeDel.getDescendants());

        final ProxyableFileHistoryNode aNodePrev = g.getNodeFor(aRevPrev);
        assertEquals(aRevPrev, aNodePrev.getFile());
        assertEquals(IFileHistoryNode.Type.UNCONFIRMED, aNodePrev.getType());
        assertEquals(false, aNodePrev.isCopyTarget());
        assertEquals(Collections.emptySet(), aNodePrev.getAncestors());

        final FileHistoryEdge aEdge = new FileHistoryEdge(g, aNodePrev, aNodeDel, IFileHistoryEdge.Type.NORMAL);
        assertEquals(Collections.singleton(aEdge), aNodePrev.getDescendants());
        assertEquals(Collections.singleton(aEdge), aNodeDel.getAncestors());
    }

    @Test
    public void testDeletionOfKnownMovedFile() {
        final IRepository repo = new TestRepository("123");
        final FileHistoryGraph g = graph();

        final IRevisionedFile xRev =
                ChangestructureFactory.createFileInRevision("/trunk/a/x", new TestRepoRevision(repo, 1L));

        g.addAddition(xRev.getPath(), xRev.getRevision());

        final IRevisionedFile xRevDel =
                ChangestructureFactory.createFileInRevision(xRev.getPath(), new TestRepoRevision(repo, 2L));
        final IRevisionedFile yRev =
                ChangestructureFactory.createFileInRevision("/trunk/a/y", new TestRepoRevision(repo, 2L));

        g.addCopy(xRev.getPath(), xRev.getRevision(), yRev.getPath(), yRev.getRevision());
        g.addDeletion(xRevDel.getPath(), xRevDel.getRevision());

        final IRevisionedFile yRevDel =
                ChangestructureFactory.createFileInRevision(yRev.getPath(), new TestRepoRevision(repo, 3L));

        g.addDeletion(yRevDel.getPath(), yRevDel.getRevision());

        // revision 1
        final ProxyableFileHistoryNode xNode = g.getNodeFor(xRev);
        assertEquals(xRev, xNode.getFile());
        assertEquals(IFileHistoryNode.Type.ADDED, xNode.getType());
        assertEquals(false, xNode.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, xNode)), xNode.getAncestors());

        // revision 2
        final ProxyableFileHistoryNode xNodeDel = g.getNodeFor(xRevDel);
        assertEquals(xRevDel, xNodeDel.getFile());
        assertEquals(IFileHistoryNode.Type.DELETED, xNodeDel.getType());
        assertEquals(false, xNodeDel.isCopyTarget());
        assertEquals(Collections.emptySet(), xNodeDel.getDescendants());

        final ProxyableFileHistoryNode yNode = g.getNodeFor(yRev);
        assertEquals(yRev, yNode.getFile());
        assertEquals(IFileHistoryNode.Type.CHANGED, yNode.getType());
        assertEquals(true, yNode.isCopyTarget());

        final FileHistoryEdge xEdgeDel = new FileHistoryEdge(g, xNode, xNodeDel, IFileHistoryEdge.Type.NORMAL);
        final FileHistoryEdge yEdge = new FileHistoryEdge(g, xNode, yNode, IFileHistoryEdge.Type.COPY);
        assertEquals(new HashSet<>(Arrays.asList(xEdgeDel, yEdge)), xNode.getDescendants());
        assertEquals(Collections.singleton(xEdgeDel), xNodeDel.getAncestors());
        assertEquals(Collections.singleton(yEdge), yNode.getAncestors());

        // revision 3
        final ProxyableFileHistoryNode yNodeDel = g.getNodeFor(yRevDel);
        assertEquals(yRevDel, yNodeDel.getFile());
        assertEquals(IFileHistoryNode.Type.DELETED, yNodeDel.getType());
        assertEquals(false, yNodeDel.isCopyTarget());

        final FileHistoryEdge yEdgeCopyDel = new FileHistoryEdge(g, yNode, yNodeDel,
                IFileHistoryEdge.Type.NORMAL);
        assertEquals(Collections.singleton(yEdgeCopyDel), yNodeDel.getAncestors());
        assertEquals(Collections.singleton(yEdgeCopyDel), yNode.getDescendants());
    }

    @Test
    public void testReplacementOfKnownFile() {
        final IRepository repo = new TestRepository("123");
        final FileHistoryGraph g = graph();

        final IRevisionedFile aRevPrev =
                ChangestructureFactory.createFileInRevision("/trunk/a", new TestRepoRevision(repo, 1L));

        g.addAddition(aRevPrev.getPath(), aRevPrev.getRevision());

        final IRevisionedFile aRevReplaced =
                ChangestructureFactory.createFileInRevision("/trunk/a", new TestRepoRevision(repo, 2L));

        g.addDeletion(aRevReplaced.getPath(), aRevReplaced.getRevision());
        g.addAddition(aRevReplaced.getPath(), aRevReplaced.getRevision());

        final ProxyableFileHistoryNode aNode = g.getNodeFor(aRevPrev);
        assertEquals(aRevPrev, aNode.getFile());
        assertEquals(IFileHistoryNode.Type.ADDED, aNode.getType());
        assertEquals(false, aNode.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, aNode)), aNode.getAncestors());

        final ProxyableFileHistoryNode aNodeReplaced = g.getNodeFor(aRevReplaced);
        assertEquals(aRevReplaced, aNodeReplaced.getFile());
        assertEquals(IFileHistoryNode.Type.REPLACED, aNodeReplaced.getType());
        assertEquals(false, aNodeReplaced.isCopyTarget());
        assertEquals(Collections.emptySet(), aNodeReplaced.getDescendants());

        final FileHistoryEdge aEdge = new FileHistoryEdge(g, aNode, aNodeReplaced, IFileHistoryEdge.Type.NORMAL);
        assertEquals(Collections.singleton(aEdge), aNode.getDescendants());
        assertEquals(Collections.singleton(aEdge), aNodeReplaced.getAncestors());
    }

    @Test
    public void testReplacementOfUnknownFile() {
        final IRepository repo = new TestRepository("123");
        final FileHistoryGraph g = graph();

        final IRevisionedFile aRevPrev =
                ChangestructureFactory.createFileInRevision("/trunk/a",
                        ChangestructureFactory.createUnknownRevision(repo));
        final IRevisionedFile aRevReplaced =
                ChangestructureFactory.createFileInRevision("/trunk/a", new TestRepoRevision(repo, 2L));

        g.addDeletion(aRevReplaced.getPath(), aRevReplaced.getRevision());
        g.addAddition(aRevReplaced.getPath(), aRevReplaced.getRevision());

        final ProxyableFileHistoryNode aNodePrev = g.getNodeFor(aRevPrev);
        assertEquals(aRevPrev, aNodePrev.getFile());
        assertEquals(IFileHistoryNode.Type.UNCONFIRMED, aNodePrev.getType());
        assertEquals(false, aNodePrev.isCopyTarget());
        assertEquals(Collections.emptySet(), aNodePrev.getAncestors());

        final ProxyableFileHistoryNode aNodeReplaced = g.getNodeFor(aRevReplaced);
        assertEquals(aRevReplaced, aNodeReplaced.getFile());
        assertEquals(IFileHistoryNode.Type.REPLACED, aNodeReplaced.getType());
        assertEquals(false, aNodeReplaced.isCopyTarget());
        assertEquals(Collections.emptySet(), aNodeReplaced.getDescendants());

        final FileHistoryEdge aEdge = new FileHistoryEdge(g, aNodePrev, aNodeReplaced, IFileHistoryEdge.Type.NORMAL);
        assertEquals(Collections.singleton(aEdge), aNodePrev.getDescendants());
        assertEquals(Collections.singleton(aEdge), aNodeReplaced.getAncestors());
    }

    @Test
    public void testAdditionAndDeletionOfSameFileInSubsequentRevisions() {
        final IRepository repo = new TestRepository("123");
        final FileHistoryGraph g = graph();

        final IRevisionedFile aRevNew =
                ChangestructureFactory.createFileInRevision("/trunk/a", new TestRepoRevision(repo, 1L));
        final IRevisionedFile aRevDel =
                ChangestructureFactory.createFileInRevision("/trunk/a", new TestRepoRevision(repo, 2L));

        g.addAddition(aRevNew.getPath(), aRevNew.getRevision());
        g.addDeletion(aRevDel.getPath(), aRevDel.getRevision());

        final ProxyableFileHistoryNode aNode = g.getNodeFor(aRevNew);
        assertEquals(aRevNew, aNode.getFile());
        assertEquals(IFileHistoryNode.Type.ADDED, aNode.getType());
        assertEquals(false, aNode.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, aNode)), aNode.getAncestors());

        final ProxyableFileHistoryNode aNodeDel = g.getNodeFor(aRevDel);
        assertEquals(aRevDel, aNodeDel.getFile());
        assertEquals(IFileHistoryNode.Type.DELETED, aNodeDel.getType());
        assertEquals(false, aNodeDel.isCopyTarget());
        assertEquals(Collections.emptySet(), aNodeDel.getDescendants());

        final FileHistoryEdge aEdge = new FileHistoryEdge(g, aNode, aNodeDel, IFileHistoryEdge.Type.NORMAL);
        assertEquals(Collections.singleton(aEdge), aNode.getDescendants());
        assertEquals(Collections.singleton(aEdge), aNodeDel.getAncestors());
    }

    @Test
    public void testAdditionDeletionAndAdditionOfSameFile() {
        final IRepository repo = new TestRepository("123");
        final FileHistoryGraph g = graph();

        final IRevisionedFile aRevNew =
                ChangestructureFactory.createFileInRevision("/trunk/a", new TestRepoRevision(repo, 1L));
        final IRevisionedFile aRevDel =
                ChangestructureFactory.createFileInRevision("/trunk/a", new TestRepoRevision(repo, 2L));
        final IRevisionedFile aRevNewAgain =
                ChangestructureFactory.createFileInRevision("/trunk/a", new TestRepoRevision(repo, 3L));

        g.addAddition(aRevNew.getPath(), aRevNew.getRevision());
        g.addDeletion(aRevDel.getPath(), aRevDel.getRevision());
        g.addAddition(aRevNew.getPath(), aRevNewAgain.getRevision());

        final ProxyableFileHistoryNode aNode = g.getNodeFor(aRevNew);
        assertEquals(aRevNew, aNode.getFile());
        assertEquals(IFileHistoryNode.Type.ADDED, aNode.getType());
        assertEquals(false, aNode.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, aNode)), aNode.getAncestors());

        final ProxyableFileHistoryNode aNodeDel = g.getNodeFor(aRevDel);
        assertEquals(aRevDel, aNodeDel.getFile());
        assertEquals(IFileHistoryNode.Type.DELETED, aNodeDel.getType());
        assertEquals(false, aNodeDel.isCopyTarget());
        assertEquals(Collections.emptySet(), aNodeDel.getDescendants());

        final ProxyableFileHistoryNode aNodeAgain = g.getNodeFor(aRevNewAgain);
        assertEquals(aRevNewAgain, aNodeAgain.getFile());
        assertEquals(IFileHistoryNode.Type.ADDED, aNodeAgain.getType());
        assertEquals(false, aNodeAgain.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, aNodeAgain)), aNodeAgain.getAncestors());

        final FileHistoryEdge aEdge = new FileHistoryEdge(g, aNode, aNodeDel, IFileHistoryEdge.Type.NORMAL);
        assertEquals(Collections.singleton(aEdge), aNode.getDescendants());
        assertEquals(Collections.singleton(aEdge), aNodeDel.getAncestors());
    }

    @Test
    public void testChangeOfKnownFile() {
        final IRepository repo = new TestRepository("123");
        final FileHistoryGraph g = graph();

        final IRevisionedFile aRevOrig =
                ChangestructureFactory.createFileInRevision("/trunk/a", new TestRepoRevision(repo, 1L));

        g.addAddition(aRevOrig.getPath(), aRevOrig.getRevision());

        final IRevisionedFile aRevChanged =
                ChangestructureFactory.createFileInRevision(aRevOrig.getPath(), new TestRepoRevision(repo, 2L));

        g.addChange(aRevChanged.getPath(), aRevChanged.getRevision(), Collections.singleton(aRevOrig.getRevision()));

        final ProxyableFileHistoryNode aOrigNode = g.getNodeFor(aRevOrig);
        assertEquals(aRevOrig, aOrigNode.getFile());
        assertEquals(IFileHistoryNode.Type.ADDED, aOrigNode.getType());
        assertEquals(false, aOrigNode.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, aOrigNode)), aOrigNode.getAncestors());

        final ProxyableFileHistoryNode aChangedNode = g.getNodeFor(aRevChanged);
        assertEquals(aRevChanged, aChangedNode.getFile());
        assertEquals(IFileHistoryNode.Type.CHANGED, aChangedNode.getType());
        assertEquals(false, aChangedNode.isCopyTarget());
        assertEquals(Collections.emptySet(), aChangedNode.getDescendants());

        final FileHistoryEdge aEdge = new FileHistoryEdge(g, aOrigNode, aChangedNode, IFileHistoryEdge.Type.NORMAL);
        assertEquals(Collections.singleton(aEdge), aOrigNode.getDescendants());
        assertEquals(Collections.singleton(aEdge), aChangedNode.getAncestors());
    }

    @Test
    public void testChangeOfUnknownFile() {
        final IRepository repo = new TestRepository("123");
        final FileHistoryGraph g = graph();

        final IRevisionedFile aRevChanged =
                ChangestructureFactory.createFileInRevision("/trunk/a", new TestRepoRevision(repo, 2L));

        g.addChange(aRevChanged.getPath(), aRevChanged.getRevision(),
                Collections.singleton(new TestRepoRevision(repo, 1L)));

        final ProxyableFileHistoryNode aChangedNode = g.getNodeFor(aRevChanged);
        assertEquals(aRevChanged, aChangedNode.getFile());
        assertEquals(IFileHistoryNode.Type.CHANGED, aChangedNode.getType());
        assertEquals(false, aChangedNode.isCopyTarget());
        assertEquals(Collections.emptySet(), aChangedNode.getDescendants());
    }

    @Test
    public void testCopyOfFile() {
        final IRepository repo = new TestRepository("123");
        final FileHistoryGraph g = graph();

        final IRevisionedFile aRevOrig =
                ChangestructureFactory.createFileInRevision("/trunk/a", new TestRepoRevision(repo, 1L));
        final IRevisionedFile aRevCopy =
                ChangestructureFactory.createFileInRevision("/trunk/b", new TestRepoRevision(repo, 2L));

        g.addCopy(aRevOrig.getPath(), aRevOrig.getRevision(), aRevCopy.getPath(), aRevCopy.getRevision());

        final ProxyableFileHistoryNode aOrigNode = g.getNodeFor(aRevOrig);
        assertEquals(aRevOrig, aOrigNode.getFile());
        assertEquals(IFileHistoryNode.Type.UNCONFIRMED, aOrigNode.getType());
        assertEquals(false, aOrigNode.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, aOrigNode)), aOrigNode.getAncestors());

        final ProxyableFileHistoryNode aCopyNode = g.getNodeFor(aRevCopy);
        assertEquals(aRevCopy, aCopyNode.getFile());
        assertEquals(IFileHistoryNode.Type.CHANGED, aCopyNode.getType());
        assertEquals(true, aCopyNode.isCopyTarget());
        assertEquals(Collections.emptySet(), aCopyNode.getDescendants());

        assertNull(g.getNodeFor(
                ChangestructureFactory.createFileInRevision(aRevOrig.getPath(), aRevCopy.getRevision())));

        final FileHistoryEdge aEdge = new FileHistoryEdge(g, aOrigNode, aCopyNode, IFileHistoryEdge.Type.COPY);
        assertEquals(Collections.singleton(aEdge), aOrigNode.getDescendants());
        assertEquals(Collections.singleton(aEdge), aCopyNode.getAncestors());
    }

    @Test
    public void testCopyAndChangeOfFile() {
        final IRepository repo = new TestRepository("123");
        final FileHistoryGraph g = graph();

        final IRevisionedFile aRevOrig =
                ChangestructureFactory.createFileInRevision("/trunk/a", new TestRepoRevision(repo, 1L));
        final IRevisionedFile aRevCopy =
                ChangestructureFactory.createFileInRevision("/trunk/b", new TestRepoRevision(repo, 2L));

        g.addCopy(aRevOrig.getPath(), aRevOrig.getRevision(), aRevCopy.getPath(), aRevCopy.getRevision());
        g.addChange(aRevCopy.getPath(), aRevCopy.getRevision(), Collections.singleton(aRevOrig.getRevision()));

        final ProxyableFileHistoryNode aOrigNode = g.getNodeFor(aRevOrig);
        assertEquals(aRevOrig, aOrigNode.getFile());
        assertEquals(IFileHistoryNode.Type.UNCONFIRMED, aOrigNode.getType());
        assertEquals(false, aOrigNode.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, aOrigNode)), aOrigNode.getAncestors());

        final ProxyableFileHistoryNode aCopyNode = g.getNodeFor(aRevCopy);
        assertEquals(aRevCopy, aCopyNode.getFile());
        assertEquals(IFileHistoryNode.Type.CHANGED, aCopyNode.getType());
        assertEquals(true, aCopyNode.isCopyTarget());
        assertEquals(Collections.emptySet(), aCopyNode.getDescendants());

        assertNull(g.getNodeFor(
                ChangestructureFactory.createFileInRevision(aRevOrig.getPath(), aRevCopy.getRevision())));

        final FileHistoryEdge aEdge = new FileHistoryEdge(g, aOrigNode, aCopyNode, IFileHistoryEdge.Type.COPY);
        assertEquals(Collections.singleton(aEdge), aOrigNode.getDescendants());
        assertEquals(Collections.singleton(aEdge), aCopyNode.getAncestors());
    }

    @Test
    public void testCopyOfFileWithSourceFromIntermediateRevisions() {
        final IRepository repo = new TestRepository("123");
        final FileHistoryGraph g = graph();

        final IRevisionedFile aRevOrig =
                ChangestructureFactory.createFileInRevision("/trunk/a", new TestRepoRevision(repo, 1L));

        g.addAddition(aRevOrig.getPath(), aRevOrig.getRevision());

        final IRevisionedFile aRevChanged =
                ChangestructureFactory.createFileInRevision(aRevOrig.getPath(), new TestRepoRevision(repo, 2L));

        g.addChange(aRevChanged.getPath(), aRevChanged.getRevision(), Collections.singleton(aRevOrig.getRevision()));

        final IRevisionedFile aRevCopy =
                ChangestructureFactory.createFileInRevision("/trunk/b", new TestRepoRevision(repo, 4L));
        final IRevisionedFile aRevSource2 =
                ChangestructureFactory.createFileInRevision(aRevOrig.getPath(), new TestRepoRevision(repo, 3L));
        final IRevisionedFile aRevCopy2 =
                ChangestructureFactory.createFileInRevision("/trunk/c", new TestRepoRevision(repo, 5L));

        g.addCopy(aRevChanged.getPath(), aRevChanged.getRevision(), aRevCopy.getPath(), aRevCopy.getRevision());
        g.addCopy(aRevSource2.getPath(), aRevSource2.getRevision(), aRevCopy2.getPath(), aRevCopy2.getRevision());

        final ProxyableFileHistoryNode aOrigNode = g.getNodeFor(aRevOrig);
        assertEquals(aRevOrig, aOrigNode.getFile());
        assertEquals(IFileHistoryNode.Type.ADDED, aOrigNode.getType());
        assertEquals(false, aOrigNode.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, aOrigNode)), aOrigNode.getAncestors());

        final ProxyableFileHistoryNode aChangedNode = g.getNodeFor(aRevChanged);
        assertEquals(aRevChanged, aChangedNode.getFile());
        assertEquals(IFileHistoryNode.Type.CHANGED, aChangedNode.getType());
        assertEquals(false, aChangedNode.isCopyTarget());

        final ProxyableFileHistoryNode aSourceNode = g.getNodeFor(aRevChanged);
        assertEquals(aRevChanged, aSourceNode.getFile());
        assertEquals(IFileHistoryNode.Type.CHANGED, aSourceNode.getType());
        assertEquals(false, aSourceNode.isCopyTarget());

        final ProxyableFileHistoryNode aSource2Node = g.getNodeFor(aRevSource2);
        assertEquals(aRevSource2, aSource2Node.getFile());
        assertEquals(IFileHistoryNode.Type.CHANGED, aSource2Node.getType());
        assertEquals(false, aSource2Node.isCopyTarget());

        final ProxyableFileHistoryNode aCopyNode = g.getNodeFor(aRevCopy);
        assertEquals(aRevCopy, aCopyNode.getFile());
        assertEquals(IFileHistoryNode.Type.CHANGED, aCopyNode.getType());
        assertEquals(true, aCopyNode.isCopyTarget());
        assertEquals(Collections.emptySet(), aCopyNode.getDescendants());

        final ProxyableFileHistoryNode aCopy2Node = g.getNodeFor(aRevCopy2);
        assertEquals(aRevCopy2, aCopy2Node.getFile());
        assertEquals(IFileHistoryNode.Type.CHANGED, aCopy2Node.getType());
        assertEquals(true, aCopy2Node.isCopyTarget());
        assertEquals(Collections.emptySet(), aCopy2Node.getDescendants());

        final FileHistoryEdge aSourceEdge =
                new FileHistoryEdge(g, aOrigNode, aSourceNode, IFileHistoryEdge.Type.NORMAL);
        assertEquals(Collections.singleton(aSourceEdge), aOrigNode.getDescendants());
        assertEquals(Collections.singleton(aSourceEdge), aSourceNode.getAncestors());

        final FileHistoryEdge aCopy1Edge = new FileHistoryEdge(g, aSourceNode, aSource2Node,
                IFileHistoryEdge.Type.NORMAL);
        final FileHistoryEdge aCopy2Edge = new FileHistoryEdge(g, aSourceNode, aCopyNode, IFileHistoryEdge.Type.COPY);
        final FileHistoryEdge aCopy3Edge = new FileHistoryEdge(g, aSource2Node, aCopy2Node,
                IFileHistoryEdge.Type.COPY);
        assertEquals(new HashSet<>(Arrays.asList(aCopy1Edge, aCopy2Edge)), aSourceNode.getDescendants());
        assertEquals(Collections.singleton(aCopy3Edge), aSource2Node.getDescendants());
        assertEquals(Collections.singleton(aCopy2Edge), aCopyNode.getAncestors());
        assertEquals(Collections.singleton(aCopy3Edge), aCopy2Node.getAncestors());
    }

    @Test
    public void testMovementOfFile() {
        final IRepository repo = new TestRepository("123");
        final FileHistoryGraph g = graph();

        final IRevisionedFile aRevOrig =
                ChangestructureFactory.createFileInRevision("/trunk/a", new TestRepoRevision(repo, 1L));
        final IRevisionedFile aRevDel =
                ChangestructureFactory.createFileInRevision("/trunk/a", new TestRepoRevision(repo, 2L));
        final IRevisionedFile aRevCopy =
                ChangestructureFactory.createFileInRevision("/trunk/b", new TestRepoRevision(repo, 2L));

        g.addCopy(aRevOrig.getPath(), aRevOrig.getRevision(), aRevCopy.getPath(), aRevCopy.getRevision());
        g.addDeletion(aRevDel.getPath(), aRevDel.getRevision());

        final ProxyableFileHistoryNode aOrigNode = g.getNodeFor(aRevOrig);
        assertEquals(aRevOrig, aOrigNode.getFile());
        assertEquals(IFileHistoryNode.Type.UNCONFIRMED, aOrigNode.getType());
        assertEquals(false, aOrigNode.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, aOrigNode)), aOrigNode.getAncestors());

        final ProxyableFileHistoryNode aCopyNode = g.getNodeFor(aRevCopy);
        assertEquals(aRevCopy, aCopyNode.getFile());
        assertEquals(IFileHistoryNode.Type.CHANGED, aCopyNode.getType());
        assertEquals(true, aCopyNode.isCopyTarget());
        assertEquals(Collections.emptySet(), aCopyNode.getDescendants());

        final ProxyableFileHistoryNode aDelNode = g.getNodeFor(aRevDel);
        assertEquals(aRevDel, aDelNode.getFile());
        assertEquals(IFileHistoryNode.Type.DELETED, aDelNode.getType());
        assertEquals(false, aDelNode.isCopyTarget());
        assertEquals(Collections.emptySet(), aDelNode.getDescendants());

        final FileHistoryEdge aEdgeCopy = new FileHistoryEdge(g, aOrigNode, aCopyNode, IFileHistoryEdge.Type.COPY);
        final FileHistoryEdge aEdgeDel = new FileHistoryEdge(g, aOrigNode, aDelNode, IFileHistoryEdge.Type.NORMAL);
        assertEquals(new HashSet<>(Arrays.asList(aEdgeCopy, aEdgeDel)), aOrigNode.getDescendants());
        assertEquals(Collections.singleton(aEdgeCopy), aCopyNode.getAncestors());
        assertEquals(Collections.singleton(aEdgeDel), aDelNode.getAncestors());
    }

    @Test
    public void testMovementOfFile2() {
        final IRepository repo = new TestRepository("123");
        final FileHistoryGraph g = graph();

        final IRevisionedFile aRevOrig =
                ChangestructureFactory.createFileInRevision("/trunk/a", new TestRepoRevision(repo, 1L));
        final IRevisionedFile aRevDel =
                ChangestructureFactory.createFileInRevision("/trunk/a", new TestRepoRevision(repo, 2L));
        final IRevisionedFile aRevCopy =
                ChangestructureFactory.createFileInRevision("/trunk/b", new TestRepoRevision(repo, 2L));

        g.addDeletion(aRevDel.getPath(), aRevDel.getRevision());
        g.addCopy(aRevOrig.getPath(), aRevOrig.getRevision(), aRevCopy.getPath(), aRevCopy.getRevision());

        final ProxyableFileHistoryNode aOrigNode = g.getNodeFor(aRevOrig);
        assertEquals(aRevOrig, aOrigNode.getFile());
        assertEquals(IFileHistoryNode.Type.UNCONFIRMED, aOrigNode.getType());
        assertEquals(false, aOrigNode.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, aOrigNode)), aOrigNode.getAncestors());

        final ProxyableFileHistoryNode aCopyNode = g.getNodeFor(aRevCopy);
        assertEquals(aRevCopy, aCopyNode.getFile());
        assertEquals(IFileHistoryNode.Type.CHANGED, aCopyNode.getType());
        assertEquals(true, aCopyNode.isCopyTarget());
        assertEquals(Collections.emptySet(), aCopyNode.getDescendants());

        final ProxyableFileHistoryNode aDelNode = g.getNodeFor(aRevDel);
        assertEquals(aRevDel, aDelNode.getFile());
        assertEquals(IFileHistoryNode.Type.DELETED, aDelNode.getType());
        assertEquals(false, aDelNode.isCopyTarget());
        assertEquals(Collections.emptySet(), aDelNode.getDescendants());

        final FileHistoryEdge aEdgeCopy = new FileHistoryEdge(g, aOrigNode, aCopyNode, IFileHistoryEdge.Type.COPY);
        assertEquals(Collections.singleton(aEdgeCopy), aOrigNode.getDescendants());
        assertEquals(Collections.singleton(aEdgeCopy), aCopyNode.getAncestors());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, aDelNode)), aDelNode.getAncestors());
    }

    @Test
    public void testGetPaths() {
        final IRepository repo = new TestRepository("123");
        final FileHistoryGraph g = graph();

        final IRevisionedFile aRev =
                ChangestructureFactory.createFileInRevision("/trunk/a", new TestRepoRevision(repo, 2L));
        g.addAddition(aRev.getPath(), aRev.getRevision());

        final IRevisionedFile bRev =
                ChangestructureFactory.createFileInRevision("/trunk/x/b", new TestRepoRevision(repo, 3L));
        g.addAddition(bRev.getPath(), bRev.getRevision());

        final Set<String> paths = g.getPaths();
        assertTrue(paths.contains("/trunk/a"));
        assertTrue(paths.contains("/trunk/x/b"));
        assertFalse(paths.contains("/trunk/b"));
        assertFalse(paths.contains("/trunk/x/a"));
    }

    @Test
    public void testToString() {
        assertEquals("{}", graph().toString());
    }
}
