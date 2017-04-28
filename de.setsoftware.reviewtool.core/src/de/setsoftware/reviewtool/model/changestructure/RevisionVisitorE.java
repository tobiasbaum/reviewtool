package de.setsoftware.reviewtool.model.changestructure;

/**
 * A visitor for {@link Revision}s that may throw an exception.
 *
 * @param <R> The result type for the operations.
 * @param <E> The type for the exceptions that may be thrown.
 */
public interface RevisionVisitorE<R, E extends Throwable> {

    /**
     * Handles a {@link LocalRevision}.
     * @param revision The revision to handle.
     * @return Some result.
     */
    R handleLocalRevision(LocalRevision revision) throws E;

    /**
     * Handles a {@link RepoRevision}.
     * @param revision The revision to handle.
     * @return Some result.
     */
    R handleRepoRevision(RepoRevision revision) throws E;

}
