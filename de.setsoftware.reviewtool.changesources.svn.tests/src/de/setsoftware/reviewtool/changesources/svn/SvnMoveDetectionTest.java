package de.setsoftware.reviewtool.changesources.svn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Date;
import java.util.SortedMap;
import java.util.TreeMap;

import org.junit.Test;
import org.tmatesoft.svn.core.SVNLogEntryPath;

import de.setsoftware.reviewtool.diffalgorithms.DiffAlgorithmFactory;
import de.setsoftware.reviewtool.model.api.IFileHistoryNode;
import de.setsoftware.reviewtool.model.changestructure.ChangestructureFactory;
import de.setsoftware.reviewtool.model.changestructure.FileHistoryGraph;

public class SvnMoveDetectionTest {

    @Test
    public void testSimpleMoveToLaterPath() {
        final SortedMap<String, SvnChangeItem> paths = new TreeMap<>();
        paths.put("/a", new SvnChangeItem("/a", 1L, null, -1L, SVNLogEntryPath.TYPE_DELETED, 'F'));
        paths.put("/b", new SvnChangeItem("/b", 1L, "/a", 1L, SVNLogEntryPath.TYPE_ADDED, 'F'));
        paths.put("/c", new SvnChangeItem("/c", 1L, null, -1L, SVNLogEntryPath.TYPE_ADDED, 'F'));
        final FileHistoryGraph graph = new FileHistoryGraph(DiffAlgorithmFactory.createDefault());
        new SvnCommit(StubRepo.INSTANCE, 2L, "", "", new Date(), paths).integrateInto(graph);

        final IFileHistoryNode aNode = graph.getNodeFor(ChangestructureFactory.createFileInRevision("/a",
                ChangestructureFactory.createRepoRevision(new SvnCommitId(2L), StubRepo.INSTANCE)));
        assertNotNull(aNode);
        assertEquals(IFileHistoryNode.Type.DELETED, aNode.getType());
        assertFalse(aNode.isCopyTarget());

        final IFileHistoryNode bNode = graph.getNodeFor(ChangestructureFactory.createFileInRevision("/b",
                ChangestructureFactory.createRepoRevision(new SvnCommitId(2L), StubRepo.INSTANCE)));
        assertNotNull(bNode);
        assertEquals(IFileHistoryNode.Type.CHANGED, bNode.getType());
        assertTrue(bNode.isCopyTarget());

        final IFileHistoryNode cNode = graph.getNodeFor(ChangestructureFactory.createFileInRevision("/c",
                ChangestructureFactory.createRepoRevision(new SvnCommitId(2L), StubRepo.INSTANCE)));
        assertNotNull(cNode);
        assertEquals(IFileHistoryNode.Type.ADDED, cNode.getType());
        assertFalse(cNode.isCopyTarget());

        assertEquals(Collections.singleton(bNode), aNode.getMoveTargets());
        assertEquals(Collections.singleton(aNode), bNode.getMoveSources());
    }

