package de.setsoftware.reviewtool.model.changestructure;

import java.io.File;
import java.util.Collection;

/**
 * A source code management repository.
 * Also includes information on a working copy.
 */
public abstract class Repository {

    /**
     * @return The root of the working copy.
     */
    public abstract File getLocalRoot();

    /**
     * Converts a path that is absolute in the repository to a path that is absolute in the file
     * system of the local working copy.
     */
    public abstract String toAbsolutePathInWc(String absolutePathInRepo);

    /**
     * Converts a path that is absolute in the file system of the working copy to a path that is absolute
     * in the repository.
     */
    public abstract String fromAbsolutePathInWc(String absolutePathInWc);

    /**
     * Returns one of the smallest revisions from the given collection. When there are multiple,
     * mutually incomparable, smallest elements, the first in iteration order is returned.
     */
    public abstract Revision getSmallestRevision(Collection<? extends Revision> revisions);

    /**
     * Returns the contents of some revisioned file in the repository.
     * @param path The path to the file.
     * @param revision The revision of the file. This is also used as peg revision of the path passed above.
     * @return The file contents as a byte array.
     * @throws Exception if an error occurs.
     */
    public abstract byte[] getFileContents(String path, RepoRevision revision) throws Exception;

    /**
     * Simple implementation for {@link #getSmallestRevision} that uses a comparable revision number.
     */
    protected final Revision getSmallestOfComparableRevisions(Collection<? extends Revision> revisions) {
        Revision smallestSoFar = null;
        for (final Revision r : revisions) {
            if (smallestSoFar == null) {
                smallestSoFar = r;
            } else if (r instanceof UnknownRevision) {
                //unknown revision (the source of an added file) is always smallest
                return r;
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
