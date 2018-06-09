package de.setsoftware.reviewtool.model.api;

import java.util.List;

/**
 *  A graph of files. Tracks renames, copies and deletion, so that the history of a file forms a tree.
 */
public interface IFileHistoryGraph {

    /**
     * Returns true if passed path is known to this {@link IFileHistoryGraph}.
     * @param path The path to check.
     * @param repo The repository.
     * @return <code>true</code> if the path is known, else <code>false</code>
     */
    public abstract boolean contains(String path, IRepository repo);

    /**
     * Returns the {@link IFileHistoryNode} for the given {@link IRevisionedFile <code>file</code>},
     * or <code>null</code> if <code>file</code> is not part of the history graph.
     *
     * @param file The {@link IRevisionedFile} to search for.
     * @return The corresponding {@link IFileHistoryNode} or <code>null</code> if not found.
     */
    public abstract IFileHistoryNode getNodeFor(IRevisionedFile file);

    /**
     * Returns the latest known versions of the given file. If all versions were deleted, the last known versions
     * before deletion are returned. If the file is unknown, a list with the file itself is
     * returned.
     * <p/>
     * The revisions returned are topologically sorted according to their dependencies.
     */
    public abstract List<? extends IRevisionedFile> getLatestFiles(IRevisionedFile file);

    /**
     * Returns the algorithm used for computing differences between file revisions.
     */
    public abstract IDiffAlgorithm getDiffAlgorithm();
}
