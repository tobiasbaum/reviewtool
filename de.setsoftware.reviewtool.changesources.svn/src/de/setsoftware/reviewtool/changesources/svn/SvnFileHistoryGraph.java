package de.setsoftware.reviewtool.changesources.svn;

import java.util.Collections;
import java.util.List;

import de.setsoftware.reviewtool.model.changestructure.FileHistoryGraph;
import de.setsoftware.reviewtool.model.changestructure.FileHistoryNode;
import de.setsoftware.reviewtool.model.changestructure.FileInRevision;
import de.setsoftware.reviewtool.model.changestructure.LocalRevision;
import de.setsoftware.reviewtool.model.changestructure.RepoRevision;
import de.setsoftware.reviewtool.model.changestructure.Repository;
import de.setsoftware.reviewtool.model.changestructure.Revision;
import de.setsoftware.reviewtool.model.changestructure.RevisionVisitor;
import de.setsoftware.reviewtool.model.changestructure.UnknownRevision;

/**
 *  A graph of files. Tracks renames, copies and deletion, so that the history of a file forms a tree.
 */
final class SvnFileHistoryGraph extends FileHistoryGraph {

    /**
     * Adds the information that the file with the given path was added or changed at the commit of the given revision.
     */
    public void addAddition(
            final String path,
            final Revision revision,
            final Repository repo) {
        this.addAdditionOrChange(path, revision, Collections.<Revision>emptySet(), repo);
    }

    /**
     * Adds the information that the file with the given path was added or changed at the commit of the given revision.
     */
    public void addChange(
            final String path,
            final Revision prevRevision,
            final Revision revision,
            final Repository repo) {
        this.addAdditionOrChange(path, revision, Collections.<Revision>singleton(prevRevision), repo);
    }

    /**
     * Adds the information that the file with the given path was deleted with the commit of the given revision.
     * If an ancestor node exists, the deletion node of type {@link NonExistingFileHistoryNode} is linked to it,
     * possibly creating an intermediate {@link ExistingFileHistoryNode} just before the deletion. This supports
     * finding the last revision of a file before being deleted.
     */
    public void addDeletion(
            final String path,
            final Revision prevRevision,
            final Revision revision,
            final Repository repo) {
        this.addDeletion(path, revision, Collections.<Revision>singleton(prevRevision), repo);
    }

    @Override
    public FileHistoryNode findAncestorFor(final FileInRevision file) {
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
    private static long getRevision(final FileInRevision revision) {
        return revision.getRevision().accept(new RevisionVisitor<Long>() {

            @Override
            public Long handleLocalRevision(final LocalRevision revision) {
                return Long.MAX_VALUE;
            }

            @Override
            public Long handleRepoRevision(final RepoRevision revision) {
                return (Long) revision.getId();
            }

            @Override
            public Long handleUnknownRevision(final UnknownRevision revision) {
                return 0L;
            }

        });
    }
}
