package de.setsoftware.reviewtool.model.changestructure;

import de.setsoftware.reviewtool.model.api.ILocalRevision;
import de.setsoftware.reviewtool.model.api.IRepository;
import de.setsoftware.reviewtool.model.api.IRevisionVisitor;
import de.setsoftware.reviewtool.model.api.IRevisionVisitorE;

/**
 * Default implementation of {@link ILocalRevision}.
 */
public final class LocalRevision implements ILocalRevision {

    private static final long serialVersionUID = 1808884414733783082L;

    private final IRepository repo;

    LocalRevision(final IRepository repo) {
        this.repo = repo;
    }

    @Override
    public IRepository getRepository() {
        return this.repo;
    }

    @Override
    public String toString() {
        return "$";
    }

    @Override
    public int hashCode() {
        return this.repo.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof LocalRevision)) {
            return false;
        }
        final LocalRevision r = (LocalRevision) o;
        return this.repo.equals(r.repo);
    }

    @Override
    public <R> R accept(IRevisionVisitor<R> visitor) {
        return visitor.handleLocalRevision(this);
    }

    @Override
    public <R, E extends Throwable> R accept(IRevisionVisitorE<R, E> visitor) throws E {
        return visitor.handleLocalRevision(this);
    }
}
