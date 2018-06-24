package de.setsoftware.reviewtool.model.changestructure;

import java.util.Collection;

import de.setsoftware.reviewtool.model.api.IMutableFileHistoryGraph;
import de.setsoftware.reviewtool.model.api.IRepoRevision;
import de.setsoftware.reviewtool.model.api.IRevision;

final class PartiallyOrderedRepo extends AbstractRepository {

    public static PartiallyOrderedRepo INSTANCE = new PartiallyOrderedRepo();
    private static final long serialVersionUID = 1L;

    @Override
    public String getId() {
        return "stub";
    }

    @Override
    public IRepoRevision<PartiallyOrderedID> toRevision(final String revisionId) {
        return ChangestructureFactory.createRepoRevision(new PartiallyOrderedID(revisionId), this);
    }

    @Override
    public IRevision getSmallestRevision(final Collection<? extends IRevision> revisions) {
        return getSmallestOfComparableRevisions(revisions);
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
