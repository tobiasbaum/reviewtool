package de.setsoftware.reviewtool.changesources.svn;

import java.util.Date;
import java.util.Map;

import de.setsoftware.reviewtool.model.api.IRevision;

/**
 * Represents a Subversion revision which can be a repository revision or a working copy revision.
 */
interface SvnRevision {

    /**
     * Returns the associated repository.
     */
    public abstract ISvnRepo getRepository();

    /**
     * Returns the associated revision number.
     */
    public abstract long getRevisionNumber();

    /**
     * Returns the associated revision number as a readable string.
     */
    public abstract String getRevisionString();

    /**
     * Returns the {@link IRevision} for this Subversion revision.
     */
    public abstract IRevision toRevision();

    /**
     * Returns the associated commit date.
     */
    public abstract Date getDate();

    /**
     * Returns the associated commit author.
     */
    public abstract String getAuthor();

    /**
     * Returns the associated commit message.
     */
    public abstract String getMessage();

    /**
     * Returns the associated commit paths.
     */
    public abstract Map<String, CachedLogEntryPath> getChangedPaths();

    /**
     * Returns a pretty description of this revision.
     */
    public abstract String toPrettyString();

}
