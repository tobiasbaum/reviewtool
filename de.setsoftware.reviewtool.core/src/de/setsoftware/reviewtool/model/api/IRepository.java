package de.setsoftware.reviewtool.model.api;

import java.io.File;
import java.util.Collection;

/**
 * A source code management repository.
 * Also includes information on a working copy.
 */
public interface IRepository {

    /**
     * Returns an identifier that is unique amongst all known repositories.
     */
    public abstract String getId();

    /**
     * Returns the root of the working copy.
     */
    public abstract File getLocalRoot();

    /**
     * Converts the string representation of a repository revision into a {@link IRepoRevision}.
     */
    public abstract IRepoRevision toRevision(String revisionId);

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
    public abstract IRevision getSmallestRevision(Collection<? extends IRevision> revisions);

    /**
     * Returns the contents of some revisioned file in the repository.
     * @param path The path to the file.
     * @param revision The revision of the file. This is also used as peg revision of the path passed above.
     * @return The file contents as a byte array.
     * @throws Exception if an error occurs.
     */
    public abstract byte[] getFileContents(String path, IRepoRevision revision) throws Exception;

}
