package de.setsoftware.reviewtool.changesources.core;

import de.setsoftware.reviewtool.changesources.core.IScmCommitId;
import de.setsoftware.reviewtool.model.api.IRepoRevision;
import de.setsoftware.reviewtool.model.api.IRepository;
import de.setsoftware.reviewtool.model.changestructure.ChangestructureFactory;

public final class StubCommitId implements IScmCommitId<StubCommitId> {

    private static final long serialVersionUID = -7286123286662549567L;
    private final long id;

    public StubCommitId(final long id) {
        this.id = id;
    }

    @Override
    public boolean le(final StubCommitId other) {
        return this.id <= other.id;
    }

    @Override
    public IRepoRevision<StubCommitId> toRevision(final IRepository repo) {
        return ChangestructureFactory.createRepoRevision(this, repo);
    }
}
