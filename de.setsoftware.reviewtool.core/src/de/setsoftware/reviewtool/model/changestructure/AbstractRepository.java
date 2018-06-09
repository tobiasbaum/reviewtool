package de.setsoftware.reviewtool.model.changestructure;

import java.util.Collection;

import de.setsoftware.reviewtool.model.api.IRepoRevision;
import de.setsoftware.reviewtool.model.api.IRepository;
import de.setsoftware.reviewtool.model.api.IRevision;

/**
 * Common behaviour for {@link IRepository} implementations.
 */
public abstract class AbstractRepository implements IRepository {

    private static final long serialVersionUID = 7916699534735945340L;

    @Override
    public final boolean equals(final Object o) {
        if (o instanceof IRepository) {
            final IRepository other = (IRepository) o;
            return this.getId().equals(other.getId());
        }
        return false;
    }

    @Override
    public final int hashCode() {
        return this.getId().hashCode();
    }

    /**
     * Simple implementation for {@link #getSmallestRevision} that uses a comparable revision number.
     */
    public static IRevision getSmallestOfComparableRevisions(Collection<? extends IRevision> revisions) {
        IRevision smallestSoFar = null;
        for (final IRevision r : revisions) {
            if (r instanceof UnknownRevision) {
                //unknown revision (the source of an added file) is always smallest
                return r;
            } else if (smallestSoFar == null) {
                smallestSoFar = r;
            } else if (r instanceof RepoRevision) {
                if (smallestSoFar instanceof RepoRevision) {
                    final Object vs = ((IRepoRevision) smallestSoFar).getId();
                    final Object vr = ((IRepoRevision) r).getId();
                    if (areCompatibleComparables(vs, vr)) {
                        @SuppressWarnings("unchecked")
                        final Comparable<Comparable<?>> cs = (Comparable<Comparable<?>>) vs;
                        @SuppressWarnings("unchecked")
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

    private static boolean areCompatibleComparables(Object vs, Object vr) {
        return vs instanceof Comparable<?> && vs.getClass().equals(vr.getClass());
    }

}
