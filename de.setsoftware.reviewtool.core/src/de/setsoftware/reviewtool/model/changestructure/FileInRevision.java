package de.setsoftware.reviewtool.model.changestructure;

/**
 * Denotes a certain revision of a file.
 */
public class FileInRevision {

    private final String path;
    private final Revision revision;

    public FileInRevision(String path, Revision revision) {
        this.path = path;
        this.revision = revision;
    }

    public String getPath() {
        return this.path;
    }

    @Override
    public String toString() {
        return this.path + "@" + this.revision;
    }

}