    @Test
    public void testSimpleMoveToEarlierPath() {
        final SortedMap<String, SvnChangeItem> paths = new TreeMap<>();
        paths.put("/a", new SvnChangeItem("/a", 1L, "/b", 1L, SVNLogEntryPath.TYPE_ADDED, 'F'));
        paths.put("/b", new SvnChangeItem("/b", 1L, null, -1L, SVNLogEntryPath.TYPE_DELETED, 'F'));
        paths.put("/c", new SvnChangeItem("/c", 1L, null, -1L, SVNLogEntryPath.TYPE_ADDED, 'F'));
        final FileHistoryGraph graph = new FileHistoryGraph(DiffAlgorithmFactory.createDefault());
        new SvnCommit(StubRepo.INSTANCE, 2L, "", "", new Date(), paths).integrateInto(graph);

        final IFileHistoryNode aNode = graph.getNodeFor(ChangestructureFactory.createFileInRevision("/a",
                ChangestructureFactory.createRepoRevision(new SvnCommitId(2L), StubRepo.INSTANCE)));
        assertNotNull(aNode);
        assertEquals(IFileHistoryNode.Type.CHANGED, aNode.getType());
        assertTrue(aNode.isCopyTarget());

        final IFileHistoryNode bNode = graph.getNodeFor(ChangestructureFactory.createFileInRevision("/b",
                ChangestructureFactory.createRepoRevision(new SvnCommitId(2L), StubRepo.INSTANCE)));
        assertNotNull(bNode);
        assertEquals(IFileHistoryNode.Type.DELETED, bNode.getType());
        assertFalse(bNode.isCopyTarget());

        final IFileHistoryNode cNode = graph.getNodeFor(ChangestructureFactory.createFileInRevision("/c",
                ChangestructureFactory.createRepoRevision(new SvnCommitId(2L), StubRepo.INSTANCE)));
        assertNotNull(cNode);
        assertEquals(IFileHistoryNode.Type.ADDED, cNode.getType());
        assertFalse(cNode.isCopyTarget());

        assertEquals(Collections.singleton(aNode), bNode.getMoveTargets());
        assertEquals(Collections.singleton(bNode), aNode.getMoveSources());
    }

    @Test
    public void testNestedMoveToLaterPath() {
        final FileHistoryGraph graph = new FileHistoryGraph(DiffAlgorithmFactory.createDefault());
        final SortedMap<String, SvnChangeItem> paths = new TreeMap<>();
        paths.put("/c", new SvnChangeItem("/c", 0L, null, -1L, SVNLogEntryPath.TYPE_ADDED, 'F'));
        new SvnCommit(StubRepo.INSTANCE, 1L, "", "", new Date(), paths).integrateInto(graph);

        paths.clear();
        paths.put("/a", new SvnChangeItem("/a", 1L, null, -1L, SVNLogEntryPath.TYPE_DELETED, 'F'));
        paths.put("/b", new SvnChangeItem("/b", 1L, "/a", 1L, SVNLogEntryPath.TYPE_ADDED, 'F'));
        paths.put("/b/c", new SvnChangeItem("/b/c", 1L, "/c", 1L, SVNLogEntryPath.TYPE_ADDED, 'F'));
        paths.put("/c", new SvnChangeItem("/c", 1L, null, -1L, SVNLogEntryPath.TYPE_DELETED, 'F'));
        new SvnCommit(StubRepo.INSTANCE, 2L, "", "", new Date(), paths).integrateInto(graph);

        final IFileHistoryNode aNode = graph.getNodeFor(ChangestructureFactory.createFileInRevision("/a",
                ChangestructureFactory.createRepoRevision(new SvnCommitId(2L), StubRepo.INSTANCE)));
        assertNotNull(aNode);
        assertEquals(IFileHistoryNode.Type.DELETED, aNode.getType());
        assertFalse(aNode.isCopyTarget());

        final IFileHistoryNode bNode = graph.getNodeFor(ChangestructureFactory.createFileInRevision("/b",
                ChangestructureFactory.createRepoRevision(new SvnCommitId(2L), StubRepo.INSTANCE)));
        assertNotNull(bNode);
        assertEquals(IFileHistoryNode.Type.CHANGED, bNode.getType());
        assertTrue(bNode.isCopyTarget());

        final IFileHistoryNode cOldNode = graph.getNodeFor(ChangestructureFactory.createFileInRevision("/c",
                ChangestructureFactory.createRepoRevision(new SvnCommitId(2L), StubRepo.INSTANCE)));
        assertNotNull(cOldNode);
        assertEquals(IFileHistoryNode.Type.DELETED, cOldNode.getType());
        assertFalse(cOldNode.isCopyTarget());

        final IFileHistoryNode cNode = graph.getNodeFor(ChangestructureFactory.createFileInRevision("/b/c",
                ChangestructureFactory.createRepoRevision(new SvnCommitId(2L), StubRepo.INSTANCE)));
        assertNotNull(cNode);
        assertEquals(IFileHistoryNode.Type.CHANGED, cNode.getType());
        assertTrue(cNode.isCopyTarget());

        assertEquals(Collections.singleton(bNode), aNode.getMoveTargets());
        assertEquals(Collections.singleton(aNode), bNode.getMoveSources());

        assertEquals(Collections.singleton(cNode), cOldNode.getMoveTargets());
        assertEquals(Collections.singleton(cOldNode), cNode.getMoveSources());
    }

