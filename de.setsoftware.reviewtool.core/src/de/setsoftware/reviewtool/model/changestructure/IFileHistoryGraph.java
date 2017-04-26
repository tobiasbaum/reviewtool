package de.setsoftware.reviewtool.model.changestructure;

import java.util.List;

/**
 *  A graph of files. Tracks renames, copies and deletion, so that the history of a file forms a tree.
 */
public interface IFileHistoryGraph {

    /**
     * Returns the {@link IFileHistoryNode} for the given {@link FileInRevision <code>file</code>},
     * or <code>null</code> if <code>file</code> is not part of the history graph.
     *
     * @param file The {@link FileInRevision} to search for.
     * @return The corresponding {@link IFileHistoryNode} or <code>null</code> if not found.
     */
    public abstract IFileHistoryNode getNodeFor(FileInRevision file);

    /**
     * Returns the latest known versions of the given file. If all versions were deleted, the last known versions
     * before deletion are returned. If the file is unknown, a list with the file itself is
     * returned.
     * <p/>
     * The revisions returned are topologically sorted according to their dependencies.
     */
    public abstract List<FileInRevision> getLatestFiles(FileInRevision file);

}
