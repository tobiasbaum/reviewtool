package de.setsoftware.reviewtool.model.changestructure;

import java.util.Collection;

/**
 * A source code management repository.
 * Also includes information on a working copy.
 */
public abstract class Repository {

    /**
     * Converts a path that is absolute in the repository to a path that is absolute in the file
     * system of the local working copy.
     */
    public abstract String toAbsolutePathInWc(String absolutePathInRepo);

    /**
     * Returns one of the smallest revisions from the given collection. When there are multiple,
     * mutually incomparable, smallest elements, the first in iteration order is returned.
     */
    public abstract Revision getSmallestRevision(Collection<? extends Revision> revisions);

    /**
     * Simple implementation for {@link #getSmallestRevision} that uses a comparable revision number.
     */
    protected final Revision getSmallestOfComparableRevisions(Collection<? extends Revision> revisions) {
        Revision smallestSoFar = null;
        for (final Revision r : revisions) {
            if (smallestSoFar == null) {
                smallestSoFar = r;
            } else if (r instanceof RepoRevision) {
                if (smallestSoFar instanceof RepoRevision) {
                    final Object vs = ((RepoRevision) smallestSoFar).getId();
                    final Object vr = ((RepoRevision) r).getId();
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
