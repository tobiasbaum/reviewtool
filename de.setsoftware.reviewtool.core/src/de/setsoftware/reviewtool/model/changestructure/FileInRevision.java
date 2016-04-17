package de.setsoftware.reviewtool.model.changestructure;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;

import de.setsoftware.reviewtool.model.PositionTransformer;

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

}
