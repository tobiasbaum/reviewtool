package de.setsoftware.reviewtool.changesources.svn;

import java.util.Collections;
import java.util.List;

import de.setsoftware.reviewtool.model.api.ILocalRevision;
import de.setsoftware.reviewtool.model.api.IRepoRevision;
import de.setsoftware.reviewtool.model.api.IRevision;
import de.setsoftware.reviewtool.model.api.IRevisionVisitor;
import de.setsoftware.reviewtool.model.api.IRevisionedFile;
import de.setsoftware.reviewtool.model.api.IUnknownRevision;
import de.setsoftware.reviewtool.model.changestructure.FileHistoryGraph;
import de.setsoftware.reviewtool.model.changestructure.FileHistoryNode;

/**
 *  A graph of files. Tracks renames, copies and deletion, so that the history of a file forms a tree.
 */
final class SvnFileHistoryGraph extends FileHistoryGraph {

    /**
     * Adds the information that the file with the given path was added or changed at the commit of the given revision.
     */
    public void addAddition(
            final String path,
            final IRevision revision) {
        this.addAdditionOrChange(path, revision, Collections.<IRevision>emptySet());
    }

    /**
     * Adds the information that the file with the given path was added or changed at the commit of the given revision.
     */
    public void addChange(
            final String path,
            final IRevision prevRevision,
            final IRevision revision) {
        this.addAdditionOrChange(path, revision, Collections.<IRevision>singleton(prevRevision));
    }

    /**
     * Adds the information that the file with the given path was deleted with the commit of the given revision.
     * If an ancestor node exists, the deletion node of type {@link NonExistingFileHistoryNode} is linked to it,
     * possibly creating an intermediate {@link ExistingFileHistoryNode} just before the deletion. This supports
     * finding the last revision of a file before being deleted.
     */
    public void addDeletion(
            final String path,
            final IRevision prevRevision,
            final IRevision revision) {
        this.addDeletion(path, revision, Collections.<IRevision>singleton(prevRevision));
    }

    @Override
    public FileHistoryNode findAncestorFor(final IRevisionedFile file) {
        final List<FileHistoryNode> nodesForKey = this.lookupFile(file);
        final long targetRevision = getRevision(file);
        long nearestRevision = Long.MIN_VALUE;
        FileHistoryNode nearestNode = null;
        for (final FileHistoryNode node : nodesForKey) {
            final long nodeRevision = getRevision(node.getFile());
            if (nodeRevision < targetRevision && nodeRevision > nearestRevision) {
                nearestNode = node;
                nearestRevision = nodeRevision;
            }
        }
        if (nearestNode != null) {
            return nearestNode.isDeleted() ? null : nearestNode;
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
