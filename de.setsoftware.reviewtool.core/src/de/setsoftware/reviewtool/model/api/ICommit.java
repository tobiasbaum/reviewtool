package de.setsoftware.reviewtool.model.api;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * A commit (SCM transaction) with the changes performed in it.
 */
public interface ICommit {

    /**
     * @return The message of the commit.
     */
    public abstract String getMessage();

    /**
     * @return The revision of the commit.
     */
    public abstract IRevision getRevision();

    /**
     * @return The {@list IChange}s of the commit.
     */
    public abstract List<? extends IChange> getChanges();

    /**
     * Creates and returns a copy of this commit where all changes contained in the given
     * set haven been changed to "irrelevant for review".
     */
    public abstract ICommit makeChangesIrrelevant(Set<? extends IChange> toMakeIrrelevant);

    /**
     * @return {@code true} if this {@link ICommit} is visible, else false.
     */
    public abstract boolean isVisible();

    /**
     * @return the date and time of the commit.
     */
    public abstract Date getTime();

}
