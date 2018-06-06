package de.setsoftware.reviewtool.changesources.svn;

import java.util.Date;
import java.util.Map;
import java.util.SortedMap;

import de.setsoftware.reviewtool.model.api.ILocalRevision;
import de.setsoftware.reviewtool.model.changestructure.ChangestructureFactory;

/**
 * Encapsulates the "revision" of a working copy.
 */
final class WorkingCopyRevision implements ISvnRevision {
    private final SvnRepo repository;
    private final SortedMap<String, CachedLogEntryPath> paths;

    /**
     * Constructor.
     * @param repository The associated repository.
     * @param paths The paths changed in the working copy.
     */
    WorkingCopyRevision(final SvnRepo repository, final SortedMap<String, CachedLogEntryPath> paths) {
        this.repository = repository;
        this.paths = paths;
    }

    @Override
    public SvnRepo getRepository() {
        return this.repository;
    }

    @Override
    public long getRevisionNumber() {
        return Long.MAX_VALUE;
    }

    @Override
    public String getRevisionString() {
        return "WORKING";
    }

    @Override
    public ILocalRevision toRevision() {
        return ChangestructureFactory.createLocalRevision(this.repository);
    }

    @Override
    public Date getDate() {
        return new Date(Long.MAX_VALUE);
    }

    @Override
    public String getAuthor() {
        return "";
    }

    @Override
    public String getMessage() {
        return "";
    }

    @Override
    public Map<String, CachedLogEntryPath> getChangedPaths() {
        return this.paths;
    }

    @Override
    public void accept(final ISvnRevisionVisitor visitor) {
        visitor.handle(this);
    }

    @Override
    public <E extends Exception> void accept(final ISvnRevisionVisitorE<E> visitor) throws E {
        visitor.handle(this);
    }

    @Override
    public String toPrettyString() {
        return "(work in progress)";
    }

    @Override
    public String toString() {
        return this.repository.toString() + "@WORKING";
    }
}
