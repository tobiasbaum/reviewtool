package de.setsoftware.reviewtool.model.changestructure;

import java.io.File;
import java.util.Collection;

/**
 * A stub implementation of {@link Repository} for use by tests.
 */
public final class StubRepo extends Repository {

    public static StubRepo INSTANCE = new StubRepo();

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
    public Revision getSmallestRevision(Collection<? extends Revision> revisions) {
        return this.getSmallestOfComparableRevisions(revisions);
    }

    @Override
    public byte[] getFileContents(final String path, final RepoRevision revision) {
        return new byte[0];
    }
}