package de.setsoftware.reviewtool.model.api;

import java.util.Date;
import java.util.List;
import java.util.function.Function;

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
     * Creates and returns a copy of this commit where all changes
     * have been transformed with the given function.
     */
    public abstract ICommit transformChanges(Function<IChange, IChange> f);

    /**
     * Returns the date and time of the commit.
     */
    public abstract Date getTime();

    /**
     * Returns the associated {@link IWorkingCopy}.
     */
    public abstract IWorkingCopy getWorkingCopy();
}
