package de.setsoftware.reviewtool.changesources.svn;

import java.util.regex.Pattern;

import org.tmatesoft.svn.core.SVNException;

/**
 * Handler that filters log entries with a given pattern.
 */
final class RelevantRevisionLookupHandler implements CachedLogLookupHandler {

    private final Pattern pattern;

    RelevantRevisionLookupHandler(final Pattern patternForKey) {
        this.pattern = patternForKey;
    }

    @Override
    public boolean handleLogEntry(final CachedLogEntry logEntry) throws SVNException {
        return logEntry.getMessage() != null && this.pattern.matcher(logEntry.getMessage()).matches();
    }
}
