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
public class CachedLogEntry implements Serializable {

    private static final long serialVersionUID = 4057821677135067022L;

    private final long revision;
    private final String message;
    private final String author;
    private final Date date;
    private final SortedMap<String, CachedLogEntryPath> paths;

    public CachedLogEntry(SVNLogEntry logEntry) {
        this.revision = logEntry.getRevision();
        this.message = logEntry.getMessage();
        this.author = logEntry.getAuthor();
        this.date = logEntry.getDate();

        this.paths = new TreeMap<>();
        for (final Entry<String, SVNLogEntryPath> e : logEntry.getChangedPaths().entrySet()) {
            this.paths.put(e.getKey(), new CachedLogEntryPath(e.getValue()));
        }
    }

    public String getMessage() {
        return this.message;
    }

    public long getRevision() {
        return this.revision;
    }

    public Date getDate() {
        return this.date;
    }

    public String getAuthor() {
        return this.author;
    }

    public Map<String, CachedLogEntryPath> getChangedPaths() {
        return this.paths;
    }

    @Override
    public String toString() {
        return Long.toString(this.revision);
    }
}
