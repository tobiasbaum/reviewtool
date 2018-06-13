package de.setsoftware.reviewtool.changesources.svn;

import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import de.setsoftware.reviewtool.diffalgorithms.DiffAlgorithmFactory;
import de.setsoftware.reviewtool.model.api.IFileHistoryNode;
import de.setsoftware.reviewtool.model.api.ILocalRevision;
import de.setsoftware.reviewtool.model.api.IRepoRevision;
import de.setsoftware.reviewtool.model.api.IRevision;
import de.setsoftware.reviewtool.model.api.IRevisionVisitor;
import de.setsoftware.reviewtool.model.api.IRevisionedFile;
import de.setsoftware.reviewtool.model.api.IUnknownRevision;
import de.setsoftware.reviewtool.model.changestructure.ChangestructureFactory;
import de.setsoftware.reviewtool.model.changestructure.FileHistoryGraph;
import de.setsoftware.reviewtool.model.changestructure.ProxyableFileHistoryNode;

/**
 *  A graph of files. Tracks renames, copies and deletion, so that the history of a file forms a tree.
 */
final class SvnFileHistoryGraph extends FileHistoryGraph {

    private static final long serialVersionUID = 2706724778357716541L;

    /**
     * Constructor.
     */
    SvnFileHistoryGraph() {
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
                return -1L;
            }

        });
    }

    /**
     * Processes a single SVN repository revision by translating it into a file history graph operation.
     *
     * @param revision The revision to process,
     */
    void processRevision(final SvnRevision revision) {
        for (final Entry<String, CachedLogEntryPath> e : revision.getChangedPaths().entrySet()) {
            final String path = e.getKey();

            final CachedLogEntryPath pathInfo = e.getValue();
            final String copyPath = pathInfo.getCopyPath();
            if (pathInfo.isDeleted()) {
                this.addDeletion(path, revision.toRevision());
            } else if (pathInfo.isReplaced()) {
                if (copyPath != null) {
                    this.addReplacement(
                            path,
                            revision.toRevision(),
                            copyPath,
                            ChangestructureFactory.createRepoRevision(pathInfo.getCopyRevision(),
                                    revision.getRepository()));
                } else {
                    this.addReplacement(path, revision.toRevision());
                }
            } else if (pathInfo.isNew()) {
                if (copyPath != null) {
                    this.addCopy(
                            copyPath,
                            path,
                            ChangestructureFactory.createRepoRevision(
                                    pathInfo.getCopyRevision(),
                                    revision.getRepository()),
                            revision.toRevision());
                } else {
                    this.addAddition(path, revision.toRevision());
                }
            } else {
                this.addChange(
                        path,
                        revision.toRevision(),
                        Collections.<IRevision>singleton(ChangestructureFactory.createRepoRevision(
                                e.getValue().getAncestorRevision(),
                                revision.getRepository())));
            }
        }
    }
}
