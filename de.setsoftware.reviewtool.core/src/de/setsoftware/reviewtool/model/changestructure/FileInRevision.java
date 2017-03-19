package de.setsoftware.reviewtool.model.changestructure;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import de.setsoftware.reviewtool.base.Multimap;
import de.setsoftware.reviewtool.model.PositionTransformer;

/**
 * Denotes a certain revision of a file.
 */
public class FileInRevision {

    private final String path;
    private final Revision revision;
    private final Repository repo;
    private Path localPath;

    FileInRevision(String path, Revision revision, Repository repository) {
        this.path = path;
        this.revision = revision;
        this.repo = repository;
    }

    /**
     * Returns the path of the file (relative to the SCM repository root).
     */
    public String getPath() {
        return this.path;
    }

    public Revision getRevision() {
        return this.revision;
    }

    public Repository getRepository() {
        return this.repo;
    }

    /**
     * Returns this revisioned file's contents.
     * @return The file contents or null if an error occurs.
     */
    public byte[] getContents() {
        return this.revision.accept(new RevisionVisitor<byte[]>() {

            @Override
            public byte[] handleLocalRevision(LocalRevision revision) {
                final IPath localPath = FileInRevision.this.toLocalPath();
                final File file = localPath.toFile();
                try {
                    return Files.readAllBytes(file.toPath());
                } catch (final IOException e) {
                    return null;
                }
            }

            @Override
            public byte[] handleRepoRevision(RepoRevision revision) {
                return FileInRevision.this.repo.getFileContents(FileInRevision.this.path,
                            (RepoRevision) FileInRevision.this.revision);
            }

        });
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
                //perhaps too much was dropped and a different file then the intended returned
                //  therefore double check by using the inverse lookup
                final String shortName = PositionTransformer.toPosition(
                        resource.getFullPath(), 1, resource.getWorkspace()).getShortFileName();
                if (partOfPath.contains(shortName)) {
                    return resource;
                }
            }
            final int slashIndex = partOfPath.indexOf('/');
            if (slashIndex < 0) {
                return null;
            }
            partOfPath = partOfPath.substring(slashIndex + 1);
        }
    }

    /**
     * Returns the absolute path of the file in the local working copy.
     */
    public IPath toLocalPath() {
        if (this.localPath == null) {
            this.localPath = new Path(this.repo.toAbsolutePathInWc(this.path));
        }
        return this.localPath;
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

    /**
     * Sorts the given files topologically by their revisions. Per revision, the files are sorted by path.
     * This makes most sense when they all denote different versions of the same file.
     * For non-comparable revisions, the sort is stable.
     * Does NOT sort in-place.
     */
    public static List<FileInRevision> sortByRevision(Collection<? extends FileInRevision> toSort) {

        if (toSort.isEmpty()) {
            return Collections.emptyList();
        }
        //TODO better handling of multiple different repos
        final Repository repo = toSort.iterator().next().getRepository();

        final LinkedHashSet<Revision> remainingRevisions = new LinkedHashSet<>();
        final Multimap<Revision, FileInRevision> filesForRevision = new Multimap<>();
        for (final FileInRevision f : toSort) {
            remainingRevisions.add(f.getRevision());
            filesForRevision.put(f.getRevision(), f);
        }

        final List<FileInRevision> ret = new ArrayList<>();
        while (!remainingRevisions.isEmpty()) {
            final Revision smallest = repo.getSmallestRevision(remainingRevisions);
            final List<FileInRevision> revs = new ArrayList<>(filesForRevision.get(smallest));
            Collections.sort(revs, new Comparator<FileInRevision>() {

                @Override
                public int compare(final FileInRevision o1, final FileInRevision o2) {
                    return o1.getPath().compareTo(o2.getPath());
                }
            });
            ret.addAll(revs);
            remainingRevisions.remove(smallest);
        }
        return ret;
    }

}
