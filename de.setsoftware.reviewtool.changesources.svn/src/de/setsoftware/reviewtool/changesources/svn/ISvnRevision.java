package de.setsoftware.reviewtool.changesources.svn;

import java.util.Date;
import java.util.Map;

import de.setsoftware.reviewtool.model.changestructure.Revision;

/**
 * Represents a Subversion revision which can be a repository revision or a working copy revision.
 */
public interface ISvnRevision {

    /**
     * @return The associated repository.
     */
    public abstract SvnRepo getRepository();

    /**
     * @return The associated revision number.
     */
    public abstract long getRevisionNumber();

    /**
     * @return The associated revision number as a readable string.
     */
    public abstract String getRevisionString();

    /**
     * @return The {@link Revision} for this Subversion revision.
     */
    public abstract Revision toRevision();

    /**
     * @return The associated commit date.
     */
    public abstract Date getDate();

    /**
     * @return The associated commit author.
     */
    public abstract String getAuthor();

    /**
     * @return The associated commit message.
     */
    public abstract String getMessage();

    /**
     * @return The associated commit paths.
     */
    public abstract Map<String, CachedLogEntryPath> getChangedPaths();

    /**
     * @return True if the revision is visible, else false.
     */
    public abstract boolean isVisible();

    /**
     * Accepts a {@link ISvnRevisionVisitor}.
     */
    public abstract void accept(ISvnRevisionVisitor visitor);

    /**
     * Accepts a {@link ISvnRevisionVisitorE}.
     */
    public abstract <E extends Exception> void accept(ISvnRevisionVisitorE<E> visitor) throws E;

    /**
     * @return A pretty description of this revision.
     */
    public abstract String toPrettyString();

}
