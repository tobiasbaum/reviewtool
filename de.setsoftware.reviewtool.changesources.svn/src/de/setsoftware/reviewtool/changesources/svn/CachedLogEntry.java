package de.setsoftware.reviewtool.changesources.svn;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNLogEntryPath;

/**
 * Stores all relevant data from a log entry.
 */
final class CachedLogEntry implements Serializable {

    private static final long serialVersionUID = 4057821677135067022L;

    private final long revision;
    private final String message;
    private final String author;
    private final Date date;
    private final SortedMap<String, CachedLogEntryPath> paths;

    CachedLogEntry(final SVNLogEntry logEntry) {
        this.revision = logEntry.getRevision();
        this.message = logEntry.getMessage();
        this.author = logEntry.getAuthor();
        this.date = logEntry.getDate();

        this.paths = new TreeMap<>();
        for (final Entry<String, SVNLogEntryPath> e : logEntry.getChangedPaths().entrySet()) {
            this.paths.put(e.getKey(), new CachedLogEntryPath(e.getValue(), this.revision - 1));
        }
    }

    String getMessage() {
        return this.message;
    }

    long getRevision() {
        return this.revision;
    }

    Date getDate() {
        return this.date;
    }

    String getAuthor() {
        return this.author;
    }

    Map<String, CachedLogEntryPath> getChangedPaths() {
        return this.paths;
    }

    @Override
    public String toString() {
        return Long.toString(this.revision);
    }
}
