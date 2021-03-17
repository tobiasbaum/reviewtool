package de.setsoftware.reviewtool.model.changestructure;

import de.setsoftware.reviewtool.base.ComparableWrapper;
import de.setsoftware.reviewtool.model.api.IRepoRevision;
import de.setsoftware.reviewtool.model.api.IRepository;
import de.setsoftware.reviewtool.model.api.IRevision;
import de.setsoftware.reviewtool.model.api.IRevisionVisitor;
import de.setsoftware.reviewtool.model.api.IRevisionVisitorE;

/**
 * Implements {@link IRepoRevision} for this test case.
 */
final class TestRepoRevision implements IRepoRevision<ComparableWrapper<Long>> {

    private static final long serialVersionUID = 1L;

    private final RepoRevision<ComparableWrapper<Long>> revision;

    TestRepoRevision(final IRepository repo, final Long id) {
        this.revision = new RepoRevision<>(ComparableWrapper.wrap(id), repo);
    }

    @Override
    public IRepository getRepository() {
        return this.revision.getRepository();
    }

    @Override
    public <R> R accept(final IRevisionVisitor<R> visitor) {
        return this.revision.accept(visitor);
    }

    @Override
    public <R, E extends Throwable> R accept(final IRevisionVisitorE<R, E> visitor) throws E {
        return this.revision.accept(visitor);
    }

    @Override
    public ComparableWrapper<Long> getId() {
        return this.revision.getId();
    }

    @Override
    public boolean le(final IRevision other) {
        return this.revision.le(other);
    }

    @Override
    public boolean equals(final Object o) {
        if (o instanceof TestRepoRevision) {
            final TestRepoRevision other = (TestRepoRevision) o;
            return this.revision.equals(other.revision);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.revision.hashCode();
    }

    @Override
    public String toString() {
        return this.revision.toString();
    }
}
