package de.setsoftware.reviewtool.model.changestructure;

import de.setsoftware.reviewtool.base.IPartiallyComparable;
import de.setsoftware.reviewtool.model.api.ILocalRevision;
import de.setsoftware.reviewtool.model.api.IRepoRevision;
import de.setsoftware.reviewtool.model.api.IRepository;
import de.setsoftware.reviewtool.model.api.IRevision;
import de.setsoftware.reviewtool.model.api.IRevisionVisitor;
import de.setsoftware.reviewtool.model.api.IRevisionVisitorE;
import de.setsoftware.reviewtool.model.api.IUnknownRevision;

/**
 * Default implementation of {@link IRepoRevision}.
 *
 * @param <RevIdT> The type of the underlying revision identifier.
 */
public final class RepoRevision<RevIdT extends IPartiallyComparable<RevIdT>> implements IRepoRevision<RevIdT> {

    private static final long serialVersionUID = 1180259541435591492L;

    private final IRepository repo;
    private final RevIdT id;

    RepoRevision(final RevIdT id, final IRepository repo) {
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
    public boolean equals(final Object o) {
        if (!(o instanceof RepoRevision)) {
            return false;
        }
        final RepoRevision<?> r = (RepoRevision<?>) o;
        return this.repo.equals(r.repo) && this.id.equals(r.id);
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
    public RevIdT getId() {
        return this.id;
    }

    @Override
    public boolean le(final IRevision other) {
        return other.accept(new IRevisionVisitor<Boolean>() {

            @Override
            public Boolean handleLocalRevision(final ILocalRevision revision) {
                return true;
            }

            @Override
            public Boolean handleRepoRevision(final IRepoRevision<?> revision) {
                if (RepoRevision.this.id.getClass().equals(revision.getId().getClass())) {
                    @SuppressWarnings("unchecked")
                    final RevIdT otherId = (RevIdT) revision.getId();
                    return RepoRevision.this.repo.equals(revision.getRepository()) && RepoRevision.this.id.le(otherId);
                }
                return false;
            }

            @Override
            public Boolean handleUnknownRevision(final IUnknownRevision revision) {
                return false;
            }
        });
    }
}
