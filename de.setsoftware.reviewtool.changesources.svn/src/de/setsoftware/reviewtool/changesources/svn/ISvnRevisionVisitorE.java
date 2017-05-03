package de.setsoftware.reviewtool.changesources.svn;

/**
 * Visitor for {@link ISvnRevision} allowing exceptions.
 * @param <E> The type of the exceptions to be allowed.
 */
public interface ISvnRevisionVisitorE<E extends Throwable> {

    /**
     * Handles a {@link SvnRevision}.
     */
    public abstract void handle(SvnRevision revision) throws E;

    /**
     * Handles a {@link WorkingCopyRevision}.
     */
    public abstract void handle(WorkingCopyRevision revision) throws E;

}
