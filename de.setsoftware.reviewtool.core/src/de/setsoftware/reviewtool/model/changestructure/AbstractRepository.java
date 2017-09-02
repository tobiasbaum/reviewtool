package de.setsoftware.reviewtool.model.changestructure;

import java.util.Collection;

import de.setsoftware.reviewtool.model.api.IRepoRevision;
import de.setsoftware.reviewtool.model.api.IRepository;
import de.setsoftware.reviewtool.model.api.IRevision;

/**
 * Common behaviour for {@link IRepository} implementations.
 */
public abstract class AbstractRepository implements IRepository {

    /**
     * Simple implementation for {@link #getSmallestRevision} that uses a comparable revision number.
     */
    public IRevision getSmallestOfComparableRevisions(Collection<? extends IRevision> revisions) {
        IRevision smallestSoFar = null;
        for (final IRevision r : revisions) {
            if (smallestSoFar == null) {
                smallestSoFar = r;
            } else if (r instanceof UnknownRevision) {
                //unknown revision (the source of an added file) is always smallest
                return r;
            } else if (r instanceof RepoRevision) {
                if (smallestSoFar instanceof RepoRevision) {
                    final Object vs = ((IRepoRevision) smallestSoFar).getId();
                    final Object vr = ((IRepoRevision) r).getId();
                    if (this.areCompatibleComparables(vs, vr)) {
                        final Comparable<Comparable<?>> cs = (Comparable<Comparable<?>>) vs;
                        final Comparable<Comparable<?>> cr = (Comparable<Comparable<?>>) vr;
                        if (cr.compareTo(cs) < 0) {
                            smallestSoFar = r;
                        }
                    }
                } else {
                    smallestSoFar = r;
                }
            }
        }
        return smallestSoFar;
    }

    private boolean areCompatibleComparables(Object vs, Object vr) {
        return vs instanceof Comparable<?> && vs.getClass().equals(vr.getClass());
    }

}
