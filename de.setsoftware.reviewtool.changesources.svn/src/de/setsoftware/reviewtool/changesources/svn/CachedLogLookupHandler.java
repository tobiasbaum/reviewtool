package de.setsoftware.reviewtool.changesources.svn;

import org.tmatesoft.svn.core.SVNException;

/**
 * Filters log entries.
 */
public interface CachedLogLookupHandler {

    /**
     * Is called for every log entry.
     * @return {@code true} if log entry matches some user-defined criteria, else {@code false}.
     */
    public abstract boolean handleLogEntry(CachedLogEntry logEntry) throws SVNException;

}
