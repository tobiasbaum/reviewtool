package de.setsoftware.reviewtool.model.api;

import java.util.List;
import java.util.Set;

/**
 *  A graph of files. Tracks renames, copies and deletion, so that the history of a file forms a tree.
 */
public interface IFileHistoryGraph {

    /**
     * Returns the set of paths known by this file history graph.
     */
    public abstract Set<String> getPaths();

    /**
     * Returns the {@link IFileHistoryNode} for the given {@link IRevisionedFile <code>file</code>},
     * or <code>null</code> if <code>file</code> is not part of the history graph.
     *
     * @param file The {@link IRevisionedFile} to search for.
     * @return The corresponding {@link IFileHistoryNode} or <code>null</code> if not found.
     */
    public abstract IFileHistoryNode getNodeFor(IRevisionedFile file);

    /**
     * Returns the nearest ancestors for passed {@link IRevisionedFile} having the same path, or an empty set
     * if no suitable nodes exist. To be suitable, the ancestor node must not be deleted.
     */
    public abstract Set<? extends IFileHistoryNode> findAncestorsFor(IRevisionedFile file);

    /**
     * Returns the latest known versions of the given file. If all versions were deleted, the last known versions
     * before deletion are returned. If the file is unknown, a list with the file itself is
     * returned.
     * <p/>
     * The revisions returned are topologically sorted according to their dependencies.
     *
     * @param file The {@link IRevisionedFile} to start with.
     * @param ignoreNonLocalCopies If {@code true}, non-local copies are ignored.
     */
    public abstract List<IRevisionedFile> getLatestFiles(IRevisionedFile file, boolean ignoreNonLocalCopies);

    /**
     * Returns all non-{@link IFileHistoryNode.Type#ADDED added} nodes that do not have any ancestors but an alpha node.
     */
    public abstract Set<IFileHistoryNode> getIncompleteFlowStarts();

    /**
     * Returns the algorithm used for computing differences between file revisions.
     */
    public abstract IDiffAlgorithm getDiffAlgorithm();
}