    @Test
    public void testNestedMoveToEarlierPath() {
        final FileHistoryGraph graph = new FileHistoryGraph(DiffAlgorithmFactory.createDefault());
        final SortedMap<String, SvnChangeItem> paths = new TreeMap<>();
        paths.put("/c", new SvnChangeItem("/c", 0L, null, -1L, SVNLogEntryPath.TYPE_ADDED, 'F'));
        new SvnCommit(StubRepo.INSTANCE, 1L, "", "", new Date(), paths).integrateInto(graph);

        paths.clear();
        paths.put("/a", new SvnChangeItem("/a", 1L, "/b", 1L, SVNLogEntryPath.TYPE_ADDED, 'F'));
        paths.put("/a/c", new SvnChangeItem("/a/c", 1L, "/c", 1L, SVNLogEntryPath.TYPE_ADDED, 'F'));
        paths.put("/b", new SvnChangeItem("/b", 1L, null, -1L, SVNLogEntryPath.TYPE_DELETED, 'F'));
        paths.put("/c", new SvnChangeItem("/c", 1L, null, -1L, SVNLogEntryPath.TYPE_DELETED, 'F'));
        new SvnCommit(StubRepo.INSTANCE, 2L, "", "", new Date(), paths).integrateInto(graph);

        final IFileHistoryNode aNode = graph.getNodeFor(ChangestructureFactory.createFileInRevision("/a",
                ChangestructureFactory.createRepoRevision(new SvnCommitId(2L), StubRepo.INSTANCE)));
        assertNotNull(aNode);
        assertEquals(IFileHistoryNode.Type.CHANGED, aNode.getType());
        assertTrue(aNode.isCopyTarget());

        final IFileHistoryNode bNode = graph.getNodeFor(ChangestructureFactory.createFileInRevision("/b",
                ChangestructureFactory.createRepoRevision(new SvnCommitId(2L), StubRepo.INSTANCE)));
        assertNotNull(bNode);
        assertEquals(IFileHistoryNode.Type.DELETED, bNode.getType());
        assertFalse(bNode.isCopyTarget());

        final IFileHistoryNode cOldNode = graph.getNodeFor(ChangestructureFactory.createFileInRevision("/c",
                ChangestructureFactory.createRepoRevision(new SvnCommitId(2L), StubRepo.INSTANCE)));
        assertNotNull(cOldNode);
        assertEquals(IFileHistoryNode.Type.DELETED, cOldNode.getType());
        assertFalse(cOldNode.isCopyTarget());

        final IFileHistoryNode cNode = graph.getNodeFor(ChangestructureFactory.createFileInRevision("/a/c",
                ChangestructureFactory.createRepoRevision(new SvnCommitId(2L), StubRepo.INSTANCE)));
        assertNotNull(cNode);
        assertEquals(IFileHistoryNode.Type.CHANGED, cNode.getType());
        assertTrue(cNode.isCopyTarget());

        assertEquals(Collections.singleton(aNode), bNode.getMoveTargets());
        assertEquals(Collections.singleton(bNode), aNode.getMoveSources());

        assertEquals(Collections.singleton(cNode), cOldNode.getMoveTargets());
        assertEquals(Collections.singleton(cOldNode), cNode.getMoveSources());
    }
}
