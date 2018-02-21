package de.setsoftware.reviewtool.model.api;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * A commit (SCM transaction) with the changes performed in it.
 */
public interface ICommit {

    /**
     * Returns the message of the commit.
     */
    public abstract String getMessage();

    /**
     * Returns the revision of the commit.
     */
    public abstract IRevision getRevision();

    /**
     * Returns the {@list IChange}s of the commit.
     */
    public abstract List<? extends IChange> getChanges();

    /**
     * Creates and returns a copy of this commit where all changes contained in the given
     * set haven been changed to "irrelevant for review".
     */
    public abstract ICommit makeChangesIrrelevant(Set<? extends IChange> toMakeIrrelevant);

    /**
     * Returns the date and time of the commit.
     */
    public abstract Date getTime();

}
