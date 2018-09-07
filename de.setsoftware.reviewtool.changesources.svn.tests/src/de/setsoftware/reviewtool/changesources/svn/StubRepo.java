package de.setsoftware.reviewtool.changesources.svn;

import de.setsoftware.reviewtool.model.api.IMutableFileHistoryGraph;
import de.setsoftware.reviewtool.model.api.IRepoRevision;
import de.setsoftware.reviewtool.model.changestructure.AbstractRepository;
import de.setsoftware.reviewtool.model.changestructure.ChangestructureFactory;

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
    public IRepoRevision<SvnCommitId> toRevision(final String revisionId) {
        try {
            return ChangestructureFactory.createRepoRevision(new SvnCommitId(Long.parseLong(revisionId)), this);
        } catch (final NumberFormatException e) {
            return null;
        }
    }

    @Override
    public byte[] getFileContents(final String path, final IRepoRevision<?> revision) {
        return new byte[0];
    }

    @Override
    public IMutableFileHistoryGraph getFileHistoryGraph() {
        return null;
    }
}
