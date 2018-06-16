package de.setsoftware.reviewtool.model.changestructure;

import java.util.Collection;

import de.setsoftware.reviewtool.model.api.IMutableFileHistoryGraph;
import de.setsoftware.reviewtool.model.api.IRepoRevision;
import de.setsoftware.reviewtool.model.api.IRevision;

/**
 * A stub implementation of {@link AbstractRepository} for use by tests.
 */
public final class StubRepo extends AbstractRepository {

    public static StubRepo INSTANCE = new StubRepo();
    private static final long serialVersionUID = 1L;

    @Override
    public String getId() {
        return "stub";
    }

    @Override
    public IRepoRevision toRevision(final String revisionId) {
        return ChangestructureFactory.createRepoRevision(revisionId, this);
    }

    @Override
    public IRevision getSmallestRevision(Collection<? extends IRevision> revisions) {
        return getSmallestOfComparableRevisions(revisions);
    }

    @Override
    public byte[] getFileContents(final String path, final IRepoRevision revision) {
        return new byte[0];
    }

    @Override
    public IMutableFileHistoryGraph getFileHistoryGraph() {
        return null;
    }
}
