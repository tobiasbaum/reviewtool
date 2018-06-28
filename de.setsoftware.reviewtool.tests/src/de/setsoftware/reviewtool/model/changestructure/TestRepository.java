package de.setsoftware.reviewtool.model.changestructure;

import de.setsoftware.reviewtool.base.ComparableWrapper;
import de.setsoftware.reviewtool.model.api.IMutableFileHistoryGraph;
import de.setsoftware.reviewtool.model.api.IRepoRevision;
import de.setsoftware.reviewtool.model.api.IRepository;

/**
 * Implements {@link IRepository} for this test case.
 */
final class TestRepository extends AbstractRepository {

    private static final long serialVersionUID = 1L;

    private final String id;

    TestRepository(final String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public IRepoRevision<ComparableWrapper<Long>> toRevision(final String revisionId) {
        try {
            return new TestRepoRevision(this, Long.parseLong(revisionId));
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
