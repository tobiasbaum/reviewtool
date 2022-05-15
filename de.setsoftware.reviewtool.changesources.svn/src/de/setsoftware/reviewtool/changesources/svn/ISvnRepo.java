package de.setsoftware.reviewtool.changesources.svn;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.tmatesoft.svn.core.ISVNLogEntryHandler;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;

import de.setsoftware.reviewtool.model.api.IMutableFileHistoryGraph;
import de.setsoftware.reviewtool.model.api.IRepoRevision;
import de.setsoftware.reviewtool.model.api.IRepository;

/**
 * Interface to a remote Subversion repository.
 */
interface ISvnRepo extends IRepository {

    /**
     * Represents a versioned file in a repository.
     */
    public interface File {

        /**
         * Returns the name of this file.
         */
        public abstract String getName();
    }

    /**
     * Returns the URL of the repository.
     */
    public abstract SVNURL getRemoteUrl();

    /**
     * Returns the relative path of the repository wrt. the root URL of the remote repository.
     * For example, if the remote repository's URL is https://example.com/svn/repo/trunk and the root URL is
     * https://example.com/svn/repo, then the relative path returned is "/trunk".
     */
    public abstract String getRelativePath();

    /**
     * Returns a read-only view of all known log entries.
     */
    public abstract List<CachedLogEntry> getEntries();

    /**
     * Appends new log entries.
     */
    public abstract void appendNewEntries(final Collection<CachedLogEntry> newEntries);

    /**
     * Returns the path to the cache file.
     */
    public abstract java.io.File getCacheFilePath();

    /**
     * Determines all commits between passed revision and the latest one.
     */
    public abstract void getLog(final long startRevision, final ISVNLogEntryHandler handler) throws SVNException;

    /**
     * Returns the latest revision of this repository.
     */
    public abstract long getLatestRevision() throws SVNException;

    /**
     * Returns all files in a directory given its path and revision.
     * If the path points to a file, only this file is returned.
     * If the path points to a directory, this operation is applied to all of its children,
     * concatenating the resulting lists.
     * Returns an empty set if an error occurred.
     * Note that the revision passed is used for both path lookup and tree entry lookup. It is not possible to specify
     * a peg revision for path lookup and an operating revision for tree entry lookup.
     *
     * @param path The path to the tree entry (typically a directory) the children of which are to be returned.
     * @param revision The revision at which to look up the tree entry and its children.
     * @return A set of {@link File files} in no particular order.
     */
    public abstract Set<? extends File> getFiles(String path, IRepoRevision<?> revision);

    @Override
    public abstract IMutableFileHistoryGraph getFileHistoryGraph();

    /**
     * Sets the underlying {@link SvnFileHistoryGraph}.
     */
    public abstract void setFileHistoryGraph(final IMutableFileHistoryGraph fileHistoryGraph);

    /**
     * Clears the cache, both on disk and in memory.
     */
    public abstract void clearCache();
}
