package de.setsoftware.reviewtool.model.changestructure;

/**
 * A visitor for {@link Revision}s.
 *
 * @param <R> The result type for the operations.
 */
public interface RevisionVisitor<R> {

    /**
     * Handles a {@link LocalRevision}.
     * @param revision The revision to handle.
     * @return Some result.
     */
    R handleLocalRevision(LocalRevision revision);

    /**
     * Handles a {@link RepoRevision}.
     * @param revision The revision to handle.
     * @return Some result.
     */
    R handleRepoRevision(RepoRevision revision);

}
