package de.setsoftware.reviewtool.model.changestructure;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import de.setsoftware.reviewtool.model.PositionTransformer;

/**
 * Denotes a certain revision of a file.
 */
public class FileInRevision {

    private final String path;
    private final Revision revision;
    private final Repository repo;

    public FileInRevision(String path, Revision revision, Repository repository) {
        this.path = path;
        this.revision = revision;
        this.repo = repository;
    }

    public String getPath() {
        return this.path;
    }

    public Revision getRevision() {
        return this.revision;
    }

    @Override
    public String toString() {
        return this.path + "@" + this.revision;
    }

    /**
     * Finds a resource corresponding to a path that is relative to the SCM repository root.
     * Heuristically drops path prefixes (like "trunk", ...) until a resource can be found.
     * If none can be found, null is returned.
     */
    public IResource determineResource() {
        String partOfPath = this.getPath();
        if (partOfPath.startsWith("/")) {
            partOfPath = partOfPath.substring(1);
        }
        while (true) {
            final IResource resource = PositionTransformer.toResource(partOfPath);
            if (!(resource instanceof IWorkspaceRoot)) {
                return resource;
            }
            final int slashIndex = partOfPath.indexOf('/');
            if (slashIndex < 0) {
                return null;
            }
            partOfPath = partOfPath.substring(slashIndex + 1);
        }
    }

    public IPath toLocalPath() {
        return new Path(this.repo.toAbsolutePathInWc(this.path));
    }

    @Override
    public int hashCode() {
        return this.path.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof FileInRevision)) {
            return false;
        }
        final FileInRevision f = (FileInRevision) o;
        return this.path.equals(f.path)
            && this.revision.equals(f.revision)
            && this.repo.equals(f.repo);
    }

}
