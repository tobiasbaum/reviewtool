package de.setsoftware.reviewtool.changesources.svn;

import de.setsoftware.reviewtool.changesources.core.IScmCommitId;
import de.setsoftware.reviewtool.model.api.IRepoRevision;
import de.setsoftware.reviewtool.model.api.IRepository;
import de.setsoftware.reviewtool.model.changestructure.ChangestructureFactory;

/**
 * Represents a Subversion revision.
 */
final class SvnCommitId implements IScmCommitId<SvnCommitId> {

    private static final long serialVersionUID = -8793772569753406025L;
    private final long id;

    /**
     * Constructor.
     * @param id The underlying revision number.
     */
    SvnCommitId(final long id) {
        this.id = id;
    }

    /**
     * Returns the underlying revision number.
     */
    long getNumber() {
        return this.id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(this.id);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof SvnCommitId) {
            final SvnCommitId other = (SvnCommitId) obj;
            return this.id == other.id;
        }
        return false;
    }

    @Override
    public boolean le(final SvnCommitId other) {
        return this.id <= other.id;
    }

    @Override
    public IRepoRevision<SvnCommitId> toRevision(final IRepository repo) {
        return ChangestructureFactory.createRepoRevision(this, repo);
    }

    @Override
    public String toString() {
        return Long.toString(this.id);
    }
}
