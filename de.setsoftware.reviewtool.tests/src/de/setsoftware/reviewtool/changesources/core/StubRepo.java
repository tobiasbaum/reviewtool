package de.setsoftware.reviewtool.changesources.core;

import de.setsoftware.reviewtool.changesources.core.DefaultScmRepository;
import de.setsoftware.reviewtool.model.api.IChangeSource;

public final class StubRepo extends DefaultScmRepository<StubChangeItem, StubCommitId, StubCommit, StubRepo> {

    private static final long serialVersionUID = -8016415563699680336L;

    public StubRepo(final IChangeSource changeSource, final String id, final StubScmRepositoryBridge scmBridge) {
        super(changeSource, id, scmBridge);
    }

    @Override
    public StubRepo getThis() {
        return this;
    }
}
