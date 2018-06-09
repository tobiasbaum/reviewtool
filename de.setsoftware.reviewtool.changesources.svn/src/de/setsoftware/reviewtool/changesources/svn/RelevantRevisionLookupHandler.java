package de.setsoftware.reviewtool.changesources.svn;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.tmatesoft.svn.core.SVNException;

/**
 * Handler that filters log entries with a given pattern.
 * Also determines entries that are relevant to restore the full history of files in matching log entries.
 * These revisions have to be considered for a consistent history as otherwise consolidation of diffs is
 * inaccurate. Revisions that are retrofitted are marked as "invisible" in order to be able to differentiate between
 * "proper" and "technically necessary" revisions.
 */
final class RelevantRevisionLookupHandler implements CachedLogLookupHandler {

    private final Pattern pattern;
    private final List<SvnRevision> relevantEntries = new ArrayList<>();
    private SvnRepo currentRoot;

    RelevantRevisionLookupHandler(final Pattern patternForKey) {
        this.pattern = patternForKey;
    }

    @Override
    public void startNewRepo(final SvnRepo repo) {
        this.currentRoot = repo;
    }

    @Override
    public void handleLogEntry(final CachedLogEntry logEntry) throws SVNException {
        if (logEntry.getMessage() != null && this.pattern.matcher(logEntry.getMessage()).matches()) {
            assert this.currentRoot != null;
            this.relevantEntries.add(new SvnRevision(this.currentRoot, logEntry));
        }
    }

    /**
     * Returns all revisions that matched the given pattern and all revisions in between that touched
     * files changed in a matching revision.
     */
    List<? extends ISvnRevision> getRelevantRevisions() {
        return this.relevantEntries;
    }
}
