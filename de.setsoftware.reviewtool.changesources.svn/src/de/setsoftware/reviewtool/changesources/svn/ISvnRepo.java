package de.setsoftware.reviewtool.changesources.svn;

import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.tmatesoft.svn.core.ISVNLogEntryHandler;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;

import de.setsoftware.reviewtool.model.api.IRepository;

/**
 * Interface to a remote Subversion repository.
 */
interface ISvnRepo extends IRepository {

    /**
     * Returns the URL of the repository.
     */
    public abstract SVNURL getRemoteUrl();

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
    public abstract IPath getCacheFilePath();

    /**
     * Determines all commits between passed revision and the latest one.
     */
    public abstract void getLog(final long startRevision, final ISVNLogEntryHandler handler) throws SVNException;

    /**
     * Returns the latest revision of this repository.
     */
    public abstract long getLatestRevision() throws SVNException;

    @Override
    public abstract SvnFileHistoryGraph getFileHistoryGraph();

    /**
     * Sets the underlying {@link SvnFileHistoryGraph}.
     */
    public abstract void setFileHistoryGraph(final SvnFileHistoryGraph fileHistoryGraph);
}
