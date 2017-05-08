package de.setsoftware.reviewtool.model.api;

/**
 * A visitor for {@link IRevision}s.
 *
 * @param <R> The result type for the operations.
 */
public interface IRevisionVisitor<R> {

    /**
     * Handles a {@link ILocalRevision}.
     * @param revision The revision to handle.
     * @return Some result.
     */
    public abstract R handleLocalRevision(ILocalRevision revision);

    /**
     * Handles a {@link IRepoRevision}.
     * @param revision The revision to handle.
     * @return Some result.
     */
    public abstract R handleRepoRevision(IRepoRevision revision);

    /**
     * Handles an {@link IUnknownRevision}.
     * @param revision The revision to handle.
     * @return Some result.
     */
    public abstract R handleUnknownRevision(IUnknownRevision revision);

}
