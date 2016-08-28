package de.setsoftware.reviewtool.changesources.svn;

import java.util.Date;
import java.util.Map;

import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNLogEntryPath;

/**
 * Encapsulates a Subversion revision with associated information about the repository, the log message, the commit
 * date, the commit author, and the paths changed. In addition, a SvnRevision can be marked as "invisible". Invisible
 * revisions are revisions which would have been filtered out as they do not belong to the ticket in question but which
 * are necessary for the change history.
 */
class SvnRevision {
    private final SvnRepo repository;
    private final SVNLogEntry logEntry;
    private final boolean isVisible;

    /**
     * Constructor.
     * @param repository The associated repository.
     * @param logEntry The log entry.
     * @param isVisible True if the revision is visible, else false.
     */
    public SvnRevision(final SvnRepo repository, final SVNLogEntry logEntry, final boolean isVisible) {
        this.repository = repository;
        this.logEntry = logEntry;
        this.isVisible = isVisible;
    }

    /**
     * @return The associated repository.
     */
    public SvnRepo getRepository() {
        return this.repository;
    }

    /**
     * @return The associated revision number.
     */
    public long getRevision() {
        return this.logEntry.getRevision();
    }

    /**
     * @return The associated commit date.
     */
    public Date getDate() {
        return this.logEntry.getDate();
    }

    /**
     * @return The associated commit author.
     */
    public String getAuthor() {
        return this.logEntry.getAuthor();
    }

    /**
     * @return The associated commit message.
     */
    public String getMessage() {
        return this.logEntry.getMessage();
    }

    /**
     * @return The associated commit paths.
     */
    public Map<String, SVNLogEntryPath> getChangedPaths() {
        return this.logEntry.getChangedPaths();
    }

    /**
     * @return True if the revision is visible, else false.
     */
    public boolean isVisible() {
        return this.isVisible;
    }
}