package de.setsoftware.reviewtool.model.changestructure;

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

}
