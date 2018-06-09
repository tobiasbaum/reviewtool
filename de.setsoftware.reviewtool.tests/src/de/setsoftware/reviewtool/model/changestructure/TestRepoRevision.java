package de.setsoftware.reviewtool.model.changestructure;

import de.setsoftware.reviewtool.model.api.IRepoRevision;
import de.setsoftware.reviewtool.model.api.IRepository;
import de.setsoftware.reviewtool.model.api.IRevisionVisitor;
import de.setsoftware.reviewtool.model.api.IRevisionVisitorE;

/**
 * Implements {@link IRepoRevision} for this test case.
 */
final class TestRepoRevision implements IRepoRevision {

    private final IRepository repo;
    private final Long id;

    TestRepoRevision(final IRepository repo, final Long id) {
        this.repo = repo;
        this.id = id;
    }

    @Override
    public IRepository getRepository() {
        return this.repo;
    }

    @Override
    public <R> R accept(final IRevisionVisitor<R> visitor) {
        return visitor.handleRepoRevision(this);
    }

    @Override
    public <R, E extends Throwable> R accept(final IRevisionVisitorE<R, E> visitor) throws E {
        return visitor.handleRepoRevision(this);
    }

    @Override
    public Object getId() {
        return this.id;
    }

    @Override
    public boolean equals(final Object o) {
        if (o instanceof TestRepoRevision) {
            final TestRepoRevision other = (TestRepoRevision) o;
            return this.id.equals(other.id);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public String toString() {
        return this.id.toString();
    }

}
