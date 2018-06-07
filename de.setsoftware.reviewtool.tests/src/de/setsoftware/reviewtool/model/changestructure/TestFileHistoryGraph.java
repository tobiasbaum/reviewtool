package de.setsoftware.reviewtool.model.changestructure;

import java.util.List;

import de.setsoftware.reviewtool.diffalgorithms.DiffAlgorithmFactory;
import de.setsoftware.reviewtool.model.api.IFileHistoryGraph;
import de.setsoftware.reviewtool.model.api.IFileHistoryNode;
import de.setsoftware.reviewtool.model.api.ILocalRevision;
import de.setsoftware.reviewtool.model.api.IRepoRevision;
import de.setsoftware.reviewtool.model.api.IRevisionVisitor;
import de.setsoftware.reviewtool.model.api.IRevisionedFile;
import de.setsoftware.reviewtool.model.api.IUnknownRevision;

/**
 * Implements {@link IFileHistoryGraph} for this test case. (Code borrowed from the SVN change source code.)
 */
final class TestFileHistoryGraph extends FileHistoryGraph {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor.
     */
    public TestFileHistoryGraph() {
        super(DiffAlgorithmFactory.createDefault());
    }

    @Override
    public ProxyableFileHistoryNode findAncestorFor(final IRevisionedFile file) {
        final List<ProxyableFileHistoryNode> nodesForKey = this.lookupFile(file);
        final long targetRevision = getRevision(file);
        long nearestRevision = Long.MIN_VALUE;
        ProxyableFileHistoryNode nearestNode = null;
        for (final ProxyableFileHistoryNode node : nodesForKey) {
            final long nodeRevision = getRevision(node.getFile());
            if (nodeRevision < targetRevision && nodeRevision > nearestRevision) {
                nearestNode = node;
                nearestRevision = nodeRevision;
            }
        }
        if (nearestNode != null) {
            return nearestNode.getType().equals(IFileHistoryNode.Type.DELETED) ? null : nearestNode;
        } else {
            return null;
        }
    }

    /**
     * Returns the underlying revision number.
     *
     * @param revision The revision.
     * @return The revision number.
     */
    private static long getRevision(final IRevisionedFile revision) {
        return revision.getRevision().accept(new IRevisionVisitor<Long>() {

            @Override
            public Long handleLocalRevision(final ILocalRevision revision) {
                return Long.MAX_VALUE;
            }

            @Override
            public Long handleRepoRevision(final IRepoRevision revision) {
                return (Long) revision.getId();
            }

            @Override
            public Long handleUnknownRevision(final IUnknownRevision revision) {
                return 0L;
            }

        });
    }

}
