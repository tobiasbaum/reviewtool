package de.setsoftware.reviewtool.model.changestructure;

import de.setsoftware.reviewtool.model.api.IRepository;
import de.setsoftware.reviewtool.model.api.IRevisionVisitor;
import de.setsoftware.reviewtool.model.api.IRevisionVisitorE;
import de.setsoftware.reviewtool.model.api.IUnknownRevision;

/**
 * Default implementation of {@link IUnknownRevision}.
 */
public final class UnknownRevision implements IUnknownRevision {

    private final IRepository repo;

    UnknownRevision(final IRepository repo) {
        this.repo = repo;
    }

    @Override
    public IRepository getRepository() {
        return this.repo;
    }

    @Override
    public String toString() {
        return "?";
    }

    @Override
    public int hashCode() {
        return this.repo.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof UnknownRevision)) {
            return false;
        }
        final UnknownRevision r = (UnknownRevision) o;
        return this.repo.equals(r.repo);
    }

    @Override
    public <R> R accept(IRevisionVisitor<R> visitor) {
        return visitor.handleUnknownRevision(this);
    }

    @Override
    public <R, E extends Throwable> R accept(IRevisionVisitorE<R, E> visitor) throws E {
        return visitor.handleUnknownRevision(this);
    }
}
