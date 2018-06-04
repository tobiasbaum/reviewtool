package de.setsoftware.reviewtool.model.changestructure;

import java.io.File;
import java.util.Collection;

import de.setsoftware.reviewtool.model.api.IMutableFileHistoryGraph;
import de.setsoftware.reviewtool.model.api.IRepoRevision;
import de.setsoftware.reviewtool.model.api.IRepository;
import de.setsoftware.reviewtool.model.api.IRevision;

/**
 * Implements {@link IRepository} for this test case.
 */
final class TestRepository extends AbstractRepository {

    private final String id;
    private final File localRoot;

    TestRepository(final String id, final File localRoot) {
        this.id = id;
        this.localRoot = localRoot;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public File getLocalRoot() {
        return this.localRoot;
    }

    @Override
    public IRepoRevision toRevision(final String revisionId) {
        try {
            return new TestRepoRevision(this, Long.parseLong(revisionId));
        } catch (final NumberFormatException e) {
            return null;
        }
    }

    @Override
    public String toAbsolutePathInWc(final String absolutePathInRepo) {
        return new File(this.localRoot, absolutePathInRepo).getAbsolutePath();
    }

    @Override
    public String fromAbsolutePathInWc(final String absolutePathInWc) {
        if (absolutePathInWc.startsWith(this.localRoot.getAbsolutePath())) {
            return absolutePathInWc.substring(this.localRoot.getAbsolutePath().length());
        } else {
            return null;
        }
    }

    @Override
    public IRevision getSmallestRevision(final Collection<? extends IRevision> revisions) {
        return getSmallestOfComparableRevisions(revisions);
    }

    @Override
    public byte[] getFileContents(final String path, final IRepoRevision revision) throws Exception {
        return new byte[0];
    }

    @Override
    public IMutableFileHistoryGraph getFileHistoryGraph() {
        return null;
    }
}
