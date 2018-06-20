package de.setsoftware.reviewtool.model.api;

/**
 * A visitor for {@link IRevision}s that may throw an exception.
 *
 * @param <R> The result type for the operations.
 * @param <E> The type for the exceptions that may be thrown.
 */
public interface IRevisionVisitorE<R, E extends Throwable> {

    /**
     * Handles a {@link ILocalRevision}.
     * @param revision The revision to handle.
     * @return Some result.
     */
    public abstract R handleLocalRevision(ILocalRevision revision) throws E;

    /**
     * Handles a {@link IRepoRevision}.
     * @param revision The revision to handle.
     * @return Some result.
     */
    public abstract R handleRepoRevision(IRepoRevision<?> revision) throws E;

    /**
     * Handles an {@link IUnknownRevision}.
     * @param revision The revision to handle.
     * @return Some result.
     */
    public abstract R handleUnknownRevision(IUnknownRevision revision) throws E;

}
