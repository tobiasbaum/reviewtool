package de.setsoftware.reviewtool.model.changestructure;

import java.io.File;
import java.util.Collection;

import de.setsoftware.reviewtool.model.api.IRepoRevision;
import de.setsoftware.reviewtool.model.api.IRevision;

/**
 * A stub implementation of {@link AbstractRepository} for use by tests.
 */
public final class StubRepo extends AbstractRepository {

    public static StubRepo INSTANCE = new StubRepo();

    @Override
    public String getId() {
        return "stub";
    }

    @Override
    public File getLocalRoot() {
        return null;
    }

    @Override
    public String toAbsolutePathInWc(String absolutePathInRepo) {
        return absolutePathInRepo;
    }

    @Override
    public String fromAbsolutePathInWc(String absolutePathInWc) {
        return absolutePathInWc;
    }

    @Override
    public IRevision getSmallestRevision(Collection<? extends IRevision> revisions) {
        return this.getSmallestOfComparableRevisions(revisions);
    }

    @Override
    public byte[] getFileContents(final String path, final IRepoRevision revision) {
        return new byte[0];
    }
}