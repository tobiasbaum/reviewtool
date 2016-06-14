package de.setsoftware.reviewtool.model.changestructure;

import java.util.Collection;

final class StubRepo extends Repository {

    public static StubRepo INSTANCE = new StubRepo();

    @Override
    public String toAbsolutePathInWc(String absolutePathInRepo) {
        return absolutePathInRepo;
    }

    @Override
    public Revision getSmallestRevision(Collection<? extends Revision> revisions) {
        return this.getSmallestOfComparableRevisions(revisions);
    }
}