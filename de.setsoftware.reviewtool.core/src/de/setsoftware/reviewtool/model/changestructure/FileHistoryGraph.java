package de.setsoftware.reviewtool.model.changestructure;

import java.util.Set;

/**
 *  A graph of files. Tracks renames, copies and deletion, so that the history of a file forms a tree.
 */
public interface FileHistoryGraph {

    /**
     * Returns the {@link FileHistoryNode} for the given {@link FileInRevision <code>file</code>},
     * or <code>null</code> if <code>file</code> is not part of the history graph.
     *
     * @param file The {@link FileInRevision} to search for.
     * @return The corresponding {@link FileHistoryNode} or <code>null</code> if not found.
     */
    public abstract FileHistoryNode getNodeFor(FileInRevision file);

    /**
     * Returns the latest known versions of the given file. If all versions were deleted, the last known versions
     * before deletion are returned. If the file is unknown, a list with the file itself is
     * returned.
     */
    public abstract Set<FileInRevision> getLatestFiles(FileInRevision file);

}
