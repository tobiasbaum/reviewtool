package de.setsoftware.reviewtool.model.changestructure;

import java.util.Collection;

import de.setsoftware.reviewtool.base.IPartiallyComparable;
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
     * Simple implementation for {@link #getSmallestRevision}.
     * If the underlying revisions are not totally ordered, some unspecified minimal revision will be returned.
     * If the collection of revisions passed is empty, {@code null} is returned.
     */
    public static IRevision getSmallestOfComparableRevisions(final Collection<? extends IRevision> revisions) {
        IRevision smallestSoFar = null;
        for (final IRevision r : revisions) {
            if (r instanceof UnknownRevision) {
                //unknown revision (the source of an added file) is always smallest
                return r;
            } else if (smallestSoFar == null) {
                smallestSoFar = r;
            } else if (r instanceof RepoRevision) {
                if (smallestSoFar instanceof RepoRevision) {
                    final IPartiallyComparable<?> vs = ((IRepoRevision<?>) smallestSoFar).getId();
                    final IPartiallyComparable<?> vr = ((IRepoRevision<?>) r).getId();
                    if (areCompatibleComparables(vs, vr)) {
                        @SuppressWarnings("unchecked")
                        final IPartiallyComparable<IPartiallyComparable<?>> cs =
                                (IPartiallyComparable<IPartiallyComparable<?>>) vs;
                        @SuppressWarnings("unchecked")
                        final IPartiallyComparable<IPartiallyComparable<?>> cr =
                                (IPartiallyComparable<IPartiallyComparable<?>>) vr;

                        if (cr.le(cs) && !cs.le(cr)) {
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

    private static boolean areCompatibleComparables(
            final IPartiallyComparable<?> vs,
            final IPartiallyComparable<?> vr) {
        return vs.getClass().equals(vr.getClass());
    }

}
