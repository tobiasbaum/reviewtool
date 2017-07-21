package de.setsoftware.reviewtool.model.changestructure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.junit.Test;

import de.setsoftware.reviewtool.model.api.IFileHistoryEdge;
import de.setsoftware.reviewtool.model.api.IFileHistoryNode;
import de.setsoftware.reviewtool.model.api.IRepository;
import de.setsoftware.reviewtool.model.api.IRevision;
import de.setsoftware.reviewtool.model.api.IRevisionedFile;

/**
 * Tests for {@link FileHistoryGraph}.
 */
public final class FileHistoryGraphTest {

    private static FileHistoryEdge createAlphaNode(
            final IRepository repo,
            final FileHistoryGraph g,
            final FileHistoryNode node) {
        final FileHistoryNode alphaNode = g.getNodeFor(
                ChangestructureFactory.createFileInRevision(node.getFile().getPath(),
                        ChangestructureFactory.createUnknownRevision(repo)));
        final FileHistoryEdge alphaEdge = new FileHistoryEdge(g, alphaNode, node, IFileHistoryEdge.Type.NORMAL,
                new FileDiff(alphaNode.getFile(), node.getFile()));
        return alphaEdge;
    }

    @Test
    public void testAdditionInKnownDirectory() {
        final IRepository repo = new TestRepository("123", new File("/some/repo"));
        final FileHistoryGraph g = new TestFileHistoryGraph();

        final IRevisionedFile trunkRev =
                ChangestructureFactory.createFileInRevision("/trunk", new TestRepoRevision(repo, 1L));

        g.addAdditionOrChange(trunkRev.getPath(), trunkRev.getRevision(), Collections.<IRevision> emptySet());

        final IRevisionedFile trunk2Rev =
                ChangestructureFactory.createFileInRevision("/trunk", new TestRepoRevision(repo, 2L));
        final IRevisionedFile aRev =
                ChangestructureFactory.createFileInRevision("/trunk/a", new TestRepoRevision(repo, 2L));

        g.addAdditionOrChange(aRev.getPath(), aRev.getRevision(), Collections.<IRevision> emptySet());

        final FileHistoryNode aNode = g.getNodeFor(aRev);
        assertEquals(aRev, aNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, aNode.getType());
        assertEquals(false, aNode.isCopyTarget());

        assertEquals(Collections.singleton(createAlphaNode(repo, g, aNode)), aNode.getAncestors());
        assertEquals(Collections.emptySet(), aNode.getDescendants());

        final FileHistoryNode trunkNode = g.getNodeFor(trunkRev);

        assertEquals(trunkRev, trunkNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, trunkNode.getType());
        assertEquals(false, trunkNode.isCopyTarget());

        assertEquals(Collections.singleton(createAlphaNode(repo, g, trunkNode)), trunkNode.getAncestors());
        assertEquals(Collections.emptySet(), trunkNode.getChildren());

        final FileHistoryNode trunk2Node = g.getNodeFor(trunk2Rev);

        assertEquals(trunk2Rev, trunk2Node.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, trunk2Node.getType());
        assertEquals(false, trunk2Node.isCopyTarget());
        assertEquals(Collections.emptySet(), trunk2Node.getDescendants());
        assertEquals(Collections.singleton(aNode), trunk2Node.getChildren());
        assertEquals(trunk2Node, aNode.getParent());

        final FileHistoryEdge trunkEdge = new FileHistoryEdge(g, trunkNode, trunk2Node, IFileHistoryEdge.Type.NORMAL,
                new FileDiff(trunkRev, trunk2Rev));
        assertEquals(Collections.singleton(trunkEdge), trunkNode.getDescendants());
        assertEquals(Collections.singleton(trunkEdge), trunk2Node.getAncestors());
    }

