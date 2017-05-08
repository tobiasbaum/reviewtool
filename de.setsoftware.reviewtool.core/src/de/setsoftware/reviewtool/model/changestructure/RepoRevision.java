package de.setsoftware.reviewtool.model.changestructure;

import de.setsoftware.reviewtool.model.api.IRepoRevision;
import de.setsoftware.reviewtool.model.api.IRevisionVisitor;
import de.setsoftware.reviewtool.model.api.IRevisionVisitorE;

/**
 * Default implementation of {@link IRepoRevision}.
 */
public final class RepoRevision implements IRepoRevision {

    private final Object id;

    RepoRevision(Object id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return this.id.toString();
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof RepoRevision)) {
            return false;
        }
        final RepoRevision r = (RepoRevision) o;
        return this.id.equals(r.id);
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
