package de.setsoftware.reviewtool.changesources.svn;

import org.tmatesoft.svn.core.SVNException;

/**
 * Callback interface for log entries.
 */
public interface CachedLogLookupHandler {

    /**
     * Is called when traversal of a new repository starts.
     */
    public abstract void startNewRepo(SvnRepo svnRepo);

    /**
     * Is called for every log entry.
     */
    public abstract void handleLogEntry(CachedLogEntry logEntry) throws SVNException;

}
