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

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import de.setsoftware.reviewtool.base.Multimap;
import de.setsoftware.reviewtool.base.PartialOrderAlgorithms;
import de.setsoftware.reviewtool.base.ReviewtoolException;
import de.setsoftware.reviewtool.model.api.ILocalRevision;
import de.setsoftware.reviewtool.model.api.IRepoRevision;
import de.setsoftware.reviewtool.model.api.IRepository;
import de.setsoftware.reviewtool.model.api.IRevision;
import de.setsoftware.reviewtool.model.api.IRevisionVisitorE;
import de.setsoftware.reviewtool.model.api.IRevisionedFile;
import de.setsoftware.reviewtool.model.api.IUnknownRevision;
import de.setsoftware.reviewtool.model.api.IWorkingCopy;

/**
 * Default implementation of {@link IRevisionFile}.
 */
public class FileInRevision implements IRevisionedFile {

    private static final long serialVersionUID = 1849848257016731168L;

    private final String path;
    private final IRevision revision;

    FileInRevision(final String path, final IRevision revision) {
        this.path = path;
        this.revision = revision;
    }

    @Override
    public String getPath() {
        return this.path;
    }

    @Override
    public IRevision getRevision() {
        return this.revision;
    }

    @Override
    public IRepository getRepository() {
        return this.revision.getRepository();
    }

    @Override
    public byte[] getContents() throws Exception {
        return this.revision.accept(new IRevisionVisitorE<byte[], Exception>() {

            @Override
            public byte[] handleLocalRevision(final ILocalRevision revision) throws IOException {
                final IPath localPath = FileInRevision.this.toLocalPath(revision.getWorkingCopy());
                final File file = localPath.toFile();
                if (!file.exists()) {
                    return new byte[0];
                } else {
                    return Files.readAllBytes(file.toPath());
                }
            }

            @Override
            public byte[] handleRepoRevision(final IRepoRevision<?> revision) throws Exception {
                return FileInRevision.this.getRepository().getFileContents(FileInRevision.this.path, revision);
            }

            @Override
            public byte[] handleUnknownRevision(final IUnknownRevision revision) throws Exception {
                return new byte[0];
            }

        });
    }

    @Override
    public String toString() {
        return this.path + "@" + this.revision;
    }

    @Override
    public IPath toLocalPath(final IWorkingCopy wc) {
        final File absolutePathInWc = wc.toAbsolutePathInWc(this.path);
        if (absolutePathInWc != null) {
            return new Path(absolutePathInWc.getPath());
        } else {
            throw new ReviewtoolException("File " + this + " cannot be mapped to working copy at " + wc.getLocalRoot());
        }
    }

    @Override
    public int hashCode() {
        return this.path.hashCode() ^ this.revision.hashCode();
    }

    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof FileInRevision)) {
            return false;
        }
        final FileInRevision f = (FileInRevision) o;
        return this.path.equals(f.path)
            && this.revision.equals(f.revision);
    }

    @Override
    public boolean le(final IRevisionedFile other) {
        final IRevision otherRevision = other.getRevision();
        if (this.revision.le(otherRevision)) {
            if (otherRevision.le(this.revision)) {
                return this.path.compareTo(other.getPath()) <= 0;
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    /**
     * Sorts the given files topologically by their revisions. Per revision, the files are sorted by path.
     * This makes most sense when they all denote different versions of the same file.
     * For non-comparable revisions, the sort is stable. The earliest revisions come first.
     * Does NOT sort in-place.
     */
    public static List<IRevisionedFile> sortByRevision(final Collection<? extends IRevisionedFile> toSort) {

        if (toSort.isEmpty()) {
            return Collections.emptyList();
        }

        final LinkedHashSet<IRevision> remainingRevisions = new LinkedHashSet<>();
        final Multimap<IRevision, IRevisionedFile> filesForRevision = new Multimap<>();
        for (final IRevisionedFile f : toSort) {
            remainingRevisions.add(f.getRevision());
            filesForRevision.put(f.getRevision(), f);
        }

        final List<IRevisionedFile> ret = new ArrayList<>();
        while (!remainingRevisions.isEmpty()) {
            final IRevision smallest = PartialOrderAlgorithms.getSomeMinimum(remainingRevisions);
            final List<IRevisionedFile> revs = new ArrayList<>(filesForRevision.get(smallest));
            Collections.sort(revs, new Comparator<IRevisionedFile>() {

                @Override
                public int compare(final IRevisionedFile o1, final IRevisionedFile o2) {
                    return o1.getPath().compareTo(o2.getPath());
                }
            });
            ret.addAll(revs);
            remainingRevisions.remove(smallest);
        }
        return ret;
    }

}
