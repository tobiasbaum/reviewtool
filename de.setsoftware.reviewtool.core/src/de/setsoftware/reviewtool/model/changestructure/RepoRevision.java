package de.setsoftware.reviewtool.model.changestructure;

import de.setsoftware.reviewtool.model.api.IRepoRevision;
import de.setsoftware.reviewtool.model.api.IRepository;
import de.setsoftware.reviewtool.model.api.IRevisionVisitor;
import de.setsoftware.reviewtool.model.api.IRevisionVisitorE;

/**
 * Default implementation of {@link IRepoRevision}.
 */
public final class RepoRevision implements IRepoRevision {

    private final IRepository repo;
    private final Object id;

    RepoRevision(final Object id, final IRepository repo) {
        this.id = id;
        this.repo = repo;
    }

    @Override
    public IRepository getRepository() {
        return this.repo;
    }

    @Override
    public String toString() {
        return this.id.toString();
    }

    @Override
    public int hashCode() {
        return this.repo.hashCode() ^ this.id.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof RepoRevision)) {
            return false;
        }
        final RepoRevision r = (RepoRevision) o;
        return this.repo.equals(r.repo) && this.id.equals(r.id);
    }

    @Override
    public <R> R accept(IRevisionVisitor<R> visitor) {
        return visitor.handleRepoRevision(this);
    }

    @Override
    public <R, E extends Throwable> R accept(IRevisionVisitorE<R, E> visitor) throws E {
        return visitor.handleRepoRevision(this);
    }

    @Override
    public Object getId() {
        return this.id;
    }

}
