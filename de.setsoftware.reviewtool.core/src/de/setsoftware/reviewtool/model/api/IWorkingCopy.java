package de.setsoftware.reviewtool.model.api;

import java.io.File;

/**
 * The working copy for a source code management repository.
 */
public interface IWorkingCopy {

    /**
     * Returns the associated {@link IRepository}.
     */
    public abstract IRepository getRepository();

    /**
     * Returns the root of the working copy.
     */
    public abstract File getLocalRoot();

    /**
     * Returns the relative path of the working copy root wrt. the URL of the remote repository.
     * For example, if the remote repository's URL is https://example.com/svn/repo and the path "trunk/Workspace"
     * is checked out, then the relative path returned is "trunk/Workspace".
     */
    public abstract String getRelativePath();

    /**
     * Converts a path that is absolute in the repository to a path that is absolute in the file
     * system of the local working copy.
     * @param absolutePathInRepo The path to convert. It must begin with {@link #getRelativePath()}.
     * @return The converted path or {@code null} if the path points outside the working copy.
     */
    public abstract String toAbsolutePathInWc(String absolutePathInRepo);

    /**
     * Returns the associated file history graph.
     * This is a file history graph combining the one from the repository and the one from the local working copy.
     */
    public abstract IFileHistoryGraph getFileHistoryGraph();
}