    @Test
    public void testAdditionInUnknownDirectory() {
        final IRepository repo = new TestRepository("123", new File("/some/repo"));
        final FileHistoryGraph g = new TestFileHistoryGraph();

        final IRevisionedFile aRev =
                ChangestructureFactory.createFileInRevision("/trunk/a", new TestRepoRevision(repo, 1L));

        g.addAdditionOrChange("/trunk/a", new TestRepoRevision(repo, 1L), Collections.<IRevision> emptySet());

        final IRevisionedFile trunkRev =
                ChangestructureFactory.createFileInRevision("/trunk", new TestRepoRevision(repo, 1L));

        final FileHistoryNode aNode = g.getNodeFor(aRev);

        assertEquals(aRev, aNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, aNode.getType());
        assertEquals(false, aNode.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, aNode)), aNode.getAncestors());
        assertEquals(Collections.emptySet(), aNode.getDescendants());

        final FileHistoryNode trunkNode = g.getNodeFor(trunkRev);

        assertEquals(trunkRev, trunkNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, trunkNode.getType());
        assertEquals(false, trunkNode.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, trunkNode)), trunkNode.getAncestors());
        assertEquals(Collections.emptySet(), trunkNode.getDescendants());

        assertEquals(Collections.singleton(aNode), trunkNode.getChildren());
        assertEquals(trunkNode, aNode.getParent());
    }

    @Test
    public void testAdditionInNewDirectory() {
        final IRepository repo = new TestRepository("123", new File("/some/repo"));
        final FileHistoryGraph g = new TestFileHistoryGraph();

        final IRevisionedFile trunkRev =
                ChangestructureFactory.createFileInRevision("/trunk", new TestRepoRevision(repo, 1L));
        final IRevisionedFile aRev =
                ChangestructureFactory.createFileInRevision("/trunk/a", new TestRepoRevision(repo, 1L));

        g.addAdditionOrChange(trunkRev.getPath(), trunkRev.getRevision(), Collections.<IRevision> emptySet());
        g.addAdditionOrChange(aRev.getPath(), aRev.getRevision(), Collections.<IRevision> emptySet());

        final FileHistoryNode aNode = g.getNodeFor(aRev);

        assertEquals(aRev, aNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, aNode.getType());
        assertEquals(false, aNode.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, aNode)), aNode.getAncestors());
        assertEquals(Collections.emptySet(), aNode.getDescendants());

        final FileHistoryNode trunkNode = g.getNodeFor(trunkRev);

        assertEquals(trunkRev, trunkNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, trunkNode.getType());
        assertEquals(false, trunkNode.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, trunkNode)), trunkNode.getAncestors());
        assertEquals(Collections.emptySet(), trunkNode.getDescendants());

        assertEquals(Collections.singleton(aNode), trunkNode.getChildren());
        assertEquals(trunkNode, aNode.getParent());
    }

    @Test
    public void testAdditionInSubsequentRevisions() {
        final IRepository repo = new TestRepository("123", new File("/some/repo"));
        final FileHistoryGraph g = new TestFileHistoryGraph();

        final IRevisionedFile a1Rev =
                ChangestructureFactory.createFileInRevision("/trunk/a1", new TestRepoRevision(repo, 1L));
        final IRevisionedFile a2Rev =
                ChangestructureFactory.createFileInRevision("/trunk/a2", new TestRepoRevision(repo, 2L));

        g.addAdditionOrChange(a1Rev.getPath(), a1Rev.getRevision(), Collections.<IRevision> emptySet());
        g.addAdditionOrChange(a2Rev.getPath(), a2Rev.getRevision(), Collections.<IRevision> emptySet());

        final FileHistoryNode a1Node = g.getNodeFor(a1Rev);
        assertEquals(a1Rev, a1Node.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, a1Node.getType());
        assertEquals(false, a1Node.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, a1Node)), a1Node.getAncestors());
        assertEquals(Collections.emptySet(), a1Node.getDescendants());

        final IRevisionedFile trunkRev = ChangestructureFactory.createFileInRevision("/trunk", a1Rev.getRevision());
        final FileHistoryNode trunkNode = g.getNodeFor(trunkRev);
        assertEquals(trunkRev, trunkNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, trunkNode.getType());
        assertEquals(false, trunkNode.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, trunkNode)), trunkNode.getAncestors());
        assertEquals(Collections.singleton(a1Node), trunkNode.getChildren());
        assertEquals(trunkNode, a1Node.getParent());

        final FileHistoryNode a2Node = g.getNodeFor(a2Rev);
        assertEquals(a2Rev, a2Node.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, a2Node.getType());
        assertEquals(false, a2Node.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, a2Node)), a2Node.getAncestors());
        assertEquals(Collections.emptySet(), a2Node.getDescendants());

        final IRevisionedFile trunkRev2 = ChangestructureFactory.createFileInRevision("/trunk", a2Rev.getRevision());
        final FileHistoryNode trunkNode2 = g.getNodeFor(trunkRev2);
        assertEquals(trunkRev2, trunkNode2.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, trunkNode2.getType());
        assertEquals(false, trunkNode2.isCopyTarget());
        assertEquals(Collections.emptySet(), trunkNode2.getDescendants());
        assertEquals(Collections.singleton(a2Node), trunkNode2.getChildren());
        assertEquals(trunkNode2, a2Node.getParent());

        final FileHistoryEdge trunkEdge = new FileHistoryEdge(g, trunkNode, trunkNode2, IFileHistoryEdge.Type.NORMAL,
                new FileDiff(trunkRev, trunkRev2));
        assertEquals(Collections.singleton(trunkEdge), trunkNode.getDescendants());
        assertEquals(Collections.singleton(trunkEdge), trunkNode2.getAncestors());
    }

    @Test
    public void testDeletionOfKnownNode() {
        final IRepository repo = new TestRepository("123", new File("/some/repo"));
        final FileHistoryGraph g = new TestFileHistoryGraph();

        final IRevisionedFile trunkRev =
                ChangestructureFactory.createFileInRevision("/trunk", new TestRepoRevision(repo, 1L));
        final IRevisionedFile aRevPrev =
                ChangestructureFactory.createFileInRevision("/trunk/a", new TestRepoRevision(repo, 1L));

        g.addAdditionOrChange(trunkRev.getPath(), trunkRev.getRevision(), Collections.<IRevision> emptySet());
        g.addAdditionOrChange(aRevPrev.getPath(), aRevPrev.getRevision(), Collections.<IRevision> emptySet());

        final IRevisionedFile trunk2Rev =
                ChangestructureFactory.createFileInRevision("/trunk", new TestRepoRevision(repo, 2L));
        final IRevisionedFile aRevDel =
                ChangestructureFactory.createFileInRevision("/trunk/a", new TestRepoRevision(repo, 2L));

        g.addDeletion(aRevDel.getPath(), aRevDel.getRevision());

        final FileHistoryNode aNode = g.getNodeFor(aRevPrev);
        assertEquals(aRevPrev, aNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, aNode.getType());
        assertEquals(false, aNode.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, aNode)), aNode.getAncestors());

        final FileHistoryNode aNodeDel = g.getNodeFor(aRevDel);
        assertEquals(aRevDel, aNodeDel.getFile());
        assertEquals(IFileHistoryNode.Type.DELETED, aNodeDel.getType());
        assertEquals(false, aNodeDel.isCopyTarget());
        assertEquals(Collections.emptySet(), aNodeDel.getDescendants());

        final FileHistoryEdge aEdge = new FileHistoryEdge(g, aNode, aNodeDel, IFileHistoryEdge.Type.NORMAL,
                new FileDiff(aRevPrev, aRevDel));
        assertEquals(Collections.singleton(aEdge), aNode.getDescendants());
        assertEquals(Collections.singleton(aEdge), aNodeDel.getAncestors());

        final FileHistoryNode trunkNode = g.getNodeFor(trunkRev);
        assertEquals(trunkRev, trunkNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, trunkNode.getType());
        assertEquals(false, trunkNode.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, trunkNode)), trunkNode.getAncestors());
        assertEquals(Collections.singleton(aNode), trunkNode.getChildren());
        assertEquals(trunkNode, aNode.getParent());

        final FileHistoryNode trunk2Node = g.getNodeFor(trunk2Rev);
        assertEquals(trunk2Rev, trunk2Node.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, trunk2Node.getType());
        assertEquals(false, trunk2Node.isCopyTarget());
        assertEquals(Collections.emptySet(), trunk2Node.getDescendants());
        assertEquals(Collections.singleton(aNodeDel), trunk2Node.getChildren());
        assertEquals(trunk2Node, aNodeDel.getParent());

        final FileHistoryEdge trunkEdge = new FileHistoryEdge(g, trunkNode, trunk2Node, IFileHistoryEdge.Type.NORMAL,
                new FileDiff(trunkRev, trunk2Rev));
        assertEquals(Collections.singleton(trunkEdge), trunkNode.getDescendants());
        assertEquals(Collections.singleton(trunkEdge), trunk2Node.getAncestors());
    }

    @Test
    public void testDeletionOfUnknownNode() {
        final IRepository repo = new TestRepository("123", new File("/some/repo"));
        final FileHistoryGraph g = new TestFileHistoryGraph();

        final IRevisionedFile aRevPrev =
                ChangestructureFactory.createFileInRevision("/trunk/a",
                        ChangestructureFactory.createUnknownRevision(repo));
        final IRevisionedFile aRevDel =
                ChangestructureFactory.createFileInRevision("/trunk/a", new TestRepoRevision(repo, 2L));

        g.addDeletion(aRevDel.getPath(), aRevDel.getRevision());

        final FileHistoryNode aNodeDel = g.getNodeFor(aRevDel);
        assertEquals(aRevDel, aNodeDel.getFile());
        assertEquals(IFileHistoryNode.Type.DELETED, aNodeDel.getType());
        assertEquals(false, aNodeDel.isCopyTarget());
        assertEquals(Collections.emptySet(), aNodeDel.getDescendants());

        final FileHistoryNode aNodePrev = g.getNodeFor(aRevPrev);
        assertEquals(aRevPrev, aNodePrev.getFile());
        assertEquals(IFileHistoryNode.Type.UNCONFIRMED, aNodePrev.getType());
        assertEquals(false, aNodePrev.isCopyTarget());
        assertEquals(Collections.emptySet(), aNodePrev.getAncestors());

        final FileHistoryEdge aEdge = new FileHistoryEdge(g, aNodePrev, aNodeDel, IFileHistoryEdge.Type.NORMAL,
                new FileDiff(aRevPrev, aRevDel));
        assertEquals(Collections.singleton(aEdge), aNodePrev.getDescendants());
        assertEquals(Collections.singleton(aEdge), aNodeDel.getAncestors());
    }

    @Test
    public void testDeletionOfKnownDirectory() {
        final IRepository repo = new TestRepository("123", new File("/some/repo"));
        final FileHistoryGraph g = new TestFileHistoryGraph();

        final IRevisionedFile trunkRev =
                ChangestructureFactory.createFileInRevision("/trunk", new TestRepoRevision(repo, 1L));
        final IRevisionedFile aRevPrev =
                ChangestructureFactory.createFileInRevision("/trunk/a", trunkRev.getRevision());
        final IRevisionedFile xRevPrev =
                ChangestructureFactory.createFileInRevision("/trunk/a/x", trunkRev.getRevision());

        g.addAdditionOrChange(trunkRev.getPath(), trunkRev.getRevision(), Collections.<IRevision> emptySet());
        g.addAdditionOrChange(aRevPrev.getPath(), aRevPrev.getRevision(), Collections.<IRevision> emptySet());
        g.addAdditionOrChange(xRevPrev.getPath(), xRevPrev.getRevision(), Collections.<IRevision> emptySet());

        final IRevisionedFile trunk2Rev =
                ChangestructureFactory.createFileInRevision(trunkRev.getPath(), new TestRepoRevision(repo, 2L));
        final IRevisionedFile aRevDel =
                ChangestructureFactory.createFileInRevision(aRevPrev.getPath(), trunk2Rev.getRevision());
        final IRevisionedFile xRevDel =
                ChangestructureFactory.createFileInRevision(xRevPrev.getPath(), trunk2Rev.getRevision());

        g.addDeletion(aRevDel.getPath(), aRevDel.getRevision());

        final FileHistoryNode aNode = g.getNodeFor(aRevPrev);
        assertEquals(aRevPrev, aNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, aNode.getType());
        assertEquals(false, aNode.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, aNode)), aNode.getAncestors());

        final FileHistoryNode xNode = g.getNodeFor(xRevPrev);
        assertEquals(xRevPrev, xNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, xNode.getType());
        assertEquals(false, xNode.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, xNode)), xNode.getAncestors());

        assertEquals(Collections.singleton(xNode), aNode.getChildren());
        assertEquals(aNode, xNode.getParent());

        final FileHistoryNode aNodeDel = g.getNodeFor(aRevDel);
        assertEquals(aRevDel, aNodeDel.getFile());
        assertEquals(IFileHistoryNode.Type.DELETED, aNodeDel.getType());
        assertEquals(false, aNodeDel.isCopyTarget());
        assertEquals(Collections.emptySet(), aNodeDel.getDescendants());

        final FileHistoryNode xNodeDel = g.getNodeFor(xRevDel);
        assertEquals(xRevDel, xNodeDel.getFile());
        assertEquals(IFileHistoryNode.Type.DELETED, xNodeDel.getType());
        assertEquals(false, xNodeDel.isCopyTarget());
        assertEquals(Collections.emptySet(), xNodeDel.getDescendants());

        assertEquals(Collections.singleton(xNodeDel), aNodeDel.getChildren());
        assertEquals(aNodeDel, xNodeDel.getParent());

        final FileHistoryEdge aEdge = new FileHistoryEdge(g, aNode, aNodeDel, IFileHistoryEdge.Type.NORMAL,
                new FileDiff(aRevPrev, aRevDel));
        assertEquals(Collections.singleton(aEdge), aNode.getDescendants());
        assertEquals(Collections.singleton(aEdge), aNodeDel.getAncestors());

        final FileHistoryEdge xEdge = new FileHistoryEdge(g, xNode, xNodeDel, IFileHistoryEdge.Type.NORMAL,
                new FileDiff(xRevPrev, xRevDel));
        assertEquals(Collections.singleton(xEdge), xNode.getDescendants());
        assertEquals(Collections.singleton(xEdge), xNodeDel.getAncestors());

        final FileHistoryNode trunkNode = g.getNodeFor(trunkRev);
        assertEquals(trunkRev, trunkNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, trunkNode.getType());
        assertEquals(false, trunkNode.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, trunkNode)), trunkNode.getAncestors());
        assertEquals(Collections.singleton(aNode), trunkNode.getChildren());
        assertEquals(trunkNode, aNode.getParent());

        final FileHistoryNode trunk2Node = g.getNodeFor(trunk2Rev);
        assertEquals(trunk2Rev, trunk2Node.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, trunk2Node.getType());
        assertEquals(false, trunk2Node.isCopyTarget());
        assertEquals(Collections.emptySet(), trunk2Node.getDescendants());
        assertEquals(Collections.singleton(aNodeDel), trunk2Node.getChildren());
        assertEquals(trunk2Node, aNodeDel.getParent());

        final FileHistoryEdge trunkEdge = new FileHistoryEdge(g, trunkNode, trunk2Node, IFileHistoryEdge.Type.NORMAL,
                new FileDiff(trunkRev, trunk2Rev));
        assertEquals(Collections.singleton(trunkEdge), trunkNode.getDescendants());
        assertEquals(Collections.singleton(trunkEdge), trunk2Node.getAncestors());
    }

    @Test
    public void testDeletionOfKnownDirectoryWithKnownDeletedNode() {
        final IRepository repo = new TestRepository("123", new File("/some/repo"));
        final FileHistoryGraph g = new TestFileHistoryGraph();

        final IRevisionedFile trunkRev =
                ChangestructureFactory.createFileInRevision("/trunk", new TestRepoRevision(repo, 1L));
        final IRevisionedFile aRevOrig =
                ChangestructureFactory.createFileInRevision("/trunk/a", trunkRev.getRevision());
        final IRevisionedFile xRevOrig =
                ChangestructureFactory.createFileInRevision("/trunk/a/x", trunkRev.getRevision());

        g.addAdditionOrChange(trunkRev.getPath(), trunkRev.getRevision(), Collections.<IRevision> emptySet());
        g.addAdditionOrChange(aRevOrig.getPath(), aRevOrig.getRevision(), Collections.<IRevision> emptySet());
        g.addAdditionOrChange(xRevOrig.getPath(), xRevOrig.getRevision(), Collections.<IRevision> emptySet());

        final IRevisionedFile trunk2Rev =
                ChangestructureFactory.createFileInRevision(trunkRev.getPath(), new TestRepoRevision(repo, 2L));
        final IRevisionedFile aRevPrev =
                ChangestructureFactory.createFileInRevision(aRevOrig.getPath(), trunk2Rev.getRevision());
        final IRevisionedFile xRevDel =
                ChangestructureFactory.createFileInRevision(xRevOrig.getPath(), trunk2Rev.getRevision());

        g.addDeletion(xRevDel.getPath(), xRevDel.getRevision());

        final IRevisionedFile trunk3Rev =
                ChangestructureFactory.createFileInRevision(trunkRev.getPath(), new TestRepoRevision(repo, 3L));
        final IRevisionedFile aRevDel =
                ChangestructureFactory.createFileInRevision(aRevPrev.getPath(), trunk3Rev.getRevision());

        g.addDeletion(aRevDel.getPath(), aRevDel.getRevision());

        final FileHistoryNode aNode = g.getNodeFor(aRevOrig);
        assertEquals(aRevOrig, aNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, aNode.getType());
        assertEquals(false, aNode.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, aNode)), aNode.getAncestors());

        final FileHistoryNode xNode = g.getNodeFor(xRevOrig);
        assertEquals(xRevOrig, xNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, xNode.getType());
        assertEquals(false, xNode.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, xNode)), xNode.getAncestors());

        assertEquals(Collections.singleton(xNode), aNode.getChildren());
        assertEquals(aNode, xNode.getParent());

        final FileHistoryNode aNodePrev = g.getNodeFor(aRevPrev);
        assertEquals(aRevPrev, aNodePrev.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, aNodePrev.getType());
        assertEquals(false, aNodePrev.isCopyTarget());

        final FileHistoryNode xNodeDel = g.getNodeFor(xRevDel);
        assertEquals(xRevDel, xNodeDel.getFile());
        assertEquals(IFileHistoryNode.Type.DELETED, xNodeDel.getType());
        assertEquals(false, xNodeDel.isCopyTarget());
        assertEquals(Collections.emptySet(), xNodeDel.getDescendants());

        assertEquals(Collections.singleton(xNodeDel), aNodePrev.getChildren());
        assertEquals(aNodePrev, xNodeDel.getParent());

        final FileHistoryEdge aEdgePrev = new FileHistoryEdge(g, aNode, aNodePrev, IFileHistoryEdge.Type.NORMAL,
                new FileDiff(aRevOrig, aRevPrev));
        assertEquals(Collections.singleton(aEdgePrev), aNode.getDescendants());
        assertEquals(Collections.singleton(aEdgePrev), aNodePrev.getAncestors());

        final FileHistoryEdge xEdge = new FileHistoryEdge(g, xNode, xNodeDel, IFileHistoryEdge.Type.NORMAL,
                new FileDiff(xRevOrig, xRevDel));
        assertEquals(Collections.singleton(xEdge), xNode.getDescendants());
        assertEquals(Collections.singleton(xEdge), xNodeDel.getAncestors());

        final FileHistoryNode aNodeDel = g.getNodeFor(aRevDel);
        assertEquals(aRevDel, aNodeDel.getFile());
        assertEquals(IFileHistoryNode.Type.DELETED, aNodeDel.getType());
        assertEquals(false, aNodeDel.isCopyTarget());
        assertEquals(Collections.emptySet(), aNodeDel.getDescendants());

        final FileHistoryEdge aEdgeDel = new FileHistoryEdge(g, aNodePrev, aNodeDel, IFileHistoryEdge.Type.NORMAL,
                new FileDiff(aRevPrev, aRevDel));
        assertEquals(Collections.singleton(aEdgeDel), aNodePrev.getDescendants());
        assertEquals(Collections.singleton(aEdgeDel), aNodeDel.getAncestors());

        final FileHistoryNode trunkNode = g.getNodeFor(trunkRev);
        assertEquals(trunkRev, trunkNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, trunkNode.getType());
        assertEquals(false, trunkNode.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, trunkNode)), trunkNode.getAncestors());
        assertEquals(Collections.singleton(aNode), trunkNode.getChildren());
        assertEquals(trunkNode, aNode.getParent());

        final FileHistoryNode trunk2Node = g.getNodeFor(trunk2Rev);
        assertEquals(trunk2Rev, trunk2Node.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, trunk2Node.getType());
        assertEquals(false, trunk2Node.isCopyTarget());
        assertEquals(Collections.singleton(aNodePrev), trunk2Node.getChildren());
        assertEquals(trunk2Node, aNodePrev.getParent());

        final FileHistoryNode trunk3Node = g.getNodeFor(trunk3Rev);
        assertEquals(trunk3Rev, trunk3Node.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, trunk3Node.getType());
        assertEquals(false, trunk3Node.isCopyTarget());
        assertEquals(Collections.emptySet(), trunk3Node.getDescendants());
        assertEquals(Collections.singleton(aNodeDel), trunk3Node.getChildren());
        assertEquals(trunk3Node, aNodeDel.getParent());

        final FileHistoryEdge trunkEdge = new FileHistoryEdge(g, trunkNode, trunk2Node, IFileHistoryEdge.Type.NORMAL,
                new FileDiff(trunkRev, trunk2Rev));
        assertEquals(Collections.singleton(trunkEdge), trunkNode.getDescendants());
        assertEquals(Collections.singleton(trunkEdge), trunk2Node.getAncestors());

        final FileHistoryEdge trunk2Edge = new FileHistoryEdge(g, trunk2Node, trunk3Node, IFileHistoryEdge.Type.NORMAL,
                new FileDiff(trunk2Rev, trunk3Rev));
        assertEquals(Collections.singleton(trunk2Edge), trunk2Node.getDescendants());
        assertEquals(Collections.singleton(trunk2Edge), trunk3Node.getAncestors());
    }

    @Test
    public void testDeletionOfKnownDirectoryWithKnownDeletedNode2() {
        final IRepository repo = new TestRepository("123", new File("/some/repo"));
        final FileHistoryGraph g = new TestFileHistoryGraph();

        final IRevisionedFile trunkRev =
                ChangestructureFactory.createFileInRevision("/trunk", new TestRepoRevision(repo, 1L));
        final IRevisionedFile aRevOrig =
                ChangestructureFactory.createFileInRevision("/trunk/a", trunkRev.getRevision());
        final IRevisionedFile xRevOrig =
                ChangestructureFactory.createFileInRevision("/trunk/a/x", trunkRev.getRevision());
        final IRevisionedFile yRevOrig =
                ChangestructureFactory.createFileInRevision("/trunk/a/y", trunkRev.getRevision());

        g.addAdditionOrChange(trunkRev.getPath(), trunkRev.getRevision(), Collections.<IRevision> emptySet());
        g.addAdditionOrChange(aRevOrig.getPath(), aRevOrig.getRevision(), Collections.<IRevision> emptySet());
        g.addAdditionOrChange(xRevOrig.getPath(), xRevOrig.getRevision(), Collections.<IRevision> emptySet());
        g.addAdditionOrChange(yRevOrig.getPath(), yRevOrig.getRevision(), Collections.<IRevision> emptySet());

        final IRevisionedFile trunk2Rev =
                ChangestructureFactory.createFileInRevision(trunkRev.getPath(), new TestRepoRevision(repo, 2L));
        final IRevisionedFile aRev2 =
                ChangestructureFactory.createFileInRevision(aRevOrig.getPath(), trunk2Rev.getRevision());
        final IRevisionedFile xRevDel =
                ChangestructureFactory.createFileInRevision(xRevOrig.getPath(), trunk2Rev.getRevision());
        final IRevisionedFile yRev2 =
                ChangestructureFactory.createFileInRevision(yRevOrig.getPath(), trunk2Rev.getRevision());

        g.addDeletion(xRevDel.getPath(), xRevDel.getRevision());

        final IRevisionedFile trunk3Rev =
                ChangestructureFactory.createFileInRevision(trunkRev.getPath(), new TestRepoRevision(repo, 3L));
        final IRevisionedFile aRev3 =
                ChangestructureFactory.createFileInRevision(aRevOrig.getPath(), trunk3Rev.getRevision());
        final IRevisionedFile yRevChange =
                ChangestructureFactory.createFileInRevision(yRevOrig.getPath(), trunk3Rev.getRevision());

        g.addAdditionOrChange(yRevChange.getPath(), yRevChange.getRevision(),
                Collections.singleton(yRevOrig.getRevision()));

        final IRevisionedFile trunk4Rev =
                ChangestructureFactory.createFileInRevision(trunkRev.getPath(), new TestRepoRevision(repo, 4L));
        final IRevisionedFile aRevDel =
                ChangestructureFactory.createFileInRevision(aRevOrig.getPath(), trunk4Rev.getRevision());
        final IRevisionedFile yRevDel =
                ChangestructureFactory.createFileInRevision(yRevOrig.getPath(), trunk4Rev.getRevision());

        g.addDeletion(aRevDel.getPath(), aRevDel.getRevision());

        // revision 1
        final FileHistoryNode aNode = g.getNodeFor(aRevOrig);
        assertEquals(aRevOrig, aNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, aNode.getType());
        assertEquals(false, aNode.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, aNode)), aNode.getAncestors());

        final FileHistoryNode xNode = g.getNodeFor(xRevOrig);
        assertEquals(xRevOrig, xNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, xNode.getType());
        assertEquals(false, xNode.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, xNode)), xNode.getAncestors());

        final FileHistoryNode yNode = g.getNodeFor(yRevOrig);
        assertEquals(yRevOrig, yNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, yNode.getType());
        assertEquals(false, yNode.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, yNode)), yNode.getAncestors());

        assertEquals(new HashSet<>(Arrays.asList(xNode, yNode)), aNode.getChildren());
        assertEquals(aNode, xNode.getParent());
        assertEquals(aNode, yNode.getParent());

        // revision 2
        final FileHistoryNode aNode2 = g.getNodeFor(aRev2);
        assertEquals(aRev2, aNode2.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, aNode2.getType());
        assertEquals(false, aNode2.isCopyTarget());

        final FileHistoryNode xNodeDel = g.getNodeFor(xRevDel);
        assertEquals(xRevDel, xNodeDel.getFile());
        assertEquals(IFileHistoryNode.Type.DELETED, xNodeDel.getType());
        assertEquals(false, xNodeDel.isCopyTarget());
        assertEquals(Collections.emptySet(), xNodeDel.getDescendants());

        final FileHistoryNode yNode2 = g.getNodeFor(yRev2);
        assertEquals(yRev2, yNode2.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, yNode2.getType());
        assertEquals(false, yNode2.isCopyTarget());

        assertEquals(new HashSet<>(Arrays.asList(xNodeDel, yNode2)), aNode2.getChildren());
        assertEquals(aNode2, xNodeDel.getParent());
        assertEquals(aNode2, yNode2.getParent());

        final FileHistoryEdge aEdge1 = new FileHistoryEdge(g, aNode, aNode2, IFileHistoryEdge.Type.NORMAL,
                new FileDiff(aRevOrig, aRev2));
        assertEquals(Collections.singleton(aEdge1), aNode.getDescendants());
        assertEquals(Collections.singleton(aEdge1), aNode2.getAncestors());

        final FileHistoryEdge xEdge = new FileHistoryEdge(g, xNode, xNodeDel, IFileHistoryEdge.Type.NORMAL,
                new FileDiff(xRevOrig, xRevDel));
        assertEquals(Collections.singleton(xEdge), xNode.getDescendants());
        assertEquals(Collections.singleton(xEdge), xNodeDel.getAncestors());

        final FileHistoryEdge yEdge = new FileHistoryEdge(g, yNode, yNode2, IFileHistoryEdge.Type.NORMAL,
                new FileDiff(yRevOrig, yRev2));
        assertEquals(Collections.singleton(yEdge), yNode.getDescendants());
        assertEquals(Collections.singleton(yEdge), yNode2.getAncestors());

        // revision 3
        final FileHistoryNode aNode3 = g.getNodeFor(aRev3);
        assertEquals(aRev3, aNode3.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, aNode3.getType());
        assertEquals(false, aNode3.isCopyTarget());

        final FileHistoryNode yNodeChange = g.getNodeFor(yRevChange);
        assertEquals(yRevChange, yNodeChange.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, yNodeChange.getType());
        assertEquals(false, yNodeChange.isCopyTarget());

        assertEquals(Collections.singleton(yNodeChange), aNode3.getChildren());
        assertEquals(aNode3, yNodeChange.getParent());

        final FileHistoryEdge aEdge2 = new FileHistoryEdge(g, aNode2, aNode3, IFileHistoryEdge.Type.NORMAL,
                new FileDiff(aRev2, aRev3));
        assertEquals(Collections.singleton(aEdge2), aNode2.getDescendants());
        assertEquals(Collections.singleton(aEdge2), aNode3.getAncestors());

        final FileHistoryEdge xEdgeDel = new FileHistoryEdge(g, xNode, xNodeDel, IFileHistoryEdge.Type.NORMAL,
                new FileDiff(xRevOrig, xRevDel));
        assertEquals(Collections.singleton(xEdgeDel), xNode.getDescendants());
        assertEquals(Collections.singleton(xEdgeDel), xNodeDel.getAncestors());

        final FileHistoryEdge yEdgeChange = new FileHistoryEdge(g, yNode2, yNodeChange, IFileHistoryEdge.Type.NORMAL,
                new FileDiff(yRev2, yRevChange));
        assertEquals(Collections.singleton(yEdgeChange), yNode2.getDescendants());
        assertEquals(Collections.singleton(yEdgeChange), yNodeChange.getAncestors());

        // revision 4
        final FileHistoryNode aNodeDel = g.getNodeFor(aRevDel);
        assertEquals(aRevDel, aNodeDel.getFile());
        assertEquals(IFileHistoryNode.Type.DELETED, aNodeDel.getType());
        assertEquals(false, aNodeDel.isCopyTarget());
        assertEquals(Collections.emptySet(), aNodeDel.getDescendants());

        final FileHistoryNode yNodeDel = g.getNodeFor(yRevDel);
        assertEquals(yRevDel, yNodeDel.getFile());
        assertEquals(IFileHistoryNode.Type.DELETED, yNodeDel.getType());
        assertEquals(false, yNodeDel.isCopyTarget());

        assertEquals(Collections.singleton(yNodeDel), aNodeDel.getChildren());
        assertEquals(aNodeDel, yNodeDel.getParent());

        final FileHistoryEdge aEdgeDel = new FileHistoryEdge(g, aNode3, aNodeDel, IFileHistoryEdge.Type.NORMAL,
                new FileDiff(aRev3, aRevDel));
        assertEquals(Collections.singleton(aEdgeDel), aNode3.getDescendants());
        assertEquals(Collections.singleton(aEdgeDel), aNodeDel.getAncestors());

        final FileHistoryNode trunkNode = g.getNodeFor(trunkRev);
        assertEquals(trunkRev, trunkNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, trunkNode.getType());
        assertEquals(false, trunkNode.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, trunkNode)), trunkNode.getAncestors());
        assertEquals(Collections.singleton(aNode), trunkNode.getChildren());
        assertEquals(trunkNode, aNode.getParent());

        final FileHistoryNode trunk2Node = g.getNodeFor(trunk2Rev);
        assertEquals(trunk2Rev, trunk2Node.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, trunk2Node.getType());
        assertEquals(false, trunk2Node.isCopyTarget());
        assertEquals(Collections.singleton(aNode2), trunk2Node.getChildren());
        assertEquals(trunk2Node, aNode2.getParent());

        final FileHistoryNode trunk3Node = g.getNodeFor(trunk3Rev);
        assertEquals(trunk3Rev, trunk3Node.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, trunk3Node.getType());
        assertEquals(false, trunk3Node.isCopyTarget());
        assertEquals(Collections.singleton(aNode3), trunk3Node.getChildren());
        assertEquals(trunk3Node, aNode3.getParent());

        final FileHistoryNode trunk4Node = g.getNodeFor(trunk4Rev);
        assertEquals(trunk4Rev, trunk4Node.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, trunk4Node.getType());
        assertEquals(false, trunk4Node.isCopyTarget());
        assertEquals(Collections.emptySet(), trunk4Node.getDescendants());
        assertEquals(Collections.singleton(aNodeDel), trunk4Node.getChildren());
        assertEquals(trunk4Node, aNodeDel.getParent());

        final FileHistoryEdge trunkEdge = new FileHistoryEdge(g, trunkNode, trunk2Node, IFileHistoryEdge.Type.NORMAL,
                new FileDiff(trunkRev, trunk2Rev));
        assertEquals(Collections.singleton(trunkEdge), trunkNode.getDescendants());
        assertEquals(Collections.singleton(trunkEdge), trunk2Node.getAncestors());

        final FileHistoryEdge trunk2Edge = new FileHistoryEdge(g, trunk2Node, trunk3Node, IFileHistoryEdge.Type.NORMAL,
                new FileDiff(trunk2Rev, trunk3Rev));
        assertEquals(Collections.singleton(trunk2Edge), trunk2Node.getDescendants());
        assertEquals(Collections.singleton(trunk2Edge), trunk3Node.getAncestors());

        final FileHistoryEdge trunk3Edge = new FileHistoryEdge(g, trunk3Node, trunk4Node, IFileHistoryEdge.Type.NORMAL,
                new FileDiff(trunk3Rev, trunk4Rev));
        assertEquals(Collections.singleton(trunk3Edge), trunk3Node.getDescendants());
        assertEquals(Collections.singleton(trunk3Edge), trunk4Node.getAncestors());
    }

    @Test
    public void testReplacementOfKnownFile() {
        final IRepository repo = new TestRepository("123", new File("/some/repo"));
        final FileHistoryGraph g = new TestFileHistoryGraph();

        final IRevisionedFile trunkRev =
                ChangestructureFactory.createFileInRevision("/trunk", new TestRepoRevision(repo, 1L));
        final IRevisionedFile aRevPrev =
                ChangestructureFactory.createFileInRevision("/trunk/a", new TestRepoRevision(repo, 1L));

        g.addAdditionOrChange(trunkRev.getPath(), trunkRev.getRevision(), Collections.<IRevision> emptySet());
        g.addAdditionOrChange(aRevPrev.getPath(), aRevPrev.getRevision(), Collections.<IRevision> emptySet());

        final IRevisionedFile trunk2Rev =
                ChangestructureFactory.createFileInRevision("/trunk", new TestRepoRevision(repo, 2L));
        final IRevisionedFile aRevReplaced =
                ChangestructureFactory.createFileInRevision("/trunk/a", new TestRepoRevision(repo, 2L));

        g.addReplacement(aRevReplaced.getPath(), aRevReplaced.getRevision());

        final FileHistoryNode aNode = g.getNodeFor(aRevPrev);
        assertEquals(aRevPrev, aNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, aNode.getType());
        assertEquals(false, aNode.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, aNode)), aNode.getAncestors());

        final FileHistoryNode aNodeReplaced = g.getNodeFor(aRevReplaced);
        assertEquals(aRevReplaced, aNodeReplaced.getFile());
        assertEquals(IFileHistoryNode.Type.REPLACED, aNodeReplaced.getType());
        assertEquals(false, aNodeReplaced.isCopyTarget());
        assertEquals(Collections.emptySet(), aNodeReplaced.getDescendants());

        final FileHistoryEdge aEdge = new FileHistoryEdge(g, aNode, aNodeReplaced, IFileHistoryEdge.Type.NORMAL,
                new FileDiff(aRevPrev, aRevReplaced));
        assertEquals(Collections.singleton(aEdge), aNode.getDescendants());
        assertEquals(Collections.singleton(aEdge), aNodeReplaced.getAncestors());

        final FileHistoryNode trunkNode = g.getNodeFor(trunkRev);
        assertEquals(trunkRev, trunkNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, trunkNode.getType());
        assertEquals(false, trunkNode.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, trunkNode)), trunkNode.getAncestors());
        assertEquals(Collections.singleton(aNode), trunkNode.getChildren());
        assertEquals(trunkNode, aNode.getParent());

        final FileHistoryNode trunk2Node = g.getNodeFor(trunk2Rev);
        assertEquals(trunk2Rev, trunk2Node.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, trunk2Node.getType());
        assertEquals(false, trunk2Node.isCopyTarget());
        assertEquals(Collections.emptySet(), trunk2Node.getDescendants());
        assertEquals(Collections.singleton(aNodeReplaced), trunk2Node.getChildren());
        assertEquals(trunk2Node, aNodeReplaced.getParent());

        final FileHistoryEdge trunkEdge = new FileHistoryEdge(g, trunkNode, trunk2Node, IFileHistoryEdge.Type.NORMAL,
                new FileDiff(trunkRev, trunk2Rev));
        assertEquals(Collections.singleton(trunkEdge), trunkNode.getDescendants());
        assertEquals(Collections.singleton(trunkEdge), trunk2Node.getAncestors());
    }

    @Test
    public void testReplacementOfUnknownFile() {
        final IRepository repo = new TestRepository("123", new File("/some/repo"));
        final FileHistoryGraph g = new TestFileHistoryGraph();

        final IRevisionedFile aRevPrev =
                ChangestructureFactory.createFileInRevision("/trunk/a",
                        ChangestructureFactory.createUnknownRevision(repo));
        final IRevisionedFile aRevReplaced =
                ChangestructureFactory.createFileInRevision("/trunk/a", new TestRepoRevision(repo, 2L));

        g.addReplacement(aRevReplaced.getPath(), aRevReplaced.getRevision());

        final FileHistoryNode aNodePrev = g.getNodeFor(aRevPrev);
        assertEquals(aRevPrev, aNodePrev.getFile());
        assertEquals(IFileHistoryNode.Type.UNCONFIRMED, aNodePrev.getType());
        assertEquals(false, aNodePrev.isCopyTarget());
        assertEquals(Collections.emptySet(), aNodePrev.getAncestors());

        final FileHistoryNode aNodeReplaced = g.getNodeFor(aRevReplaced);
        assertEquals(aRevReplaced, aNodeReplaced.getFile());
        assertEquals(IFileHistoryNode.Type.REPLACED, aNodeReplaced.getType());
        assertEquals(false, aNodeReplaced.isCopyTarget());
        assertEquals(Collections.emptySet(), aNodeReplaced.getDescendants());

        final FileHistoryEdge aEdge = new FileHistoryEdge(g, aNodePrev, aNodeReplaced, IFileHistoryEdge.Type.NORMAL,
                new FileDiff(aRevPrev, aRevReplaced));
        assertEquals(Collections.singleton(aEdge), aNodePrev.getDescendants());
        assertEquals(Collections.singleton(aEdge), aNodeReplaced.getAncestors());
    }

    @Test
    public void testReplacementOfKnownDirectory() {
        final IRepository repo = new TestRepository("123", new File("/some/repo"));
        final FileHistoryGraph g = new TestFileHistoryGraph();

        final IRevisionedFile trunkRev =
                ChangestructureFactory.createFileInRevision("/trunk", new TestRepoRevision(repo, 1L));
        final IRevisionedFile xRevPrev =
                ChangestructureFactory.createFileInRevision("/trunk/x", new TestRepoRevision(repo, 1L));
        final IRevisionedFile aRevPrev =
                ChangestructureFactory.createFileInRevision("/trunk/x/a", new TestRepoRevision(repo, 1L));

        g.addAdditionOrChange(trunkRev.getPath(), trunkRev.getRevision(), Collections.<IRevision> emptySet());
        g.addAdditionOrChange(xRevPrev.getPath(), xRevPrev.getRevision(), Collections.<IRevision> emptySet());
        g.addAdditionOrChange(aRevPrev.getPath(), aRevPrev.getRevision(), Collections.<IRevision> emptySet());

        final IRevisionedFile trunk2Rev =
                ChangestructureFactory.createFileInRevision("/trunk", new TestRepoRevision(repo, 2L));
        final IRevisionedFile xRevReplaced =
                ChangestructureFactory.createFileInRevision("/trunk/x", new TestRepoRevision(repo, 2L));
        final IRevisionedFile aRevReplaced =
                ChangestructureFactory.createFileInRevision("/trunk/x/a", new TestRepoRevision(repo, 2L));

        g.addReplacement(xRevReplaced.getPath(), xRevReplaced.getRevision());
        g.addAdditionOrChange(aRevReplaced.getPath(), aRevReplaced.getRevision(), Collections.<IRevision> emptySet());

        final FileHistoryNode aNode = g.getNodeFor(aRevPrev);
        assertEquals(aRevPrev, aNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, aNode.getType());
        assertEquals(false, aNode.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, aNode)), aNode.getAncestors());

        final FileHistoryNode aNodeReplaced = g.getNodeFor(aRevReplaced);
        assertEquals(aRevReplaced, aNodeReplaced.getFile());
        assertEquals(IFileHistoryNode.Type.REPLACED, aNodeReplaced.getType());
        assertEquals(false, aNodeReplaced.isCopyTarget());
        assertEquals(Collections.emptySet(), aNodeReplaced.getDescendants());

        final FileHistoryEdge aEdge = new FileHistoryEdge(g, aNode, aNodeReplaced, IFileHistoryEdge.Type.NORMAL,
                new FileDiff(aRevPrev, aRevReplaced));
        assertEquals(Collections.singleton(aEdge), aNode.getDescendants());
        assertEquals(Collections.singleton(aEdge), aNodeReplaced.getAncestors());

        final FileHistoryNode xOldNode = g.getNodeFor(xRevPrev);
        assertEquals(xRevPrev, xOldNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, xOldNode.getType());
        assertEquals(false, xOldNode.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, xOldNode)), xOldNode.getAncestors());
        assertEquals(Collections.singleton(aNode), xOldNode.getChildren());
        assertEquals(xOldNode, aNode.getParent());

        final FileHistoryNode xNewNode = g.getNodeFor(xRevReplaced);
        assertEquals(xRevReplaced, xNewNode.getFile());
        assertEquals(IFileHistoryNode.Type.REPLACED, xNewNode.getType());
        assertEquals(false, xNewNode.isCopyTarget());
        assertEquals(Collections.emptySet(), xNewNode.getDescendants());
        assertEquals(Collections.singleton(aNodeReplaced), xNewNode.getChildren());
        assertEquals(xNewNode, aNodeReplaced.getParent());

        final FileHistoryEdge xEdge = new FileHistoryEdge(g, xOldNode, xNewNode, IFileHistoryEdge.Type.NORMAL,
                new FileDiff(xRevPrev, xRevReplaced));
        assertEquals(Collections.singleton(xEdge), xOldNode.getDescendants());
        assertEquals(Collections.singleton(xEdge), xNewNode.getAncestors());

        final FileHistoryNode trunkNode = g.getNodeFor(trunkRev);
        assertEquals(trunkRev, trunkNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, trunkNode.getType());
        assertEquals(false, trunkNode.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, trunkNode)), trunkNode.getAncestors());
        assertEquals(Collections.singleton(xOldNode), trunkNode.getChildren());
        assertEquals(trunkNode, xOldNode.getParent());

        final FileHistoryNode trunk2Node = g.getNodeFor(trunk2Rev);
        assertEquals(trunk2Rev, trunk2Node.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, trunk2Node.getType());
        assertEquals(false, trunk2Node.isCopyTarget());
        assertEquals(Collections.emptySet(), trunk2Node.getDescendants());
        assertEquals(Collections.singleton(xNewNode), trunk2Node.getChildren());
        assertEquals(trunk2Node, xNewNode.getParent());

        final FileHistoryEdge trunkEdge = new FileHistoryEdge(g, trunkNode, trunk2Node, IFileHistoryEdge.Type.NORMAL,
                new FileDiff(trunkRev, trunk2Rev));
        assertEquals(Collections.singleton(trunkEdge), trunkNode.getDescendants());
        assertEquals(Collections.singleton(trunkEdge), trunk2Node.getAncestors());
    }

    @Test
    public void testReplacementOfUnknownDirectory() {
        final IRepository repo = new TestRepository("123", new File("/some/repo"));
        final FileHistoryGraph g = new TestFileHistoryGraph();

        final IRevisionedFile trunkRev =
                ChangestructureFactory.createFileInRevision("/trunk",
                        ChangestructureFactory.createUnknownRevision(repo));
        final IRevisionedFile xRevPrev =
                ChangestructureFactory.createFileInRevision("/trunk/x",
                        ChangestructureFactory.createUnknownRevision(repo));
        final IRevisionedFile aRevPrev =
                ChangestructureFactory.createFileInRevision("/trunk/x/a",
                        ChangestructureFactory.createUnknownRevision(repo));

        final IRevisionedFile trunk2Rev =
                ChangestructureFactory.createFileInRevision("/trunk", new TestRepoRevision(repo, 2L));
        final IRevisionedFile xRevReplaced =
                ChangestructureFactory.createFileInRevision("/trunk/x", new TestRepoRevision(repo, 2L));
        final IRevisionedFile aRevReplaced =
                ChangestructureFactory.createFileInRevision("/trunk/x/a", new TestRepoRevision(repo, 2L));

        g.addReplacement(xRevReplaced.getPath(), xRevReplaced.getRevision());
        g.addAdditionOrChange(aRevReplaced.getPath(), aRevReplaced.getRevision(), Collections.<IRevision> emptySet());

        final FileHistoryNode aNodeReplaced = g.getNodeFor(aRevReplaced);
        assertEquals(aRevReplaced, aNodeReplaced.getFile());
        // we do not know anything about old /trunk/x/a here
        assertEquals(IFileHistoryNode.Type.NORMAL, aNodeReplaced.getType());
        assertEquals(false, aNodeReplaced.isCopyTarget());
        assertEquals(Collections.emptySet(), aNodeReplaced.getDescendants());

        final FileHistoryNode aOldNode = g.getNodeFor(aRevPrev);
        assertEquals(aRevPrev, aOldNode.getFile());
        assertEquals(IFileHistoryNode.Type.UNCONFIRMED, aOldNode.getType());
        assertEquals(false, aOldNode.isCopyTarget());
        assertEquals(Collections.emptySet(), aOldNode.getAncestors());
        assertEquals(Collections.emptySet(), aOldNode.getChildren());

        final FileHistoryNode xOldNode = g.getNodeFor(xRevPrev);
        assertEquals(xRevPrev, xOldNode.getFile());
        assertEquals(IFileHistoryNode.Type.UNCONFIRMED, xOldNode.getType());
        assertEquals(false, xOldNode.isCopyTarget());
        assertEquals(Collections.emptySet(), xOldNode.getAncestors());
        assertEquals(Collections.singleton(aOldNode), xOldNode.getChildren());

        final FileHistoryNode xNewNode = g.getNodeFor(xRevReplaced);
        assertEquals(xRevReplaced, xNewNode.getFile());
        assertEquals(IFileHistoryNode.Type.REPLACED, xNewNode.getType());
        assertEquals(false, xNewNode.isCopyTarget());
        assertEquals(Collections.emptySet(), xNewNode.getDescendants());
        assertEquals(Collections.singleton(aNodeReplaced), xNewNode.getChildren());
        assertEquals(xNewNode, aNodeReplaced.getParent());

        final FileHistoryNode trunkNode = g.getNodeFor(trunkRev);
        assertEquals(trunkRev, trunkNode.getFile());
        assertEquals(IFileHistoryNode.Type.UNCONFIRMED, trunkNode.getType());
        assertEquals(false, trunkNode.isCopyTarget());
        assertEquals(Collections.emptySet(), trunkNode.getAncestors());
        assertEquals(Collections.singleton(xOldNode), trunkNode.getChildren());
        assertEquals(trunkNode, xOldNode.getParent());

        final FileHistoryNode trunk2Node = g.getNodeFor(trunk2Rev);
        assertEquals(trunk2Rev, trunk2Node.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, trunk2Node.getType());
        assertEquals(false, trunk2Node.isCopyTarget());
        assertEquals(Collections.emptySet(), trunk2Node.getDescendants());
        assertEquals(Collections.singleton(xNewNode), trunk2Node.getChildren());
        assertEquals(trunk2Node, xNewNode.getParent());

        final FileHistoryEdge trunkEdge = new FileHistoryEdge(g, trunkNode, trunk2Node, IFileHistoryEdge.Type.NORMAL,
                new FileDiff(trunkRev, trunk2Rev));
        assertEquals(Collections.singleton(trunkEdge), trunkNode.getDescendants());
        assertEquals(Collections.singleton(trunkEdge), trunk2Node.getAncestors());
    }

    @Test
    public void testAdditionAndDeletionOfSameFileInSubsequentRevisions() {
        final IRepository repo = new TestRepository("123", new File("/some/repo"));
        final FileHistoryGraph g = new TestFileHistoryGraph();

        final IRevisionedFile aRevNew =
                ChangestructureFactory.createFileInRevision("/trunk/a", new TestRepoRevision(repo, 1L));
        final IRevisionedFile aRevDel =
                ChangestructureFactory.createFileInRevision("/trunk/a", new TestRepoRevision(repo, 2L));

        g.addAdditionOrChange(aRevNew.getPath(), aRevNew.getRevision(), Collections.<IRevision> emptySet());
        g.addDeletion(aRevDel.getPath(), aRevDel.getRevision());

        final FileHistoryNode aNode = g.getNodeFor(aRevNew);
        assertEquals(aRevNew, aNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, aNode.getType());
        assertEquals(false, aNode.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, aNode)), aNode.getAncestors());

        final FileHistoryNode aNodeDel = g.getNodeFor(aRevDel);
        assertEquals(aRevDel, aNodeDel.getFile());
        assertEquals(IFileHistoryNode.Type.DELETED, aNodeDel.getType());
        assertEquals(false, aNodeDel.isCopyTarget());
        assertEquals(Collections.emptySet(), aNodeDel.getDescendants());

        final FileHistoryEdge aEdge = new FileHistoryEdge(g, aNode, aNodeDel, IFileHistoryEdge.Type.NORMAL,
                new FileDiff(aRevNew, aRevDel));
        assertEquals(Collections.singleton(aEdge), aNode.getDescendants());
        assertEquals(Collections.singleton(aEdge), aNodeDel.getAncestors());
    }

    @Test
    public void testAdditionAndDeletionOfSameDirectoryInSubsequentRevisions() {
        final IRepository repo = new TestRepository("123", new File("/some/repo"));
        final FileHistoryGraph g = new TestFileHistoryGraph();

        final IRevisionedFile trunkRevNew =
                ChangestructureFactory.createFileInRevision("/trunk", new TestRepoRevision(repo, 1L));
        final IRevisionedFile aRevNew =
                ChangestructureFactory.createFileInRevision("/trunk/a", new TestRepoRevision(repo, 1L));

        g.addAdditionOrChange(trunkRevNew.getPath(), trunkRevNew.getRevision(), Collections.<IRevision> emptySet());
        g.addAdditionOrChange(aRevNew.getPath(), aRevNew.getRevision(), Collections.<IRevision> emptySet());

        final IRevisionedFile trunkRevDel =
                ChangestructureFactory.createFileInRevision("/trunk", new TestRepoRevision(repo, 2L));
        final IRevisionedFile aRevDel =
                ChangestructureFactory.createFileInRevision("/trunk/a", new TestRepoRevision(repo, 2L));

        g.addDeletion(trunkRevDel.getPath(), trunkRevDel.getRevision());

        final FileHistoryNode aNode = g.getNodeFor(aRevNew);
        assertEquals(aRevNew, aNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, aNode.getType());
        assertEquals(false, aNode.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, aNode)), aNode.getAncestors());

        final FileHistoryNode aNodeDel = g.getNodeFor(aRevDel);
        assertEquals(aRevDel, aNodeDel.getFile());
        assertEquals(IFileHistoryNode.Type.DELETED, aNodeDel.getType());
        assertEquals(false, aNodeDel.isCopyTarget());
        assertEquals(Collections.emptySet(), aNodeDel.getDescendants());

        final FileHistoryEdge aEdge = new FileHistoryEdge(g, aNode, aNodeDel, IFileHistoryEdge.Type.NORMAL,
                new FileDiff(aRevNew, aRevDel));
        assertEquals(Collections.singleton(aEdge), aNode.getDescendants());
        assertEquals(Collections.singleton(aEdge), aNodeDel.getAncestors());

        final FileHistoryNode trunkNode = g.getNodeFor(trunkRevNew);
        assertEquals(trunkRevNew, trunkNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, trunkNode.getType());
        assertEquals(false, trunkNode.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, trunkNode)), trunkNode.getAncestors());
        assertEquals(Collections.singleton(aNode), trunkNode.getChildren());
        assertEquals(trunkNode, aNode.getParent());

        final FileHistoryNode trunkDelNode = g.getNodeFor(trunkRevDel);
        assertEquals(trunkRevDel, trunkDelNode.getFile());
        assertEquals(IFileHistoryNode.Type.DELETED, trunkDelNode.getType());
        assertEquals(false, trunkDelNode.isCopyTarget());
        assertEquals(Collections.emptySet(), trunkDelNode.getDescendants());
        assertEquals(Collections.singleton(aNodeDel), trunkDelNode.getChildren());
        assertEquals(trunkDelNode, aNodeDel.getParent());

        final FileHistoryEdge trunkEdge = new FileHistoryEdge(g, trunkNode, trunkDelNode, IFileHistoryEdge.Type.NORMAL,
                new FileDiff(trunkRevNew, trunkRevDel));
        assertEquals(Collections.singleton(trunkEdge), trunkNode.getDescendants());
        assertEquals(Collections.singleton(trunkEdge), trunkDelNode.getAncestors());
    }

    @Test
    public void testChangeOfKnownFile() {
        final IRepository repo = new TestRepository("123", new File("/some/repo"));
        final FileHistoryGraph g = new TestFileHistoryGraph();

        final IRevisionedFile trunkRevOrig =
                ChangestructureFactory.createFileInRevision("/trunk", new TestRepoRevision(repo, 1L));
        final IRevisionedFile aRevOrig =
                ChangestructureFactory.createFileInRevision("/trunk/a", new TestRepoRevision(repo, 1L));

        g.addAdditionOrChange(trunkRevOrig.getPath(), trunkRevOrig.getRevision(), Collections.<IRevision> emptySet());
        g.addAdditionOrChange(aRevOrig.getPath(), aRevOrig.getRevision(), Collections.<IRevision> emptySet());

        final IRevisionedFile aRevChanged =
                ChangestructureFactory.createFileInRevision(aRevOrig.getPath(), new TestRepoRevision(repo, 2L));

        g.addAdditionOrChange(aRevChanged.getPath(), aRevChanged.getRevision(),
                Collections.singleton(aRevOrig.getRevision()));

        final IRevisionedFile trunkRevChanged =
                ChangestructureFactory.createFileInRevision(trunkRevOrig.getPath(), aRevChanged.getRevision());

        final FileHistoryNode aOrigNode = g.getNodeFor(aRevOrig);
        assertEquals(aRevOrig, aOrigNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, aOrigNode.getType());
        assertEquals(false, aOrigNode.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, aOrigNode)), aOrigNode.getAncestors());

        final FileHistoryNode aChangedNode = g.getNodeFor(aRevChanged);
        assertEquals(aRevChanged, aChangedNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, aChangedNode.getType());
        assertEquals(false, aChangedNode.isCopyTarget());
        assertEquals(Collections.emptySet(), aChangedNode.getDescendants());

        final FileHistoryEdge aEdge = new FileHistoryEdge(g, aOrigNode, aChangedNode, IFileHistoryEdge.Type.NORMAL,
                new FileDiff(aRevOrig, aRevChanged));
        assertEquals(Collections.singleton(aEdge), aOrigNode.getDescendants());
        assertEquals(Collections.singleton(aEdge), aChangedNode.getAncestors());

        final FileHistoryNode trunkOrigNode = g.getNodeFor(trunkRevOrig);
        assertEquals(trunkRevOrig, trunkOrigNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, trunkOrigNode.getType());
        assertEquals(false, trunkOrigNode.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, trunkOrigNode)), trunkOrigNode.getAncestors());
        assertEquals(Collections.singleton(aOrigNode), trunkOrigNode.getChildren());
        assertEquals(trunkOrigNode, aOrigNode.getParent());

        final FileHistoryNode trunkChangedNode = g.getNodeFor(trunkRevChanged);
        assertEquals(trunkRevChanged, trunkChangedNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, trunkChangedNode.getType());
        assertEquals(false, trunkChangedNode.isCopyTarget());
        assertEquals(Collections.emptySet(), trunkChangedNode.getDescendants());
        assertEquals(Collections.singleton(aChangedNode), trunkChangedNode.getChildren());
        assertEquals(trunkChangedNode, aChangedNode.getParent());

        final FileHistoryEdge trunkEdge = new FileHistoryEdge(g, trunkOrigNode, trunkChangedNode,
                IFileHistoryEdge.Type.NORMAL, new FileDiff(trunkRevOrig, trunkRevChanged));
        assertEquals(Collections.singleton(trunkEdge), trunkOrigNode.getDescendants());
        assertEquals(Collections.singleton(trunkEdge), trunkChangedNode.getAncestors());
    }

    @Test
    public void testChangeOfUnknownFile() {
        final IRepository repo = new TestRepository("123", new File("/some/repo"));
        final FileHistoryGraph g = new TestFileHistoryGraph();

        final IRevisionedFile aRevOrig =
                ChangestructureFactory.createFileInRevision("/trunk/a", new TestRepoRevision(repo, 1L));
        final IRevisionedFile aRevChanged =
                ChangestructureFactory.createFileInRevision(aRevOrig.getPath(), new TestRepoRevision(repo, 2L));

        g.addAdditionOrChange(aRevChanged.getPath(), aRevChanged.getRevision(),
                Collections.singleton(aRevOrig.getRevision()));

        final IRevisionedFile trunkRevOrig =
                ChangestructureFactory.createFileInRevision("/trunk", new TestRepoRevision(repo, 1L));
        final IRevisionedFile trunkRevChanged =
                ChangestructureFactory.createFileInRevision(trunkRevOrig.getPath(), aRevChanged.getRevision());

        final FileHistoryNode aOrigNode = g.getNodeFor(aRevOrig);
        assertEquals(aRevOrig, aOrigNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, aOrigNode.getType());
        assertEquals(false, aOrigNode.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, aOrigNode)), aOrigNode.getAncestors());

        final FileHistoryNode aChangedNode = g.getNodeFor(aRevChanged);
        assertEquals(aRevChanged, aChangedNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, aChangedNode.getType());
        assertEquals(false, aChangedNode.isCopyTarget());
        assertEquals(Collections.emptySet(), aChangedNode.getDescendants());

        final FileHistoryEdge aEdge = new FileHistoryEdge(g, aOrigNode, aChangedNode, IFileHistoryEdge.Type.NORMAL,
                new FileDiff(aRevOrig, aRevChanged));
        assertEquals(Collections.singleton(aEdge), aOrigNode.getDescendants());
        assertEquals(Collections.singleton(aEdge), aChangedNode.getAncestors());

        final FileHistoryNode trunkOrigNode = g.getNodeFor(trunkRevOrig);
        assertEquals(trunkRevOrig, trunkOrigNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, trunkOrigNode.getType());
        assertEquals(false, trunkOrigNode.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, trunkOrigNode)), trunkOrigNode.getAncestors());
        assertEquals(Collections.singleton(aOrigNode), trunkOrigNode.getChildren());
        assertEquals(trunkOrigNode, aOrigNode.getParent());

        final FileHistoryNode trunkChangedNode = g.getNodeFor(trunkRevChanged);
        assertEquals(trunkRevChanged, trunkChangedNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, trunkChangedNode.getType());
        assertEquals(false, trunkChangedNode.isCopyTarget());
        assertEquals(Collections.emptySet(), trunkChangedNode.getDescendants());
        assertEquals(Collections.singleton(aChangedNode), trunkChangedNode.getChildren());
        assertEquals(trunkChangedNode, aChangedNode.getParent());

        final FileHistoryEdge trunkEdge = new FileHistoryEdge(g, trunkOrigNode, trunkChangedNode,
                IFileHistoryEdge.Type.NORMAL, new FileDiff(trunkRevOrig, trunkRevChanged));
        assertEquals(Collections.singleton(trunkEdge), trunkOrigNode.getDescendants());
        assertEquals(Collections.singleton(trunkEdge), trunkChangedNode.getAncestors());
    }

    @Test
    public void testChangeOfUnknownFileInKnownDirectory() {
        final IRepository repo = new TestRepository("123", new File("/some/repo"));
        final FileHistoryGraph g = new TestFileHistoryGraph();

        final IRevisionedFile trunkRevOrig =
                ChangestructureFactory.createFileInRevision("/trunk", new TestRepoRevision(repo, 2L));
        final IRevisionedFile xRevOrig =
                ChangestructureFactory.createFileInRevision("/trunk/x", new TestRepoRevision(repo, 2L));

        g.addAdditionOrChange(xRevOrig.getPath(), xRevOrig.getRevision(), Collections.<IRevision> emptySet());

        final IRevisionedFile aRevOrig =
                ChangestructureFactory.createFileInRevision("/trunk/a", new TestRepoRevision(repo, 2L));
        final IRevisionedFile aRevChanged =
                ChangestructureFactory.createFileInRevision(aRevOrig.getPath(), new TestRepoRevision(repo, 3L));

        g.addAdditionOrChange(aRevChanged.getPath(), aRevChanged.getRevision(),
                Collections.singleton(aRevOrig.getRevision()));

        final IRevisionedFile trunkRevChanged =
                ChangestructureFactory.createFileInRevision(trunkRevOrig.getPath(), aRevChanged.getRevision());

        final FileHistoryNode xNode = g.getNodeFor(xRevOrig);
        assertEquals(xRevOrig, xNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, xNode.getType());
        assertEquals(false, xNode.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, xNode)), xNode.getAncestors());

        final FileHistoryNode aOrigNode = g.getNodeFor(aRevOrig);
        assertEquals(aRevOrig, aOrigNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, aOrigNode.getType());
        assertEquals(false, aOrigNode.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, aOrigNode)), aOrigNode.getAncestors());

        final FileHistoryNode aChangedNode = g.getNodeFor(aRevChanged);
        assertEquals(aRevChanged, aChangedNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, aChangedNode.getType());
        assertEquals(false, aChangedNode.isCopyTarget());
        assertEquals(Collections.emptySet(), aChangedNode.getDescendants());

        final FileHistoryEdge aEdge = new FileHistoryEdge(g, aOrigNode, aChangedNode, IFileHistoryEdge.Type.NORMAL,
                new FileDiff(aRevOrig, aRevChanged));
        assertEquals(Collections.singleton(aEdge), aOrigNode.getDescendants());
        assertEquals(Collections.singleton(aEdge), aChangedNode.getAncestors());

        final FileHistoryNode trunkOrigNode = g.getNodeFor(trunkRevOrig);
        assertEquals(trunkRevOrig, trunkOrigNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, trunkOrigNode.getType());
        assertEquals(false, trunkOrigNode.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, trunkOrigNode)), trunkOrigNode.getAncestors());
        assertEquals(new HashSet<>(Arrays.asList(xNode, aOrigNode)), trunkOrigNode.getChildren());

        final FileHistoryNode trunkChangedNode = g.getNodeFor(trunkRevChanged);
        assertEquals(trunkRevChanged, trunkChangedNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, trunkChangedNode.getType());
        assertEquals(false, trunkChangedNode.isCopyTarget());
        assertEquals(Collections.emptySet(), trunkChangedNode.getDescendants());
        assertEquals(Collections.singleton(aChangedNode), trunkChangedNode.getChildren());
        assertEquals(trunkChangedNode, aChangedNode.getParent());

        final FileHistoryEdge trunkEdge = new FileHistoryEdge(g, trunkOrigNode, trunkChangedNode,
                IFileHistoryEdge.Type.NORMAL, new FileDiff(trunkRevOrig, trunkRevChanged));
        assertEquals(Collections.singleton(trunkEdge), trunkOrigNode.getDescendants());
        assertEquals(Collections.singleton(trunkEdge), trunkChangedNode.getAncestors());
    }

    @Test
    public void testCopyOfFile() {
        final IRepository repo = new TestRepository("123", new File("/some/repo"));
        final FileHistoryGraph g = new TestFileHistoryGraph();

        final IRevisionedFile aRevOrig =
                ChangestructureFactory.createFileInRevision("/trunk/a", new TestRepoRevision(repo, 1L));
        final IRevisionedFile aRevCopy =
                ChangestructureFactory.createFileInRevision("/trunk/b", new TestRepoRevision(repo, 2L));

        g.addCopy(aRevOrig.getPath(), aRevCopy.getPath(), aRevOrig.getRevision(), aRevCopy.getRevision());

        final FileHistoryNode aOrigNode = g.getNodeFor(aRevOrig);
        assertEquals(aRevOrig, aOrigNode.getFile());
        assertEquals(IFileHistoryNode.Type.UNCONFIRMED, aOrigNode.getType());
        assertEquals(false, aOrigNode.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, aOrigNode)), aOrigNode.getAncestors());

        final FileHistoryNode aCopyNode = g.getNodeFor(aRevCopy);
        assertEquals(aRevCopy, aCopyNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, aCopyNode.getType());
        assertEquals(true, aCopyNode.isCopyTarget());
        assertEquals(Collections.emptySet(), aCopyNode.getDescendants());

        assertNull(g.getNodeFor(
                ChangestructureFactory.createFileInRevision(aRevOrig.getPath(), aRevCopy.getRevision())));

        final FileHistoryEdge aEdge = new FileHistoryEdge(g, aOrigNode, aCopyNode, IFileHistoryEdge.Type.COPY,
                new FileDiff(aRevOrig, aRevCopy));
        assertEquals(Collections.singleton(aEdge), aOrigNode.getDescendants());
        assertEquals(Collections.singleton(aEdge), aCopyNode.getAncestors());

        final IRevisionedFile trunkRev =
                ChangestructureFactory.createFileInRevision("/trunk", aRevOrig.getRevision());
        final FileHistoryNode trunkNode = g.getNodeFor(trunkRev);
        final IRevisionedFile trunkRev2 =
                ChangestructureFactory.createFileInRevision("/trunk", aRevCopy.getRevision());
        final FileHistoryNode trunkNode2 = g.getNodeFor(trunkRev2);

        assertEquals(Collections.singleton(aOrigNode), trunkNode.getChildren());
        assertEquals(trunkNode, aOrigNode.getParent());

        assertEquals(Collections.singleton(aCopyNode), trunkNode2.getChildren());
        assertEquals(trunkNode2, aCopyNode.getParent());

        final FileHistoryEdge trunkEdge = new FileHistoryEdge(g, trunkNode, trunkNode2, IFileHistoryEdge.Type.NORMAL,
                new FileDiff(trunkRev, trunkRev2));
        assertEquals(Collections.singleton(trunkEdge), trunkNode.getDescendants());
        assertEquals(Collections.singleton(trunkEdge), trunkNode2.getAncestors());
    }

    @Test
    public void testCopyAndChangeOfFile() {
        final IRepository repo = new TestRepository("123", new File("/some/repo"));
        final FileHistoryGraph g = new TestFileHistoryGraph();

        final IRevisionedFile aRevOrig =
                ChangestructureFactory.createFileInRevision("/trunk/a", new TestRepoRevision(repo, 1L));
        final IRevisionedFile aRevCopy =
                ChangestructureFactory.createFileInRevision("/trunk/b", new TestRepoRevision(repo, 2L));

        g.addCopy(aRevOrig.getPath(), aRevCopy.getPath(), aRevOrig.getRevision(), aRevCopy.getRevision());
        g.addAdditionOrChange(aRevCopy.getPath(), aRevCopy.getRevision(),
                Collections.singleton(aRevOrig.getRevision()));

        final FileHistoryNode aOrigNode = g.getNodeFor(aRevOrig);
        assertEquals(aRevOrig, aOrigNode.getFile());
        assertEquals(IFileHistoryNode.Type.UNCONFIRMED, aOrigNode.getType());
        assertEquals(false, aOrigNode.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, aOrigNode)), aOrigNode.getAncestors());

        final FileHistoryNode aCopyNode = g.getNodeFor(aRevCopy);
        assertEquals(aRevCopy, aCopyNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, aCopyNode.getType());
        assertEquals(true, aCopyNode.isCopyTarget());
        assertEquals(Collections.emptySet(), aCopyNode.getDescendants());

        assertNull(g.getNodeFor(
                ChangestructureFactory.createFileInRevision(aRevOrig.getPath(), aRevCopy.getRevision())));

        final FileHistoryEdge aEdge = new FileHistoryEdge(g, aOrigNode, aCopyNode, IFileHistoryEdge.Type.COPY,
                new FileDiff(aRevOrig, aRevCopy));
        assertEquals(Collections.singleton(aEdge), aOrigNode.getDescendants());
        assertEquals(Collections.singleton(aEdge), aCopyNode.getAncestors());

        final IRevisionedFile trunkRev =
                ChangestructureFactory.createFileInRevision("/trunk", aRevOrig.getRevision());
        final FileHistoryNode trunkNode = g.getNodeFor(trunkRev);
        final IRevisionedFile trunkRev2 =
                ChangestructureFactory.createFileInRevision("/trunk", aRevCopy.getRevision());
        final FileHistoryNode trunkNode2 = g.getNodeFor(trunkRev2);

        assertEquals(Collections.singleton(aOrigNode), trunkNode.getChildren());
        assertEquals(trunkNode, aOrigNode.getParent());

        assertEquals(Collections.singleton(aCopyNode), trunkNode2.getChildren());
        assertEquals(trunkNode2, aCopyNode.getParent());

        final FileHistoryEdge trunkEdge = new FileHistoryEdge(g, trunkNode, trunkNode2, IFileHistoryEdge.Type.NORMAL,
                new FileDiff(trunkRev, trunkRev2));
        assertEquals(Collections.singleton(trunkEdge), trunkNode.getDescendants());
        assertEquals(Collections.singleton(trunkEdge), trunkNode2.getAncestors());
    }

    @Test
    public void testCopyOfFileWithSourceFromIntermediateRevisions() {
        final IRepository repo = new TestRepository("123", new File("/some/repo"));
        final FileHistoryGraph g = new TestFileHistoryGraph();

        final IRevisionedFile aRevOrig =
                ChangestructureFactory.createFileInRevision("/trunk/a", new TestRepoRevision(repo, 1L));

        g.addAdditionOrChange(aRevOrig.getPath(), aRevOrig.getRevision(), Collections.<IRevision> emptySet());

        final IRevisionedFile aRevChanged =
                ChangestructureFactory.createFileInRevision(aRevOrig.getPath(), new TestRepoRevision(repo, 2L));

        g.addAdditionOrChange(aRevChanged.getPath(), aRevChanged.getRevision(),
                Collections.singleton(aRevOrig.getRevision()));

        final IRevisionedFile aRevCopy =
                ChangestructureFactory.createFileInRevision("/trunk/b", new TestRepoRevision(repo, 4L));
        final IRevisionedFile aRevSource2 =
                ChangestructureFactory.createFileInRevision(aRevOrig.getPath(), new TestRepoRevision(repo, 3L));
        final IRevisionedFile aRevCopy2 =
                ChangestructureFactory.createFileInRevision("/trunk/c", new TestRepoRevision(repo, 5L));

        g.addCopy(aRevChanged.getPath(), aRevCopy.getPath(), aRevChanged.getRevision(), aRevCopy.getRevision());
        g.addCopy(aRevSource2.getPath(), aRevCopy2.getPath(), aRevSource2.getRevision(), aRevCopy2.getRevision());

        final FileHistoryNode aOrigNode = g.getNodeFor(aRevOrig);
        assertEquals(aRevOrig, aOrigNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, aOrigNode.getType());
        assertEquals(false, aOrigNode.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, aOrigNode)), aOrigNode.getAncestors());

        final FileHistoryNode aChangedNode = g.getNodeFor(aRevChanged);
        assertEquals(aRevChanged, aChangedNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, aChangedNode.getType());
        assertEquals(false, aChangedNode.isCopyTarget());

        final FileHistoryNode aSourceNode = g.getNodeFor(aRevChanged);
        assertEquals(aRevChanged, aSourceNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, aSourceNode.getType());
        assertEquals(false, aSourceNode.isCopyTarget());

        final FileHistoryNode aSource2Node = g.getNodeFor(aRevSource2);
        assertEquals(aRevSource2, aSource2Node.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, aSource2Node.getType());
        assertEquals(false, aSource2Node.isCopyTarget());

        final FileHistoryNode aCopyNode = g.getNodeFor(aRevCopy);
        assertEquals(aRevCopy, aCopyNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, aCopyNode.getType());
        assertEquals(true, aCopyNode.isCopyTarget());
        assertEquals(Collections.emptySet(), aCopyNode.getDescendants());

        final FileHistoryNode aCopy2Node = g.getNodeFor(aRevCopy2);
        assertEquals(aRevCopy2, aCopy2Node.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, aCopy2Node.getType());
        assertEquals(true, aCopy2Node.isCopyTarget());
        assertEquals(Collections.emptySet(), aCopy2Node.getDescendants());

        final FileHistoryEdge aSourceEdge = new FileHistoryEdge(g, aOrigNode, aSourceNode, IFileHistoryEdge.Type.NORMAL,
                new FileDiff(aRevOrig, aRevChanged));
        assertEquals(Collections.singleton(aSourceEdge), aOrigNode.getDescendants());
        assertEquals(Collections.singleton(aSourceEdge), aSourceNode.getAncestors());

        final FileHistoryEdge aCopy1Edge = new FileHistoryEdge(g, aSourceNode, aSource2Node,
                IFileHistoryEdge.Type.NORMAL, new FileDiff(aRevChanged, aRevSource2));
        final FileHistoryEdge aCopy2Edge = new FileHistoryEdge(g, aSourceNode, aCopyNode, IFileHistoryEdge.Type.COPY,
                new FileDiff(aRevChanged, aRevCopy));
        final FileHistoryEdge aCopy3Edge = new FileHistoryEdge(g, aSource2Node, aCopy2Node,
                IFileHistoryEdge.Type.COPY, new FileDiff(aRevSource2, aRevCopy2));
        assertEquals(new HashSet<>(Arrays.asList(aCopy1Edge, aCopy2Edge)), aSourceNode.getDescendants());
        assertEquals(Collections.singleton(aCopy3Edge), aSource2Node.getDescendants());
        assertEquals(Collections.singleton(aCopy2Edge), aCopyNode.getAncestors());
        assertEquals(Collections.singleton(aCopy3Edge), aCopy2Node.getAncestors());
    }

    @Test
    public void testMovementOfFile() {
        final IRepository repo = new TestRepository("123", new File("/some/repo"));
        final FileHistoryGraph g = new TestFileHistoryGraph();

        final IRevisionedFile aRevOrig =
                ChangestructureFactory.createFileInRevision("/trunk/a", new TestRepoRevision(repo, 1L));
        final IRevisionedFile aRevDel =
                ChangestructureFactory.createFileInRevision("/trunk/a", new TestRepoRevision(repo, 2L));
        final IRevisionedFile aRevCopy =
                ChangestructureFactory.createFileInRevision("/trunk/b", new TestRepoRevision(repo, 2L));

        g.addCopy(aRevOrig.getPath(), aRevCopy.getPath(), aRevOrig.getRevision(), aRevCopy.getRevision());
        g.addDeletion(aRevDel.getPath(), aRevDel.getRevision());

        final FileHistoryNode aOrigNode = g.getNodeFor(aRevOrig);
        assertEquals(aRevOrig, aOrigNode.getFile());
        assertEquals(IFileHistoryNode.Type.UNCONFIRMED, aOrigNode.getType());
        assertEquals(false, aOrigNode.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, aOrigNode)), aOrigNode.getAncestors());

        final FileHistoryNode aCopyNode = g.getNodeFor(aRevCopy);
        assertEquals(aRevCopy, aCopyNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, aCopyNode.getType());
        assertEquals(true, aCopyNode.isCopyTarget());
        assertEquals(Collections.emptySet(), aCopyNode.getDescendants());

        final FileHistoryNode aDelNode = g.getNodeFor(aRevDel);
        assertEquals(aRevDel, aDelNode.getFile());
        assertEquals(IFileHistoryNode.Type.DELETED, aDelNode.getType());
        assertEquals(false, aDelNode.isCopyTarget());
        assertEquals(Collections.emptySet(), aDelNode.getDescendants());

        final FileHistoryEdge aEdgeCopy = new FileHistoryEdge(g, aOrigNode, aCopyNode, IFileHistoryEdge.Type.COPY,
                new FileDiff(aRevOrig, aRevCopy));
        final FileHistoryEdge aEdgeDel = new FileHistoryEdge(g, aOrigNode, aDelNode, IFileHistoryEdge.Type.NORMAL,
                new FileDiff(aRevOrig, aRevDel));
        assertEquals(new HashSet<>(Arrays.asList(aEdgeCopy, aEdgeDel)), aOrigNode.getDescendants());
        assertEquals(Collections.singleton(aEdgeCopy), aCopyNode.getAncestors());
        assertEquals(Collections.singleton(aEdgeDel), aDelNode.getAncestors());

        final IRevisionedFile trunkRev =
                ChangestructureFactory.createFileInRevision("/trunk", new TestRepoRevision(repo, 1L));
        final FileHistoryNode trunkNode = g.getNodeFor(trunkRev);
        final IRevisionedFile trunkRev2 =
                ChangestructureFactory.createFileInRevision("/trunk", new TestRepoRevision(repo, 2L));
        final FileHistoryNode trunkNode2 = g.getNodeFor(trunkRev2);

        assertEquals(Collections.singleton(aOrigNode), trunkNode.getChildren());
        assertEquals(trunkNode, aOrigNode.getParent());

        assertEquals(new HashSet<>(Arrays.asList(aCopyNode, aDelNode)), trunkNode2.getChildren());
        assertEquals(trunkNode2, aCopyNode.getParent());
        assertEquals(trunkNode2, aDelNode.getParent());

        final FileHistoryEdge trunkEdge = new FileHistoryEdge(g, trunkNode, trunkNode2, IFileHistoryEdge.Type.NORMAL,
                new FileDiff(trunkRev, trunkRev2));
        assertEquals(Collections.singleton(trunkEdge), trunkNode.getDescendants());
        assertEquals(Collections.singleton(trunkEdge), trunkNode2.getAncestors());
    }

    @Test
    public void testMovementOfFile2() {
        final IRepository repo = new TestRepository("123", new File("/some/repo"));
        final FileHistoryGraph g = new TestFileHistoryGraph();

        final IRevisionedFile aRevOrig =
                ChangestructureFactory.createFileInRevision("/trunk/a", new TestRepoRevision(repo, 1L));
        final IRevisionedFile aRevDel =
                ChangestructureFactory.createFileInRevision("/trunk/a", new TestRepoRevision(repo, 2L));
        final IRevisionedFile aRevCopy =
                ChangestructureFactory.createFileInRevision("/trunk/b", new TestRepoRevision(repo, 2L));

        g.addDeletion(aRevDel.getPath(), aRevDel.getRevision());
        g.addCopy(aRevOrig.getPath(), aRevCopy.getPath(), aRevOrig.getRevision(), aRevCopy.getRevision());

        final FileHistoryNode aOrigNode = g.getNodeFor(aRevOrig);
        assertEquals(aRevOrig, aOrigNode.getFile());
        assertEquals(IFileHistoryNode.Type.UNCONFIRMED, aOrigNode.getType());
        assertEquals(false, aOrigNode.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, aOrigNode)), aOrigNode.getAncestors());

        final FileHistoryNode aCopyNode = g.getNodeFor(aRevCopy);
        assertEquals(aRevCopy, aCopyNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, aCopyNode.getType());
        assertEquals(true, aCopyNode.isCopyTarget());
        assertEquals(Collections.emptySet(), aCopyNode.getDescendants());

        final FileHistoryNode aDelNode = g.getNodeFor(aRevDel);
        assertEquals(aRevDel, aDelNode.getFile());
        assertEquals(IFileHistoryNode.Type.DELETED, aDelNode.getType());
        assertEquals(false, aDelNode.isCopyTarget());
        assertEquals(Collections.emptySet(), aDelNode.getDescendants());

        final FileHistoryEdge aEdgeCopy = new FileHistoryEdge(g, aOrigNode, aCopyNode, IFileHistoryEdge.Type.COPY,
                new FileDiff(aRevOrig, aRevCopy));
        assertEquals(Collections.singleton(aEdgeCopy), aOrigNode.getDescendants());
        assertEquals(Collections.singleton(aEdgeCopy), aCopyNode.getAncestors());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, aDelNode)), aDelNode.getAncestors());

        final IRevisionedFile trunkRev =
                ChangestructureFactory.createFileInRevision("/trunk", new TestRepoRevision(repo, 1L));
        final FileHistoryNode trunkNode = g.getNodeFor(trunkRev);
        final IRevisionedFile trunkRev2 =
                ChangestructureFactory.createFileInRevision("/trunk", new TestRepoRevision(repo, 2L));
        final FileHistoryNode trunkNode2 = g.getNodeFor(trunkRev2);

        assertEquals(Collections.singleton(aOrigNode), trunkNode.getChildren());
        assertEquals(trunkNode, aOrigNode.getParent());

        assertEquals(new HashSet<>(Arrays.asList(aCopyNode, aDelNode)), trunkNode2.getChildren());
        assertEquals(trunkNode2, aCopyNode.getParent());
        assertEquals(trunkNode2, aDelNode.getParent());

        assertEquals(Collections.emptySet(), trunkNode.getDescendants());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, trunkNode2)), trunkNode2.getAncestors());
    }

    @Test
    public void testCopyOfDirectory() {
        final IRepository repo = new TestRepository("123", new File("/some/repo"));
        final FileHistoryGraph g = new TestFileHistoryGraph();

        final IRevisionedFile aRevOrig =
                ChangestructureFactory.createFileInRevision("/trunk/x/a", new TestRepoRevision(repo, 1L));

        g.addAdditionOrChange(aRevOrig.getPath(), aRevOrig.getRevision(), Collections.<IRevision> emptySet());

        final IRevisionedFile xRev =
                ChangestructureFactory.createFileInRevision("/trunk/x", new TestRepoRevision(repo, 1L));
        final IRevisionedFile yRev =
                ChangestructureFactory.createFileInRevision("/trunk/y", new TestRepoRevision(repo, 2L));
        final IRevisionedFile aRevCopy =
                ChangestructureFactory.createFileInRevision("/trunk/y/a", yRev.getRevision());

        g.addCopy(xRev.getPath(), yRev.getPath(), xRev.getRevision(), yRev.getRevision());

        final FileHistoryNode aOrigNode = g.getNodeFor(aRevOrig);
        assertEquals(aRevOrig, aOrigNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, aOrigNode.getType());
        assertEquals(false, aOrigNode.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, aOrigNode)), aOrigNode.getAncestors());

        final FileHistoryNode aCopyNode = g.getNodeFor(aRevCopy);
        assertEquals(aRevCopy, aCopyNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, aCopyNode.getType());
        assertEquals(true, aCopyNode.isCopyTarget());
        assertEquals(Collections.emptySet(), aCopyNode.getDescendants());

        final IRevisionedFile aRevDel =
                ChangestructureFactory.createFileInRevision(aRevOrig.getPath(), yRev.getRevision());
        assertNull(g.getNodeFor(aRevDel));

        final FileHistoryEdge aEdge = new FileHistoryEdge(g, aOrigNode, aCopyNode, IFileHistoryEdge.Type.COPY,
                new FileDiff(aRevOrig, aRevCopy));
        assertEquals(Collections.singleton(aEdge), aOrigNode.getDescendants());
        assertEquals(Collections.singleton(aEdge), aCopyNode.getAncestors());

        final FileHistoryNode xNode = g.getNodeFor(xRev);
        final FileHistoryNode yNode = g.getNodeFor(yRev);

        assertEquals(Collections.singleton(aOrigNode), xNode.getChildren());
        assertEquals(xNode, aOrigNode.getParent());

        assertEquals(Collections.singleton(aCopyNode), yNode.getChildren());
        assertEquals(yNode, aCopyNode.getParent());

        final FileHistoryEdge xyEdge = new FileHistoryEdge(g, xNode, yNode, IFileHistoryEdge.Type.COPY,
                new FileDiff(xRev, yRev));
        assertEquals(Collections.singleton(xyEdge), xNode.getDescendants());
        assertEquals(Collections.singleton(xyEdge), yNode.getAncestors());
    }

    @Test
    public void testCopyOfDirectoryWithDeletedNodes() {
        final IRepository repo = new TestRepository("123", new File("/some/repo"));
        final FileHistoryGraph g = new TestFileHistoryGraph();

        final IRevisionedFile xRevOrig =
                ChangestructureFactory.createFileInRevision("/trunk/x", new TestRepoRevision(repo, 1L));
        final IRevisionedFile aRevOrig =
                ChangestructureFactory.createFileInRevision("/trunk/x/a", xRevOrig.getRevision());

        g.addAdditionOrChange(aRevOrig.getPath(), aRevOrig.getRevision(), Collections.<IRevision> emptySet());

        final IRevisionedFile xRev =
                ChangestructureFactory.createFileInRevision("/trunk/x", new TestRepoRevision(repo, 2L));
        final IRevisionedFile aRevDel =
                ChangestructureFactory.createFileInRevision(aRevOrig.getPath(), xRev.getRevision());

        g.addDeletion(aRevDel.getPath(), aRevDel.getRevision());

        final IRevisionedFile yRev =
                ChangestructureFactory.createFileInRevision("/trunk/y", new TestRepoRevision(repo, 3L));

        g.addCopy(xRev.getPath(), yRev.getPath(), xRev.getRevision(), yRev.getRevision());

        final FileHistoryNode aOrigNode = g.getNodeFor(aRevOrig);
        assertEquals(aRevOrig, aOrigNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, aOrigNode.getType());
        assertEquals(false, aOrigNode.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, aOrigNode)), aOrigNode.getAncestors());

        final FileHistoryNode aDelNode = g.getNodeFor(aRevDel);
        assertEquals(aRevDel, aDelNode.getFile());
        assertEquals(IFileHistoryNode.Type.DELETED, aDelNode.getType());
        assertEquals(false, aDelNode.isCopyTarget());
        assertEquals(Collections.emptySet(), aDelNode.getDescendants());

        final FileHistoryEdge aEdge = new FileHistoryEdge(g, aOrigNode, aDelNode, IFileHistoryEdge.Type.NORMAL,
                new FileDiff(aRevOrig, aRevDel));
        assertEquals(Collections.singleton(aEdge), aOrigNode.getDescendants());
        assertEquals(Collections.singleton(aEdge), aDelNode.getAncestors());

        final FileHistoryNode xOrigNode = g.getNodeFor(xRevOrig);
        final FileHistoryNode xNode = g.getNodeFor(xRev);
        final FileHistoryNode yNode = g.getNodeFor(yRev);

        assertEquals(Collections.singleton(aOrigNode), xOrigNode.getChildren());
        assertEquals(xOrigNode, aOrigNode.getParent());

        assertEquals(Collections.singleton(aDelNode), xNode.getChildren());
        assertEquals(xNode, aDelNode.getParent());

        assertEquals(Collections.emptySet(), yNode.getChildren());

        final FileHistoryEdge xxEdge = new FileHistoryEdge(g, xOrigNode, xNode, IFileHistoryEdge.Type.NORMAL,
                new FileDiff(xRevOrig, xRev));
        assertEquals(Collections.singleton(xxEdge), xOrigNode.getDescendants());
        assertEquals(Collections.singleton(xxEdge), xNode.getAncestors());

        final FileHistoryEdge xyEdge = new FileHistoryEdge(g, xNode, yNode, IFileHistoryEdge.Type.COPY,
                new FileDiff(xRev, yRev));
        assertEquals(Collections.singleton(xyEdge), xNode.getDescendants());
        assertEquals(Collections.singleton(xyEdge), yNode.getAncestors());
    }

    @Test
    public void testAdditionInCopiedKnownDirectory() {
        final IRepository repo = new TestRepository("123", new File("/some/repo"));
        final FileHistoryGraph g = new TestFileHistoryGraph();

        final IRevisionedFile xRevOrig =
                ChangestructureFactory.createFileInRevision("/trunk/x", new TestRepoRevision(repo, 1L));

        g.addAdditionOrChange(xRevOrig.getPath(), xRevOrig.getRevision(), Collections.<IRevision> emptySet());

        final IRevisionedFile yRev =
                ChangestructureFactory.createFileInRevision("/trunk/y", new TestRepoRevision(repo, 2L));

        g.addCopy(xRevOrig.getPath(), yRev.getPath(), xRevOrig.getRevision(), yRev.getRevision());

        final IRevisionedFile aRev =
                ChangestructureFactory.createFileInRevision("/trunk/y/a", yRev.getRevision());

        g.addAdditionOrChange(aRev.getPath(), aRev.getRevision(), Collections.<IRevision> emptySet());

        final FileHistoryNode xNode = g.getNodeFor(xRevOrig);
        assertEquals(xRevOrig, xNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, xNode.getType());
        assertEquals(false, xNode.isCopyTarget());
        assertEquals(Collections.emptySet(), xNode.getChildren());

        final FileHistoryNode aNode = g.getNodeFor(aRev);
        assertEquals(aRev, aNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, aNode.getType());
        assertEquals(false, aNode.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, aNode)), aNode.getAncestors());
        assertEquals(Collections.emptySet(), aNode.getDescendants());

        final FileHistoryNode yNode = g.getNodeFor(yRev);
        assertEquals(yRev, yNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, yNode.getType());
        assertEquals(true, yNode.isCopyTarget());
        assertEquals(Collections.singleton(aNode), yNode.getChildren());
        assertEquals(yNode, aNode.getParent());

        final FileHistoryEdge trunkEdge = new FileHistoryEdge(g, xNode, yNode, IFileHistoryEdge.Type.COPY,
                new FileDiff(xRevOrig, yRev));
        assertEquals(Collections.singleton(trunkEdge), xNode.getDescendants());
        assertEquals(Collections.singleton(trunkEdge), yNode.getAncestors());
    }


    @Test
    public void testAdditionInCopiedUnknownDirectory() {
        final IRepository repo = new TestRepository("123", new File("/some/repo"));
        final FileHistoryGraph g = new TestFileHistoryGraph();

        final IRevisionedFile xRevOrig =
                ChangestructureFactory.createFileInRevision("/trunk/x", new TestRepoRevision(repo, 1L));
        final IRevisionedFile yRev =
                ChangestructureFactory.createFileInRevision("/trunk/y", new TestRepoRevision(repo, 2L));

        g.addCopy(xRevOrig.getPath(), yRev.getPath(), xRevOrig.getRevision(), yRev.getRevision());

        final IRevisionedFile aRev =
                ChangestructureFactory.createFileInRevision("/trunk/y/a", yRev.getRevision());

        g.addAdditionOrChange(aRev.getPath(), aRev.getRevision(), Collections.<IRevision> emptySet());

        final FileHistoryNode xNode = g.getNodeFor(xRevOrig);
        assertEquals(xRevOrig, xNode.getFile());
        assertEquals(IFileHistoryNode.Type.UNCONFIRMED, xNode.getType());
        assertEquals(false, xNode.isCopyTarget());
        assertEquals(Collections.emptySet(), xNode.getChildren());

        final FileHistoryNode aNode = g.getNodeFor(aRev);
        assertEquals(aRev, aNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, aNode.getType());
        assertEquals(false, aNode.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, aNode)), aNode.getAncestors());
        assertEquals(Collections.emptySet(), aNode.getDescendants());

        final FileHistoryNode yNode = g.getNodeFor(yRev);
        assertEquals(yRev, yNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, yNode.getType());
        assertEquals(true, yNode.isCopyTarget());
        assertEquals(Collections.singleton(aNode), yNode.getChildren());
        assertEquals(yNode, aNode.getParent());

        final FileHistoryEdge trunkEdge = new FileHistoryEdge(g, xNode, yNode, IFileHistoryEdge.Type.COPY,
                new FileDiff(xRevOrig, yRev));
        assertEquals(Collections.singleton(trunkEdge), xNode.getDescendants());
        assertEquals(Collections.singleton(trunkEdge), yNode.getAncestors());
    }

    @Test
    public void testDeletionInCopiedKnownDirectory() {
        final IRepository repo = new TestRepository("123", new File("/some/repo"));
        final FileHistoryGraph g = new TestFileHistoryGraph();

        final IRevisionedFile xRevOrig =
                ChangestructureFactory.createFileInRevision("/trunk/x", new TestRepoRevision(repo, 1L));
        final IRevisionedFile aRevOrig =
                ChangestructureFactory.createFileInRevision("/trunk/x/a", xRevOrig.getRevision());

        g.addAdditionOrChange(xRevOrig.getPath(), xRevOrig.getRevision(), Collections.<IRevision> emptySet());
        g.addAdditionOrChange(aRevOrig.getPath(), aRevOrig.getRevision(), Collections.<IRevision> emptySet());

        final IRevisionedFile yRev =
                ChangestructureFactory.createFileInRevision("/trunk/y", new TestRepoRevision(repo, 2L));
        final IRevisionedFile aRevDel =
                ChangestructureFactory.createFileInRevision("/trunk/y/a", yRev.getRevision());

        g.addCopy(xRevOrig.getPath(), yRev.getPath(), xRevOrig.getRevision(), yRev.getRevision());
        g.addDeletion(aRevDel.getPath(), aRevDel.getRevision());

        final FileHistoryNode aOrigNode = g.getNodeFor(aRevOrig);
        assertEquals(aRevOrig, aOrigNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, aOrigNode.getType());
        assertEquals(false, aOrigNode.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, aOrigNode)), aOrigNode.getAncestors());

        final FileHistoryNode xNode = g.getNodeFor(xRevOrig);
        assertEquals(xRevOrig, xNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, xNode.getType());
        assertEquals(false, xNode.isCopyTarget());
        assertEquals(Collections.singleton(aOrigNode), xNode.getChildren());

        final FileHistoryNode aDelNode = g.getNodeFor(aRevDel);
        assertEquals(aRevDel, aDelNode.getFile());
        assertEquals(IFileHistoryNode.Type.DELETED, aDelNode.getType());
        assertEquals(true, aDelNode.isCopyTarget());
        assertEquals(Collections.emptySet(), aDelNode.getDescendants());

        final FileHistoryNode yNode = g.getNodeFor(yRev);
        assertEquals(yRev, yNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, yNode.getType());
        assertEquals(true, yNode.isCopyTarget());
        assertEquals(Collections.singleton(aDelNode), yNode.getChildren());
        assertEquals(yNode, aDelNode.getParent());

        final FileHistoryEdge xyEdge = new FileHistoryEdge(g, xNode, yNode, IFileHistoryEdge.Type.COPY,
                new FileDiff(xRevOrig, yRev));
        assertEquals(Collections.singleton(xyEdge), xNode.getDescendants());
        assertEquals(Collections.singleton(xyEdge), yNode.getAncestors());

        final FileHistoryEdge aEdge = new FileHistoryEdge(g, aOrigNode, aDelNode, IFileHistoryEdge.Type.COPY_DELETED,
                new FileDiff(aRevOrig, aRevDel));
        assertEquals(Collections.singleton(aEdge), aOrigNode.getDescendants());
        assertEquals(Collections.singleton(aEdge), aDelNode.getAncestors());
    }

    @Test
    public void testDeletionInCopiedUnknownDirectory() {
        final IRepository repo = new TestRepository("123", new File("/some/repo"));
        final FileHistoryGraph g = new TestFileHistoryGraph();

        final IRevisionedFile xRevOrig =
                ChangestructureFactory.createFileInRevision("/trunk/x", new TestRepoRevision(repo, 1L));
        final IRevisionedFile aRevOrig =
                ChangestructureFactory.createFileInRevision("/trunk/x/a", xRevOrig.getRevision());

        final IRevisionedFile yRev =
                ChangestructureFactory.createFileInRevision("/trunk/y", new TestRepoRevision(repo, 2L));
        final IRevisionedFile aRevDel =
                ChangestructureFactory.createFileInRevision("/trunk/y/a", yRev.getRevision());

        g.addCopy(xRevOrig.getPath(), yRev.getPath(), xRevOrig.getRevision(), yRev.getRevision());
        g.addDeletion(aRevDel.getPath(), aRevDel.getRevision());

        final FileHistoryNode aOrigNode = g.getNodeFor(aRevOrig);
        assertEquals(aRevOrig, aOrigNode.getFile());
        assertEquals(IFileHistoryNode.Type.UNCONFIRMED, aOrigNode.getType());
        assertEquals(false, aOrigNode.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, aOrigNode)), aOrigNode.getAncestors());

        final FileHistoryNode xNode = g.getNodeFor(xRevOrig);
        assertEquals(xRevOrig, xNode.getFile());
        assertEquals(IFileHistoryNode.Type.UNCONFIRMED, xNode.getType());
        assertEquals(false, xNode.isCopyTarget());
        assertEquals(Collections.singleton(aOrigNode), xNode.getChildren());

        final FileHistoryNode aDelNode = g.getNodeFor(aRevDel);
        assertEquals(aRevDel, aDelNode.getFile());
        assertEquals(IFileHistoryNode.Type.DELETED, aDelNode.getType());
        assertEquals(true, aDelNode.isCopyTarget());
        assertEquals(Collections.emptySet(), aDelNode.getDescendants());

        final FileHistoryNode yNode = g.getNodeFor(yRev);
        assertEquals(yRev, yNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, yNode.getType());
        assertEquals(true, yNode.isCopyTarget());
        assertEquals(Collections.singleton(aDelNode), yNode.getChildren());
        assertEquals(yNode, aDelNode.getParent());

        final FileHistoryEdge xyEdge = new FileHistoryEdge(g, xNode, yNode, IFileHistoryEdge.Type.COPY,
                new FileDiff(xRevOrig, yRev));
        assertEquals(Collections.singleton(xyEdge), xNode.getDescendants());
        assertEquals(Collections.singleton(xyEdge), yNode.getAncestors());

        final FileHistoryEdge aEdge = new FileHistoryEdge(g, aOrigNode, aDelNode, IFileHistoryEdge.Type.COPY_DELETED,
                new FileDiff(aRevOrig, aRevDel));
        assertEquals(Collections.singleton(aEdge), aOrigNode.getDescendants());
        assertEquals(Collections.singleton(aEdge), aDelNode.getAncestors());
    }

    @Test
    public void testReplacementInCopiedKnownDirectory() {
        final IRepository repo = new TestRepository("123", new File("/some/repo"));
        final FileHistoryGraph g = new TestFileHistoryGraph();

        final IRevisionedFile xRevOrig =
                ChangestructureFactory.createFileInRevision("/trunk/x", new TestRepoRevision(repo, 1L));
        final IRevisionedFile aRevOrig =
                ChangestructureFactory.createFileInRevision("/trunk/x/a", xRevOrig.getRevision());
        final IRevisionedFile bRevOrig =
                ChangestructureFactory.createFileInRevision("/trunk/x/b", xRevOrig.getRevision());

        g.addAdditionOrChange(xRevOrig.getPath(), xRevOrig.getRevision(), Collections.<IRevision> emptySet());
        g.addAdditionOrChange(aRevOrig.getPath(), aRevOrig.getRevision(), Collections.<IRevision> emptySet());
        g.addAdditionOrChange(bRevOrig.getPath(), bRevOrig.getRevision(), Collections.<IRevision> emptySet());

        final IRevisionedFile yRev =
                ChangestructureFactory.createFileInRevision("/trunk/y", new TestRepoRevision(repo, 2L));
        final IRevisionedFile aRevReplaced =
                ChangestructureFactory.createFileInRevision("/trunk/y/a", yRev.getRevision());
        final IRevisionedFile bRevReplaced =
                ChangestructureFactory.createFileInRevision("/trunk/y/b", yRev.getRevision());

        g.addCopy(xRevOrig.getPath(), yRev.getPath(), xRevOrig.getRevision(), yRev.getRevision());
        g.addReplacement(aRevReplaced.getPath(), aRevReplaced.getRevision());
        g.addReplacement(bRevReplaced.getPath(), bRevReplaced.getRevision(),
                bRevOrig.getPath(), bRevOrig.getRevision());

        final FileHistoryNode aOrigNode = g.getNodeFor(aRevOrig);
        assertEquals(aRevOrig, aOrigNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, aOrigNode.getType());
        assertEquals(false, aOrigNode.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, aOrigNode)), aOrigNode.getAncestors());

        final FileHistoryNode bOrigNode = g.getNodeFor(bRevOrig);
        assertEquals(bRevOrig, bOrigNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, bOrigNode.getType());
        assertEquals(false, bOrigNode.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, bOrigNode)), bOrigNode.getAncestors());

        final FileHistoryNode xNode = g.getNodeFor(xRevOrig);
        assertEquals(xRevOrig, xNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, xNode.getType());
        assertEquals(false, xNode.isCopyTarget());
        assertEquals(new HashSet<>(Arrays.asList(aOrigNode, bOrigNode)), xNode.getChildren());
        assertEquals(xNode, aOrigNode.getParent());
        assertEquals(xNode, bOrigNode.getParent());

        final FileHistoryNode aReplacedNode = g.getNodeFor(aRevReplaced);
        assertEquals(aRevReplaced, aReplacedNode.getFile());
        assertEquals(IFileHistoryNode.Type.REPLACED, aReplacedNode.getType());
        assertEquals(false, aReplacedNode.isCopyTarget());
        assertEquals(Collections.emptySet(), aReplacedNode.getDescendants());

        final FileHistoryNode bReplacedNode = g.getNodeFor(bRevReplaced);
        assertEquals(bRevReplaced, bReplacedNode.getFile());
        assertEquals(IFileHistoryNode.Type.REPLACED, bReplacedNode.getType());
        assertEquals(true, bReplacedNode.isCopyTarget());
        assertEquals(Collections.emptySet(), bReplacedNode.getDescendants());

        final FileHistoryNode yNode = g.getNodeFor(yRev);
        assertEquals(yRev, yNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, yNode.getType());
        assertEquals(true, yNode.isCopyTarget());
        assertEquals(new HashSet<>(Arrays.asList(aReplacedNode, bReplacedNode)), yNode.getChildren());
        assertEquals(yNode, aReplacedNode.getParent());
        assertEquals(yNode, bReplacedNode.getParent());

        final FileHistoryEdge xyEdge = new FileHistoryEdge(g, xNode, yNode, IFileHistoryEdge.Type.COPY,
                new FileDiff(xRevOrig, yRev));
        assertEquals(Collections.singleton(xyEdge), xNode.getDescendants());
        assertEquals(Collections.singleton(xyEdge), yNode.getAncestors());

        final FileHistoryEdge aEdge = new FileHistoryEdge(g, aOrigNode, aReplacedNode,
                IFileHistoryEdge.Type.COPY_DELETED, new FileDiff(aRevOrig, aRevReplaced));
        assertEquals(Collections.singleton(aEdge), aOrigNode.getDescendants());
        assertEquals(Collections.singleton(aEdge), aReplacedNode.getAncestors());

        final FileHistoryEdge b1Edge = new FileHistoryEdge(g, bOrigNode, bReplacedNode,
                IFileHistoryEdge.Type.COPY_DELETED, new FileDiff(bRevOrig, bRevReplaced));
        final FileHistoryEdge b2Edge = new FileHistoryEdge(g, bOrigNode, bReplacedNode, IFileHistoryEdge.Type.COPY,
                new FileDiff(bRevOrig, bRevReplaced));
        assertEquals(new HashSet<>(Arrays.asList(b1Edge, b2Edge)), bOrigNode.getDescendants());
        assertEquals(new HashSet<>(Arrays.asList(b1Edge, b2Edge)), bReplacedNode.getAncestors());
    }

    @Test
    public void testReplacementInCopiedUnknownDirectory() {
        final IRepository repo = new TestRepository("123", new File("/some/repo"));
        final FileHistoryGraph g = new TestFileHistoryGraph();

        final IRevisionedFile xRevOrig =
                ChangestructureFactory.createFileInRevision("/trunk/x", new TestRepoRevision(repo, 1L));
        final IRevisionedFile aRevOrig =
                ChangestructureFactory.createFileInRevision("/trunk/x/a", xRevOrig.getRevision());
        final IRevisionedFile bRevOrig =
                ChangestructureFactory.createFileInRevision("/trunk/x/b", xRevOrig.getRevision());

        final IRevisionedFile yRev =
                ChangestructureFactory.createFileInRevision("/trunk/y", new TestRepoRevision(repo, 2L));
        final IRevisionedFile aRevReplaced =
                ChangestructureFactory.createFileInRevision("/trunk/y/a", yRev.getRevision());
        final IRevisionedFile bRevReplaced =
                ChangestructureFactory.createFileInRevision("/trunk/y/b", yRev.getRevision());

        g.addCopy(xRevOrig.getPath(), yRev.getPath(), xRevOrig.getRevision(), yRev.getRevision());
        g.addReplacement(aRevReplaced.getPath(), aRevReplaced.getRevision());
        g.addReplacement(bRevReplaced.getPath(), bRevReplaced.getRevision(),
                bRevOrig.getPath(), bRevOrig.getRevision());

        final FileHistoryNode aOrigNode = g.getNodeFor(aRevOrig);
        assertEquals(aRevOrig, aOrigNode.getFile());
        assertEquals(IFileHistoryNode.Type.UNCONFIRMED, aOrigNode.getType());
        assertEquals(false, aOrigNode.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, aOrigNode)), aOrigNode.getAncestors());

        final FileHistoryNode bOrigNode = g.getNodeFor(bRevOrig);
        assertEquals(bRevOrig, bOrigNode.getFile());
        assertEquals(IFileHistoryNode.Type.UNCONFIRMED, bOrigNode.getType());
        assertEquals(false, bOrigNode.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, bOrigNode)), bOrigNode.getAncestors());

        final FileHistoryNode xNode = g.getNodeFor(xRevOrig);
        assertEquals(xRevOrig, xNode.getFile());
        assertEquals(IFileHistoryNode.Type.UNCONFIRMED, xNode.getType());
        assertEquals(false, xNode.isCopyTarget());
        assertEquals(new HashSet<>(Arrays.asList(aOrigNode, bOrigNode)), xNode.getChildren());
        assertEquals(xNode, aOrigNode.getParent());
        assertEquals(xNode, bOrigNode.getParent());

        final FileHistoryNode aReplacedNode = g.getNodeFor(aRevReplaced);
        assertEquals(aRevReplaced, aReplacedNode.getFile());
        assertEquals(IFileHistoryNode.Type.REPLACED, aReplacedNode.getType());
        assertEquals(false, aReplacedNode.isCopyTarget());
        assertEquals(Collections.emptySet(), aReplacedNode.getDescendants());

        final FileHistoryNode bReplacedNode = g.getNodeFor(bRevReplaced);
        assertEquals(bRevReplaced, bReplacedNode.getFile());
        assertEquals(IFileHistoryNode.Type.REPLACED, bReplacedNode.getType());
        assertEquals(true, bReplacedNode.isCopyTarget());
        assertEquals(Collections.emptySet(), bReplacedNode.getDescendants());

        final FileHistoryNode yNode = g.getNodeFor(yRev);
        assertEquals(yRev, yNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, yNode.getType());
        assertEquals(true, yNode.isCopyTarget());
        assertEquals(new HashSet<>(Arrays.asList(aReplacedNode, bReplacedNode)), yNode.getChildren());
        assertEquals(yNode, aReplacedNode.getParent());
        assertEquals(yNode, bReplacedNode.getParent());

        final FileHistoryEdge xyEdge = new FileHistoryEdge(g, xNode, yNode, IFileHistoryEdge.Type.COPY,
                new FileDiff(xRevOrig, yRev));
        assertEquals(Collections.singleton(xyEdge), xNode.getDescendants());
        assertEquals(Collections.singleton(xyEdge), yNode.getAncestors());

        final FileHistoryEdge aEdge = new FileHistoryEdge(g, aOrigNode, aReplacedNode,
                IFileHistoryEdge.Type.COPY_DELETED, new FileDiff(aRevOrig, aRevReplaced));
        assertEquals(Collections.singleton(aEdge), aOrigNode.getDescendants());
        assertEquals(Collections.singleton(aEdge), aReplacedNode.getAncestors());

        final FileHistoryEdge b1Edge = new FileHistoryEdge(g, bOrigNode, bReplacedNode,
                IFileHistoryEdge.Type.COPY_DELETED, new FileDiff(bRevOrig, bRevReplaced));
        final FileHistoryEdge b2Edge = new FileHistoryEdge(g, bOrigNode, bReplacedNode, IFileHistoryEdge.Type.COPY,
                new FileDiff(bRevOrig, bRevReplaced));
        assertEquals(new HashSet<>(Arrays.asList(b1Edge, b2Edge)), bOrigNode.getDescendants());
        assertEquals(new HashSet<>(Arrays.asList(b1Edge, b2Edge)), bReplacedNode.getAncestors());
    }

    @Test
    public void testChangeInCopiedKnownDirectory() {
        final IRepository repo = new TestRepository("123", new File("/some/repo"));
        final FileHistoryGraph g = new TestFileHistoryGraph();

        final IRevisionedFile xRevOrig =
                ChangestructureFactory.createFileInRevision("/trunk/x", new TestRepoRevision(repo, 1L));
        final IRevisionedFile aRevOrig =
                ChangestructureFactory.createFileInRevision("/trunk/x/a", xRevOrig.getRevision());

        g.addAdditionOrChange(xRevOrig.getPath(), xRevOrig.getRevision(), Collections.<IRevision> emptySet());
        g.addAdditionOrChange(aRevOrig.getPath(), aRevOrig.getRevision(), Collections.<IRevision> emptySet());

        final IRevisionedFile yRev =
                ChangestructureFactory.createFileInRevision("/trunk/y", new TestRepoRevision(repo, 2L));
        final IRevisionedFile aRevChanged =
                ChangestructureFactory.createFileInRevision("/trunk/y/a", yRev.getRevision());

        g.addCopy(xRevOrig.getPath(), yRev.getPath(), xRevOrig.getRevision(), yRev.getRevision());
        g.addAdditionOrChange(aRevChanged.getPath(), aRevChanged.getRevision(),
                Collections.singleton(aRevOrig.getRevision()));

        final FileHistoryNode aOrigNode = g.getNodeFor(aRevOrig);
        assertEquals(aRevOrig, aOrigNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, aOrigNode.getType());
        assertEquals(false, aOrigNode.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, aOrigNode)), aOrigNode.getAncestors());

        final FileHistoryNode xNode = g.getNodeFor(xRevOrig);
        assertEquals(xRevOrig, xNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, xNode.getType());
        assertEquals(false, xNode.isCopyTarget());
        assertEquals(Collections.singleton(aOrigNode), xNode.getChildren());

        final FileHistoryNode aChangedNode = g.getNodeFor(aRevChanged);
        assertEquals(aRevChanged, aChangedNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, aChangedNode.getType());
        assertEquals(true, aChangedNode.isCopyTarget());
        assertEquals(Collections.emptySet(), aChangedNode.getDescendants());

        final FileHistoryNode yNode = g.getNodeFor(yRev);
        assertEquals(yRev, yNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, yNode.getType());
        assertEquals(true, yNode.isCopyTarget());
        assertEquals(Collections.singleton(aChangedNode), yNode.getChildren());
        assertEquals(yNode, aChangedNode.getParent());

        final FileHistoryEdge xyEdge = new FileHistoryEdge(g, xNode, yNode, IFileHistoryEdge.Type.COPY,
                new FileDiff(xRevOrig, yRev));
        assertEquals(Collections.singleton(xyEdge), xNode.getDescendants());
        assertEquals(Collections.singleton(xyEdge), yNode.getAncestors());

        final FileHistoryEdge aEdge = new FileHistoryEdge(g, aOrigNode, aChangedNode, IFileHistoryEdge.Type.COPY,
                new FileDiff(aRevOrig, aRevChanged));
        assertEquals(Collections.singleton(aEdge), aOrigNode.getDescendants());
        assertEquals(Collections.singleton(aEdge), aChangedNode.getAncestors());
    }

    @Test
    public void testChangeInCopiedUnknownDirectory() {
        final IRepository repo = new TestRepository("123", new File("/some/repo"));
        final FileHistoryGraph g = new TestFileHistoryGraph();

        final IRevisionedFile xRevOrig =
                ChangestructureFactory.createFileInRevision("/trunk/x", new TestRepoRevision(repo, 1L));
        final IRevisionedFile aRevOrig =
                ChangestructureFactory.createFileInRevision("/trunk/x/a", xRevOrig.getRevision());

        final IRevisionedFile yRev =
                ChangestructureFactory.createFileInRevision("/trunk/y", new TestRepoRevision(repo, 2L));
        final IRevisionedFile aRevChanged =
                ChangestructureFactory.createFileInRevision("/trunk/y/a", yRev.getRevision());

        g.addCopy(xRevOrig.getPath(), yRev.getPath(), xRevOrig.getRevision(), yRev.getRevision());
        g.addAdditionOrChange(aRevChanged.getPath(), aRevChanged.getRevision(),
                Collections.singleton(aRevOrig.getRevision()));

        final FileHistoryNode aOrigNode = g.getNodeFor(aRevOrig);
        assertEquals(aRevOrig, aOrigNode.getFile());
        assertEquals(IFileHistoryNode.Type.UNCONFIRMED, aOrigNode.getType());
        assertEquals(false, aOrigNode.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, aOrigNode)), aOrigNode.getAncestors());

        final FileHistoryNode xNode = g.getNodeFor(xRevOrig);
        assertEquals(xRevOrig, xNode.getFile());
        assertEquals(IFileHistoryNode.Type.UNCONFIRMED, xNode.getType());
        assertEquals(false, xNode.isCopyTarget());
        assertEquals(Collections.singleton(aOrigNode), xNode.getChildren());

        final FileHistoryNode aChangedNode = g.getNodeFor(aRevChanged);
        assertEquals(aRevChanged, aChangedNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, aChangedNode.getType());
        assertEquals(true, aChangedNode.isCopyTarget());
        assertEquals(Collections.emptySet(), aChangedNode.getDescendants());

        final FileHistoryNode yNode = g.getNodeFor(yRev);
        assertEquals(yRev, yNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, yNode.getType());
        assertEquals(true, yNode.isCopyTarget());
        assertEquals(Collections.singleton(aChangedNode), yNode.getChildren());
        assertEquals(yNode, aChangedNode.getParent());

        final FileHistoryEdge xyEdge = new FileHistoryEdge(g, xNode, yNode, IFileHistoryEdge.Type.COPY,
                new FileDiff(xRevOrig, yRev));
        assertEquals(Collections.singleton(xyEdge), xNode.getDescendants());
        assertEquals(Collections.singleton(xyEdge), yNode.getAncestors());

        final FileHistoryEdge aEdge = new FileHistoryEdge(g, aOrigNode, aChangedNode, IFileHistoryEdge.Type.COPY,
                new FileDiff(aRevOrig, aRevChanged));
        assertEquals(Collections.singleton(aEdge), aOrigNode.getDescendants());
        assertEquals(Collections.singleton(aEdge), aChangedNode.getAncestors());
    }

    @Test
    public void testChangeInCopiedUnknownDirectory2() {
        final IRepository repo = new TestRepository("123", new File("/some/repo"));
        final FileHistoryGraph g = new TestFileHistoryGraph();

        final IRevisionedFile xRevOrig =
                ChangestructureFactory.createFileInRevision("/trunk/x", new TestRepoRevision(repo, 1L));
        final IRevisionedFile aRevOrig =
                ChangestructureFactory.createFileInRevision("/trunk/x/a", xRevOrig.getRevision());

        final IRevisionedFile yRev =
                ChangestructureFactory.createFileInRevision("/trunk/y", new TestRepoRevision(repo, 2L));
        final IRevisionedFile aRevPrevChange =
                ChangestructureFactory.createFileInRevision("/trunk/y/a", yRev.getRevision());

        final IRevisionedFile yRevChanged =
                ChangestructureFactory.createFileInRevision("/trunk/y", new TestRepoRevision(repo, 3L));
        final IRevisionedFile aRevChanged =
                ChangestructureFactory.createFileInRevision("/trunk/y/a", yRevChanged.getRevision());

        g.addCopy(xRevOrig.getPath(), yRev.getPath(), xRevOrig.getRevision(), yRev.getRevision());
        g.addAdditionOrChange(aRevChanged.getPath(), aRevChanged.getRevision(),
                Collections.singleton(aRevPrevChange.getRevision()));

        final FileHistoryNode aOrigNode = g.getNodeFor(aRevOrig);
        assertEquals(aRevOrig, aOrigNode.getFile());
        assertEquals(IFileHistoryNode.Type.UNCONFIRMED, aOrigNode.getType());
        assertEquals(false, aOrigNode.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, aOrigNode)), aOrigNode.getAncestors());

        final FileHistoryNode xNode = g.getNodeFor(xRevOrig);
        assertEquals(xRevOrig, xNode.getFile());
        assertEquals(IFileHistoryNode.Type.UNCONFIRMED, xNode.getType());
        assertEquals(false, xNode.isCopyTarget());
        assertEquals(Collections.singleton(aOrigNode), xNode.getChildren());

        final FileHistoryNode aPrevChangeNode = g.getNodeFor(aRevPrevChange);
        assertEquals(aRevPrevChange, aPrevChangeNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, aPrevChangeNode.getType());
        assertEquals(true, aPrevChangeNode.isCopyTarget());

        final FileHistoryNode aChangedNode = g.getNodeFor(aRevChanged);
        assertEquals(aRevChanged, aChangedNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, aChangedNode.getType());
        assertEquals(false, aChangedNode.isCopyTarget());
        assertEquals(Collections.emptySet(), aChangedNode.getDescendants());

        final FileHistoryNode yNode = g.getNodeFor(yRev);
        assertEquals(yRev, yNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, yNode.getType());
        assertEquals(true, yNode.isCopyTarget());
        assertEquals(Collections.singleton(aPrevChangeNode), yNode.getChildren());
        assertEquals(yNode, aPrevChangeNode.getParent());

        final FileHistoryNode yChangedNode = g.getNodeFor(yRevChanged);
        assertEquals(yRevChanged, yChangedNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, yChangedNode.getType());
        assertEquals(false, yChangedNode.isCopyTarget());
        assertEquals(Collections.singleton(aChangedNode), yChangedNode.getChildren());
        assertEquals(yChangedNode, aChangedNode.getParent());

        final FileHistoryEdge xyEdge = new FileHistoryEdge(g, xNode, yNode, IFileHistoryEdge.Type.COPY,
                new FileDiff(xRevOrig, yRev));
        assertEquals(Collections.singleton(xyEdge), xNode.getDescendants());
        assertEquals(Collections.singleton(xyEdge), yNode.getAncestors());

        final FileHistoryEdge aCopyEdge = new FileHistoryEdge(g, aOrigNode, aPrevChangeNode,
                IFileHistoryEdge.Type.COPY, new FileDiff(aRevOrig, aRevPrevChange));
        assertEquals(Collections.singleton(aCopyEdge), aOrigNode.getDescendants());
        assertEquals(Collections.singleton(aCopyEdge), aPrevChangeNode.getAncestors());

        final FileHistoryEdge aEdge = new FileHistoryEdge(g, aPrevChangeNode, aChangedNode,
                IFileHistoryEdge.Type.NORMAL, new FileDiff(aRevPrevChange, aRevChanged));
        assertEquals(Collections.singleton(aEdge), aPrevChangeNode.getDescendants());
        assertEquals(Collections.singleton(aEdge), aChangedNode.getAncestors());
    }

    @Test
    public void testCopyInCopiedKnownDirectory() {
        final IRepository repo = new TestRepository("123", new File("/some/repo"));
        final FileHistoryGraph g = new TestFileHistoryGraph();

        final IRevisionedFile xRevOrig =
                ChangestructureFactory.createFileInRevision("/trunk/x", new TestRepoRevision(repo, 1L));
        final IRevisionedFile bRevOrig =
                ChangestructureFactory.createFileInRevision("/trunk/b", new TestRepoRevision(repo, 1L));
        final IRevisionedFile aRevOrig =
                ChangestructureFactory.createFileInRevision("/trunk/x/a", xRevOrig.getRevision());

        g.addAdditionOrChange(xRevOrig.getPath(), xRevOrig.getRevision(), Collections.<IRevision> emptySet());
        g.addAdditionOrChange(aRevOrig.getPath(), aRevOrig.getRevision(), Collections.<IRevision> emptySet());
        g.addAdditionOrChange(bRevOrig.getPath(), bRevOrig.getRevision(), Collections.<IRevision> emptySet());

        final IRevisionedFile yRev =
                ChangestructureFactory.createFileInRevision("/trunk/y", new TestRepoRevision(repo, 2L));
        final IRevisionedFile aRev =
                ChangestructureFactory.createFileInRevision("/trunk/y/a", yRev.getRevision());
        final IRevisionedFile bRev =
                ChangestructureFactory.createFileInRevision("/trunk/y/b", yRev.getRevision());

        g.addCopy(xRevOrig.getPath(), yRev.getPath(), xRevOrig.getRevision(), yRev.getRevision());
        g.addCopy(bRevOrig.getPath(), bRev.getPath(), bRevOrig.getRevision(), bRev.getRevision());

        final FileHistoryNode aOrigNode = g.getNodeFor(aRevOrig);
        assertEquals(aRevOrig, aOrigNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, aOrigNode.getType());
        assertEquals(false, aOrigNode.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, aOrigNode)), aOrigNode.getAncestors());

        final FileHistoryNode bOrigNode = g.getNodeFor(bRevOrig);
        assertEquals(bRevOrig, bOrigNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, bOrigNode.getType());
        assertEquals(false, bOrigNode.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, bOrigNode)), bOrigNode.getAncestors());

        final FileHistoryNode xNode = g.getNodeFor(xRevOrig);
        assertEquals(xRevOrig, xNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, xNode.getType());
        assertEquals(false, xNode.isCopyTarget());
        assertEquals(Collections.singleton(aOrigNode), xNode.getChildren());
        assertEquals(xNode, aOrigNode.getParent());

        final FileHistoryNode aNode = g.getNodeFor(aRev);
        assertEquals(aRev, aNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, aNode.getType());
        assertEquals(true, aNode.isCopyTarget());
        assertEquals(Collections.emptySet(), aNode.getDescendants());

        final FileHistoryNode bNode = g.getNodeFor(bRev);
        assertEquals(bRev, bNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, bNode.getType());
        assertEquals(true, bNode.isCopyTarget());
        assertEquals(Collections.emptySet(), bNode.getDescendants());

        final FileHistoryNode yNode = g.getNodeFor(yRev);
        assertEquals(yRev, yNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, yNode.getType());
        assertEquals(true, yNode.isCopyTarget());
        assertEquals(new HashSet<>(Arrays.asList(aNode, bNode)), yNode.getChildren());
        assertEquals(yNode, aNode.getParent());
        assertEquals(yNode, bNode.getParent());

        final FileHistoryEdge xyEdge = new FileHistoryEdge(g, xNode, yNode, IFileHistoryEdge.Type.COPY,
                new FileDiff(xRevOrig, yRev));
        assertEquals(Collections.singleton(xyEdge), xNode.getDescendants());
        assertEquals(Collections.singleton(xyEdge), yNode.getAncestors());

        final FileHistoryEdge aEdge = new FileHistoryEdge(g, aOrigNode, aNode, IFileHistoryEdge.Type.COPY,
                new FileDiff(aRevOrig, aRev));
        assertEquals(Collections.singleton(aEdge), aOrigNode.getDescendants());
        assertEquals(Collections.singleton(aEdge), aNode.getAncestors());

        final FileHistoryEdge bEdge = new FileHistoryEdge(g, bOrigNode, bNode, IFileHistoryEdge.Type.COPY,
                new FileDiff(bRevOrig, bRev));
        assertEquals(Collections.singleton(bEdge), bOrigNode.getDescendants());
        assertEquals(Collections.singleton(bEdge), bNode.getAncestors());
    }

    @Test
    public void testCopyInCopiedUnknownDirectory() {
        final IRepository repo = new TestRepository("123", new File("/some/repo"));
        final FileHistoryGraph g = new TestFileHistoryGraph();

        final IRevisionedFile xRevOrig =
                ChangestructureFactory.createFileInRevision("/trunk/x", new TestRepoRevision(repo, 1L));
        final IRevisionedFile bRevOrig =
                ChangestructureFactory.createFileInRevision("/trunk/b", new TestRepoRevision(repo, 1L));

        final IRevisionedFile yRev =
                ChangestructureFactory.createFileInRevision("/trunk/y", new TestRepoRevision(repo, 2L));
        final IRevisionedFile bRev =
                ChangestructureFactory.createFileInRevision("/trunk/y/b", yRev.getRevision());

        g.addCopy(xRevOrig.getPath(), yRev.getPath(), xRevOrig.getRevision(), yRev.getRevision());
        g.addCopy(bRevOrig.getPath(), bRev.getPath(), bRevOrig.getRevision(), bRev.getRevision());

        final FileHistoryNode bOrigNode = g.getNodeFor(bRevOrig);
        assertEquals(bRevOrig, bOrigNode.getFile());
        assertEquals(IFileHistoryNode.Type.UNCONFIRMED, bOrigNode.getType());
        assertEquals(false, bOrigNode.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, bOrigNode)), bOrigNode.getAncestors());

        final FileHistoryNode xNode = g.getNodeFor(xRevOrig);
        assertEquals(xRevOrig, xNode.getFile());
        assertEquals(IFileHistoryNode.Type.UNCONFIRMED, xNode.getType());
        assertEquals(false, xNode.isCopyTarget());
        assertEquals(Collections.emptySet(), xNode.getChildren());

        final FileHistoryNode bNode = g.getNodeFor(bRev);
        assertEquals(bRev, bNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, bNode.getType());
        assertEquals(true, bNode.isCopyTarget());
        assertEquals(Collections.emptySet(), bNode.getDescendants());

        final FileHistoryNode yNode = g.getNodeFor(yRev);
        assertEquals(yRev, yNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, yNode.getType());
        assertEquals(true, yNode.isCopyTarget());
        assertEquals(Collections.singleton(bNode), yNode.getChildren());
        assertEquals(yNode, bNode.getParent());

        final FileHistoryEdge xyEdge = new FileHistoryEdge(g, xNode, yNode, IFileHistoryEdge.Type.COPY,
                new FileDiff(xRevOrig, yRev));
        assertEquals(Collections.singleton(xyEdge), xNode.getDescendants());
        assertEquals(Collections.singleton(xyEdge), yNode.getAncestors());

        final FileHistoryEdge bEdge = new FileHistoryEdge(g, bOrigNode, bNode, IFileHistoryEdge.Type.COPY,
                new FileDiff(bRevOrig, bRev));
        assertEquals(Collections.singleton(bEdge), bOrigNode.getDescendants());
        assertEquals(Collections.singleton(bEdge), bNode.getAncestors());
    }

    @Test
    public void testCopyInCopiedUnknownDirectory2() {
        final IRepository repo = new TestRepository("123", new File("/some/repo"));
        final FileHistoryGraph g = new TestFileHistoryGraph();

        final IRevisionedFile xRevOrig =
                ChangestructureFactory.createFileInRevision("/trunk/x", new TestRepoRevision(repo, 1L));
        final IRevisionedFile bRevOrig =
                ChangestructureFactory.createFileInRevision("/trunk/b", new TestRepoRevision(repo, 1L));

        final IRevisionedFile yRev =
                ChangestructureFactory.createFileInRevision("/trunk/y", new TestRepoRevision(repo, 2L));

        final IRevisionedFile yRev2 =
                ChangestructureFactory.createFileInRevision("/trunk/y", new TestRepoRevision(repo, 3L));
        final IRevisionedFile bRev =
                ChangestructureFactory.createFileInRevision("/trunk/y/b", new TestRepoRevision(repo, 3L));

        g.addCopy(xRevOrig.getPath(), yRev.getPath(), xRevOrig.getRevision(), yRev.getRevision());
        g.addCopy(bRevOrig.getPath(), bRev.getPath(), bRevOrig.getRevision(), bRev.getRevision());

        final FileHistoryNode bOrigNode = g.getNodeFor(bRevOrig);
        assertEquals(bRevOrig, bOrigNode.getFile());
        assertEquals(IFileHistoryNode.Type.UNCONFIRMED, bOrigNode.getType());
        assertEquals(false, bOrigNode.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, bOrigNode)), bOrigNode.getAncestors());

        final FileHistoryNode xNode = g.getNodeFor(xRevOrig);
        assertEquals(xRevOrig, xNode.getFile());
        assertEquals(IFileHistoryNode.Type.UNCONFIRMED, xNode.getType());
        assertEquals(false, xNode.isCopyTarget());
        assertEquals(Collections.emptySet(), xNode.getChildren());

        final FileHistoryNode bNode = g.getNodeFor(bRev);
        assertEquals(bRev, bNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, bNode.getType());
        assertEquals(true, bNode.isCopyTarget());
        assertEquals(Collections.emptySet(), bNode.getDescendants());

        final FileHistoryNode yNode = g.getNodeFor(yRev);
        assertEquals(yRev, yNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, yNode.getType());
        assertEquals(true, yNode.isCopyTarget());
        assertEquals(Collections.emptySet(), yNode.getChildren());

        final FileHistoryNode yNode2 = g.getNodeFor(yRev2);
        assertEquals(yRev2, yNode2.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, yNode2.getType());
        assertEquals(false, yNode2.isCopyTarget());
        assertEquals(Collections.singleton(bNode), yNode2.getChildren());
        assertEquals(yNode2, bNode.getParent());

        final FileHistoryEdge xyEdge = new FileHistoryEdge(g, xNode, yNode, IFileHistoryEdge.Type.COPY,
                new FileDiff(xRevOrig, yRev));
        assertEquals(Collections.singleton(xyEdge), xNode.getDescendants());
        assertEquals(Collections.singleton(xyEdge), yNode.getAncestors());

        final FileHistoryEdge bEdge = new FileHistoryEdge(g, bOrigNode, bNode, IFileHistoryEdge.Type.COPY,
                new FileDiff(bRevOrig, bRev));
        assertEquals(Collections.singleton(bEdge), bOrigNode.getDescendants());
        assertEquals(Collections.singleton(bEdge), bNode.getAncestors());
    }

    @Test
    public void testCopyAndChangeInCopiedKnownDirectory() {
        final IRepository repo = new TestRepository("123", new File("/some/repo"));
        final FileHistoryGraph g = new TestFileHistoryGraph();

        final IRevisionedFile xRevOrig =
                ChangestructureFactory.createFileInRevision("/trunk/x", new TestRepoRevision(repo, 1L));
        final IRevisionedFile bRevOrig =
                ChangestructureFactory.createFileInRevision("/trunk/b", new TestRepoRevision(repo, 1L));
        final IRevisionedFile aRevOrig =
                ChangestructureFactory.createFileInRevision("/trunk/x/a", xRevOrig.getRevision());

        g.addAdditionOrChange(xRevOrig.getPath(), xRevOrig.getRevision(), Collections.<IRevision> emptySet());
        g.addAdditionOrChange(aRevOrig.getPath(), aRevOrig.getRevision(), Collections.<IRevision> emptySet());
        g.addAdditionOrChange(bRevOrig.getPath(), bRevOrig.getRevision(), Collections.<IRevision> emptySet());

        final IRevisionedFile yRev =
                ChangestructureFactory.createFileInRevision("/trunk/y", new TestRepoRevision(repo, 2L));
        final IRevisionedFile aRev =
                ChangestructureFactory.createFileInRevision("/trunk/y/a", yRev.getRevision());
        final IRevisionedFile bRev =
                ChangestructureFactory.createFileInRevision("/trunk/y/b", yRev.getRevision());

        g.addCopy(xRevOrig.getPath(), yRev.getPath(), xRevOrig.getRevision(), yRev.getRevision());
        g.addCopy(bRevOrig.getPath(), bRev.getPath(), bRevOrig.getRevision(), bRev.getRevision());
        g.addAdditionOrChange(bRev.getPath(), bRev.getRevision(), Collections.singleton(bRevOrig.getRevision()));

        final FileHistoryNode aOrigNode = g.getNodeFor(aRevOrig);
        assertEquals(aRevOrig, aOrigNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, aOrigNode.getType());
        assertEquals(false, aOrigNode.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, aOrigNode)), aOrigNode.getAncestors());

        final FileHistoryNode bOrigNode = g.getNodeFor(bRevOrig);
        assertEquals(bRevOrig, bOrigNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, bOrigNode.getType());
        assertEquals(false, bOrigNode.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, bOrigNode)), bOrigNode.getAncestors());

        final FileHistoryNode xNode = g.getNodeFor(xRevOrig);
        assertEquals(xRevOrig, xNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, xNode.getType());
        assertEquals(false, xNode.isCopyTarget());
        assertEquals(Collections.singleton(aOrigNode), xNode.getChildren());
        assertEquals(xNode, aOrigNode.getParent());

        final FileHistoryNode aNode = g.getNodeFor(aRev);
        assertEquals(aRev, aNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, aNode.getType());
        assertEquals(true, aNode.isCopyTarget());
        assertEquals(Collections.emptySet(), aNode.getDescendants());

        final FileHistoryNode bNode = g.getNodeFor(bRev);
        assertEquals(bRev, bNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, bNode.getType());
        assertEquals(true, bNode.isCopyTarget());
        assertEquals(Collections.emptySet(), bNode.getDescendants());

        final FileHistoryNode yNode = g.getNodeFor(yRev);
        assertEquals(yRev, yNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, yNode.getType());
        assertEquals(true, yNode.isCopyTarget());
        assertEquals(new HashSet<>(Arrays.asList(aNode, bNode)), yNode.getChildren());
        assertEquals(yNode, aNode.getParent());
        assertEquals(yNode, bNode.getParent());

        final FileHistoryEdge xyEdge = new FileHistoryEdge(g, xNode, yNode, IFileHistoryEdge.Type.COPY,
                new FileDiff(xRevOrig, yRev));
        assertEquals(Collections.singleton(xyEdge), xNode.getDescendants());
        assertEquals(Collections.singleton(xyEdge), yNode.getAncestors());

        final FileHistoryEdge aEdge = new FileHistoryEdge(g, aOrigNode, aNode, IFileHistoryEdge.Type.COPY,
                new FileDiff(aRevOrig, aRev));
        assertEquals(Collections.singleton(aEdge), aOrigNode.getDescendants());
        assertEquals(Collections.singleton(aEdge), aNode.getAncestors());

        final FileHistoryEdge bEdge = new FileHistoryEdge(g, bOrigNode, bNode, IFileHistoryEdge.Type.COPY,
                new FileDiff(bRevOrig, bRev));
        assertEquals(Collections.singleton(bEdge), bOrigNode.getDescendants());
        assertEquals(Collections.singleton(bEdge), bNode.getAncestors());
    }

    @Test
    public void testCopyAndChangeInCopiedUnknownDirectory() {
        final IRepository repo = new TestRepository("123", new File("/some/repo"));
        final FileHistoryGraph g = new TestFileHistoryGraph();

        final IRevisionedFile xRevOrig =
                ChangestructureFactory.createFileInRevision("/trunk/x", new TestRepoRevision(repo, 1L));
        final IRevisionedFile bRevOrig =
                ChangestructureFactory.createFileInRevision("/trunk/b", new TestRepoRevision(repo, 1L));

        final IRevisionedFile yRev =
                ChangestructureFactory.createFileInRevision("/trunk/y", new TestRepoRevision(repo, 2L));
        final IRevisionedFile bRev =
                ChangestructureFactory.createFileInRevision("/trunk/y/b", yRev.getRevision());

        g.addCopy(xRevOrig.getPath(), yRev.getPath(), xRevOrig.getRevision(), yRev.getRevision());
        g.addCopy(bRevOrig.getPath(), bRev.getPath(), bRevOrig.getRevision(), bRev.getRevision());
        g.addAdditionOrChange(bRev.getPath(), bRev.getRevision(), Collections.singleton(bRevOrig.getRevision()));

        final FileHistoryNode bOrigNode = g.getNodeFor(bRevOrig);
        assertEquals(bRevOrig, bOrigNode.getFile());
        assertEquals(IFileHistoryNode.Type.UNCONFIRMED, bOrigNode.getType());
        assertEquals(false, bOrigNode.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, bOrigNode)), bOrigNode.getAncestors());

        final FileHistoryNode xNode = g.getNodeFor(xRevOrig);
        assertEquals(xRevOrig, xNode.getFile());
        assertEquals(IFileHistoryNode.Type.UNCONFIRMED, xNode.getType());
        assertEquals(false, xNode.isCopyTarget());
        assertEquals(Collections.emptySet(), xNode.getChildren());

        final FileHistoryNode bNode = g.getNodeFor(bRev);
        assertEquals(bRev, bNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, bNode.getType());
        assertEquals(true, bNode.isCopyTarget());
        assertEquals(Collections.emptySet(), bNode.getDescendants());

        final FileHistoryNode yNode = g.getNodeFor(yRev);
        assertEquals(yRev, yNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, yNode.getType());
        assertEquals(true, yNode.isCopyTarget());
        assertEquals(Collections.singleton(bNode), yNode.getChildren());
        assertEquals(yNode, bNode.getParent());

        final FileHistoryEdge xyEdge = new FileHistoryEdge(g, xNode, yNode, IFileHistoryEdge.Type.COPY,
                new FileDiff(xRevOrig, yRev));
        assertEquals(Collections.singleton(xyEdge), xNode.getDescendants());
        assertEquals(Collections.singleton(xyEdge), yNode.getAncestors());

        final FileHistoryEdge bEdge = new FileHistoryEdge(g, bOrigNode, bNode, IFileHistoryEdge.Type.COPY,
                new FileDiff(bRevOrig, bRev));
        assertEquals(Collections.singleton(bEdge), bOrigNode.getDescendants());
        assertEquals(Collections.singleton(bEdge), bNode.getAncestors());
    }

    @Test
    public void testMovementOfDirectory() {
        final IRepository repo = new TestRepository("123", new File("/some/repo"));
        final FileHistoryGraph g = new TestFileHistoryGraph();

        final IRevisionedFile aRevOrig =
                ChangestructureFactory.createFileInRevision("/trunk/x/a", new TestRepoRevision(repo, 1L));

        g.addAdditionOrChange(aRevOrig.getPath(), aRevOrig.getRevision(), Collections.<IRevision> emptySet());

        final IRevisionedFile xRev =
                ChangestructureFactory.createFileInRevision("/trunk/x", new TestRepoRevision(repo, 1L));
        final IRevisionedFile yRev =
                ChangestructureFactory.createFileInRevision("/trunk/y", new TestRepoRevision(repo, 2L));
        final IRevisionedFile xRevDel =
                ChangestructureFactory.createFileInRevision(xRev.getPath(), yRev.getRevision());
        final IRevisionedFile aRevDel =
                ChangestructureFactory.createFileInRevision(aRevOrig.getPath(), yRev.getRevision());
        final IRevisionedFile aRevCopy =
                ChangestructureFactory.createFileInRevision("/trunk/y/a", yRev.getRevision());

        g.addCopy(xRev.getPath(), yRev.getPath(), xRev.getRevision(), yRev.getRevision());
        g.addDeletion(xRevDel.getPath(), xRevDel.getRevision());

        final FileHistoryNode aOrigNode = g.getNodeFor(aRevOrig);
        assertEquals(aRevOrig, aOrigNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, aOrigNode.getType());
        assertEquals(false, aOrigNode.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, aOrigNode)), aOrigNode.getAncestors());

        final FileHistoryNode aCopyNode = g.getNodeFor(aRevCopy);
        assertEquals(aRevCopy, aCopyNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, aCopyNode.getType());
        assertEquals(true, aCopyNode.isCopyTarget());
        assertEquals(Collections.emptySet(), aCopyNode.getDescendants());

        final FileHistoryNode aDelNode = g.getNodeFor(aRevDel);
        assertEquals(aRevDel, aDelNode.getFile());
        assertEquals(IFileHistoryNode.Type.DELETED, aDelNode.getType());
        assertEquals(false, aDelNode.isCopyTarget());
        assertEquals(Collections.emptySet(), aDelNode.getDescendants());

        final FileHistoryNode xDelNode = g.getNodeFor(xRevDel);
        assertEquals(xRevDel, xDelNode.getFile());
        assertEquals(IFileHistoryNode.Type.DELETED, xDelNode.getType());
        assertEquals(false, xDelNode.isCopyTarget());
        assertEquals(Collections.emptySet(), xDelNode.getDescendants());

        final FileHistoryEdge aEdgeCopy = new FileHistoryEdge(g, aOrigNode, aCopyNode, IFileHistoryEdge.Type.COPY,
                new FileDiff(aRevOrig, aRevCopy));
        final FileHistoryEdge aEdgeDel = new FileHistoryEdge(g, aOrigNode, aDelNode, IFileHistoryEdge.Type.NORMAL,
                new FileDiff(aRevOrig, aRevDel));
        assertEquals(new HashSet<>(Arrays.asList(aEdgeCopy, aEdgeDel)), aOrigNode.getDescendants());
        assertEquals(Collections.singleton(aEdgeCopy), aCopyNode.getAncestors());
        assertEquals(Collections.singleton(aEdgeDel), aDelNode.getAncestors());

        final FileHistoryNode xNode = g.getNodeFor(xRev);
        assertEquals(xRev, xNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, xNode.getType());
        assertEquals(false, xNode.isCopyTarget());

        final FileHistoryNode yNode = g.getNodeFor(yRev);
        assertEquals(yRev, yNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, yNode.getType());
        assertEquals(true, yNode.isCopyTarget());

        assertEquals(Collections.singleton(aOrigNode), xNode.getChildren());
        assertEquals(xNode, aOrigNode.getParent());

        assertEquals(Collections.singleton(aCopyNode), yNode.getChildren());
        assertEquals(yNode, aCopyNode.getParent());

        final FileHistoryEdge xyEdgeCopy = new FileHistoryEdge(g, xNode, yNode, IFileHistoryEdge.Type.COPY,
                new FileDiff(xRev, yRev));
        final FileHistoryEdge xyEdgeDel = new FileHistoryEdge(g, xNode, xDelNode, IFileHistoryEdge.Type.NORMAL,
                new FileDiff(xRev, xRevDel));
        assertEquals(new HashSet<>(Arrays.asList(xyEdgeCopy, xyEdgeDel)), xNode.getDescendants());
        assertEquals(Collections.singleton(xyEdgeCopy), yNode.getAncestors());
        assertEquals(Collections.singleton(xyEdgeDel), xDelNode.getAncestors());
    }

    @Test
    public void testMovementOfDirectory2() {
        final IRepository repo = new TestRepository("123", new File("/some/repo"));
        final FileHistoryGraph g = new TestFileHistoryGraph();

        final IRevisionedFile aRevOrig =
                ChangestructureFactory.createFileInRevision("/trunk/x/a", new TestRepoRevision(repo, 1L));

        g.addAdditionOrChange(aRevOrig.getPath(), aRevOrig.getRevision(), Collections.<IRevision> emptySet());

        final IRevisionedFile xRev =
                ChangestructureFactory.createFileInRevision("/trunk/x", new TestRepoRevision(repo, 1L));
        final IRevisionedFile yRev =
                ChangestructureFactory.createFileInRevision("/trunk/y", new TestRepoRevision(repo, 2L));
        final IRevisionedFile xRevDel =
                ChangestructureFactory.createFileInRevision(xRev.getPath(), yRev.getRevision());
        final IRevisionedFile aRevDel =
                ChangestructureFactory.createFileInRevision(aRevOrig.getPath(), yRev.getRevision());
        final IRevisionedFile aRevCopy =
                ChangestructureFactory.createFileInRevision("/trunk/y/a", yRev.getRevision());

        g.addDeletion(xRevDel.getPath(), xRevDel.getRevision());
        g.addCopy(xRev.getPath(), yRev.getPath(), xRev.getRevision(), yRev.getRevision());

        final FileHistoryNode aOrigNode = g.getNodeFor(aRevOrig);
        assertEquals(aRevOrig, aOrigNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, aOrigNode.getType());
        assertEquals(false, aOrigNode.isCopyTarget());
        assertEquals(Collections.singleton(createAlphaNode(repo, g, aOrigNode)), aOrigNode.getAncestors());

        final FileHistoryNode aCopyNode = g.getNodeFor(aRevCopy);
        assertEquals(aRevCopy, aCopyNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, aCopyNode.getType());
        assertEquals(true, aCopyNode.isCopyTarget());
        assertEquals(Collections.emptySet(), aCopyNode.getDescendants());

        final FileHistoryNode aDelNode = g.getNodeFor(aRevDel);
        assertEquals(aRevDel, aDelNode.getFile());
        assertEquals(IFileHistoryNode.Type.DELETED, aDelNode.getType());
        assertEquals(false, aDelNode.isCopyTarget());
        assertEquals(Collections.emptySet(), aDelNode.getDescendants());

        final FileHistoryNode xDelNode = g.getNodeFor(xRevDel);
        assertEquals(xRevDel, xDelNode.getFile());
        assertEquals(IFileHistoryNode.Type.DELETED, xDelNode.getType());
        assertEquals(false, xDelNode.isCopyTarget());
        assertEquals(Collections.emptySet(), xDelNode.getDescendants());

        final FileHistoryEdge aEdgeCopy = new FileHistoryEdge(g, aOrigNode, aCopyNode, IFileHistoryEdge.Type.COPY,
                new FileDiff(aRevOrig, aRevCopy));
        final FileHistoryEdge aEdgeDel = new FileHistoryEdge(g, aOrigNode, aDelNode, IFileHistoryEdge.Type.NORMAL,
                new FileDiff(aRevOrig, aRevDel));
        assertEquals(new HashSet<>(Arrays.asList(aEdgeCopy, aEdgeDel)), aOrigNode.getDescendants());
        assertEquals(Collections.singleton(aEdgeCopy), aCopyNode.getAncestors());
        assertEquals(Collections.singleton(aEdgeDel), aDelNode.getAncestors());

        final FileHistoryNode xNode = g.getNodeFor(xRev);
        assertEquals(xRev, xNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, xNode.getType());
        assertEquals(false, xNode.isCopyTarget());

        final FileHistoryNode yNode = g.getNodeFor(yRev);
        assertEquals(yRev, yNode.getFile());
        assertEquals(IFileHistoryNode.Type.NORMAL, yNode.getType());
        assertEquals(true, yNode.isCopyTarget());

        assertEquals(Collections.singleton(aOrigNode), xNode.getChildren());
        assertEquals(xNode, aOrigNode.getParent());

        assertEquals(Collections.singleton(aCopyNode), yNode.getChildren());
        assertEquals(yNode, aCopyNode.getParent());

        final FileHistoryEdge xyEdgeCopy = new FileHistoryEdge(g, xNode, yNode, IFileHistoryEdge.Type.COPY,
                new FileDiff(xRev, yRev));
        final FileHistoryEdge xyEdgeDel = new FileHistoryEdge(g, xNode, xDelNode, IFileHistoryEdge.Type.NORMAL,
                new FileDiff(xRev, xRevDel));
        assertEquals(new HashSet<>(Arrays.asList(xyEdgeCopy, xyEdgeDel)), xNode.getDescendants());
        assertEquals(Collections.singleton(xyEdgeCopy), yNode.getAncestors());
        assertEquals(Collections.singleton(xyEdgeDel), xDelNode.getAncestors());
    }

    @Test
    public void testContains() {
        final IRepository repo = new TestRepository("123", new File("/some/repo"));
        final FileHistoryGraph g = new TestFileHistoryGraph();

        final IRevisionedFile trunkRev =
                ChangestructureFactory.createFileInRevision("/trunk", new TestRepoRevision(repo, 1L));
        g.addAdditionOrChange(trunkRev.getPath(), trunkRev.getRevision(), Collections.<IRevision> emptySet());

        final IRevisionedFile aRev =
                ChangestructureFactory.createFileInRevision("/trunk/a", new TestRepoRevision(repo, 2L));
        g.addAdditionOrChange(aRev.getPath(), aRev.getRevision(), Collections.<IRevision> emptySet());

        final IRevisionedFile bRev =
                ChangestructureFactory.createFileInRevision("/trunk/x/b", new TestRepoRevision(repo, 3L));
        g.addAdditionOrChange(bRev.getPath(), bRev.getRevision(), Collections.<IRevision> emptySet());

        assertTrue(g.contains("/trunk", repo));
        assertTrue(g.contains("/trunk/a", repo));
        assertTrue(g.contains("/trunk/x", repo));
        assertTrue(g.contains("/trunk/x/b", repo));
        assertFalse(g.contains("/trunk/b", repo));
        assertFalse(g.contains("/trunk/x/a", repo));
    }

    @Test
    public void testToString() {
        assertEquals("{}", new TestFileHistoryGraph().toString());
    }
}
