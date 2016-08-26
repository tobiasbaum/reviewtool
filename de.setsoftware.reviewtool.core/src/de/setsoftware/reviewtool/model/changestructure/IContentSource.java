package de.setsoftware.reviewtool.model.changestructure;

/**
 * Represents a content provider.
 */
public interface IContentSource {

    /**
     * Retrieves the contents of passed file.
     * @param path The path to the file.
     * @param revision The revision of the file to use.
     * @param repository The repository where the file lives.
     * @return The contents as a byte array or null not found.
     */
    byte[] getContents(String path, RepoRevision revision, Repository repository);

}
