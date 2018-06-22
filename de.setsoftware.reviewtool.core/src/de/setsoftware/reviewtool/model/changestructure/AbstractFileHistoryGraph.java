package de.setsoftware.reviewtool.model.changestructure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import de.setsoftware.reviewtool.base.PartialOrderAlgorithms;
import de.setsoftware.reviewtool.model.api.IFileHistoryEdge;
import de.setsoftware.reviewtool.model.api.IFileHistoryGraph;
import de.setsoftware.reviewtool.model.api.IFileHistoryNode;
import de.setsoftware.reviewtool.model.api.IRevisionVisitor;
import de.setsoftware.reviewtool.model.api.IFileHistoryNode.Type;
import de.setsoftware.reviewtool.model.api.ILocalRevision;
import de.setsoftware.reviewtool.model.api.IRepoRevision;
import de.setsoftware.reviewtool.model.api.IRevisionedFile;
import de.setsoftware.reviewtool.model.api.IUnknownRevision;

/**
 * Contains behaviour common to all {@link IFileHistoryGraph} implementations.
 */
public abstract class AbstractFileHistoryGraph implements IFileHistoryGraph {

    @Override
    public final List<IRevisionedFile> getLatestFiles(
            final IRevisionedFile file,
            final boolean ignoreNonLocalCopies) {

        Set<IFileHistoryNode> nodes = this.getLatestFilesHelper(file, ignoreNonLocalCopies, false);
        if (nodes.isEmpty()) {
            nodes = this.getLatestFilesHelper(file, ignoreNonLocalCopies, true);
        }

        if (nodes.isEmpty()) {
            return Collections.singletonList(file);
        } else {
            final List<IRevisionedFile> revs = new ArrayList<>();
            for (final IFileHistoryNode node : nodes) {
                revs.add(node.getFile());
            }
            return PartialOrderAlgorithms.topoSort(revs);
        }
    }

    /**
     * Returns the latest known nodes of the given file. If the file is unknown, a list with the file itself is
     * returned.
     *
     * @param returnDeletions If <code>true</code> and all versions were deleted, the last known nodes
     *      before deletion are returned. If <code>false</code>, no nodes are returned in this case.
     */
    private Set<IFileHistoryNode> getLatestFilesHelper(
            final IRevisionedFile file,
            final boolean ignoreNonLocalCopies,
            final boolean returnDeletions) {

        final IFileHistoryNode node = this.getNodeFor(file);
        if (node == null) {
            // unknown file
            return Collections.emptySet();
        } else {
            // either node for file or descendant node shares history with passed file, follow it
            return this.getLatestFilesHelper(node, ignoreNonLocalCopies, returnDeletions);
        }
    }

    /**
     * Returns the latest known successor nodes of the given node. Branching (e.g. because of copy/rename operations)
     * is handled properly.
     *
     * @param returnDeletions If <code>true</code> and all versions were deleted, the last known nodes
     *      before deletion are returned. If <code>false</code>, no nodes are returned in this case.
     */
    private Set<IFileHistoryNode> getLatestFilesHelper(
            final IFileHistoryNode node,
            final boolean ignoreNonLocalCopies,
            final boolean returnDeletions) {

        // deletion nodes are never returned
        if (!node.getType().equals(Type.DELETED)) {
            if (node.getDescendants().isEmpty()) {
                return Collections.singleton(node);
            } else {
                // is this node the last known one given its path?
                final Set<IFileHistoryNode> result = new LinkedHashSet<>();
                boolean samePathFound = false;
                for (final IFileHistoryEdge descendantEdge : node.getDescendants()) {
                    final IFileHistoryNode descendant = descendantEdge.getDescendant();
                    if (node.getFile().getPath().equals(descendant.getFile().getPath())) {
                        samePathFound = true;
                        result.addAll(this.getLatestFilesHelper(descendant, ignoreNonLocalCopies, returnDeletions));
                    } else if (!ignoreNonLocalCopies) {
                        result.addAll(this.getLatestFilesHelper(descendant, false, returnDeletions));
                    } else {
                        descendant.getFile().getRevision().accept(new IRevisionVisitor<Void>() {

                            @Override
                            public Void handleLocalRevision(final ILocalRevision revision) {
                                result.addAll(AbstractFileHistoryGraph.this.getLatestFilesHelper(
                                        descendant,
                                        true,
                                        returnDeletions));
                                return null;
                            }

                            @Override
                            public Void handleRepoRevision(final IRepoRevision<?> revision) {
                                return null;
                            }

                            @Override
                            public Void handleUnknownRevision(final IUnknownRevision revision) {
                                return null;
                            }
                        });
                    }
                }
                if (!samePathFound || (returnDeletions && result.isEmpty())) {
                    // either this node is the last known one existing for its path, or this node is the last node
                    // before deletion and we have been advised to return such nodes
                    result.add(node);
                }
                return result;
            }
        } else {
            return Collections.emptySet();
        }
    }
}
