package de.setsoftware.reviewtool.changesources.svn;

import java.util.Date;
import java.util.Map;
import java.util.SortedMap;

import de.setsoftware.reviewtool.model.api.ILocalRevision;
import de.setsoftware.reviewtool.model.changestructure.ChangestructureFactory;

/**
 * Encapsulates the "revision" of a Subversion working copy.
 */
final class WorkingCopyRevision extends AbstractSvnRevision {
    private final SvnWorkingCopy wc;
    private final SortedMap<String, CachedLogEntryPath> paths;

    /**
     * Constructor.
     * @param repository The associated repository.
     * @param paths The paths changed in the working copy.
     */
    WorkingCopyRevision(final SvnWorkingCopy wc, final SortedMap<String, CachedLogEntryPath> paths) {
        this.wc = wc;
        this.paths = paths;
    }

    /**
     * Returns the associated working copy.
     */
    SvnWorkingCopy getWorkingCopy() {
        return this.wc;
    }

    @Override
    public SvnRepo getRepository() {
        return this.wc.getRepository();
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
        return ChangestructureFactory.createLocalRevision(this.wc);
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
    public String toPrettyString() {
        return "(work in progress)";
    }

    @Override
    public String toString() {
        return this.getRepository() + "@WORKING";
    }
}
