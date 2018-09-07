package de.setsoftware.reviewtool.changesources.svn;

import java.util.Date;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNLogEntryPath;

import de.setsoftware.reviewtool.changesources.core.IScmCommit;
import de.setsoftware.reviewtool.model.api.IRepoRevision;
import de.setsoftware.reviewtool.model.api.IRepository;

/**
 * Implements {@link IScmCommit} for Subversion.
 */
final class SvnCommit extends SvnChange implements IScmCommit<SvnChangeItem, SvnCommitId> {

    private static final long serialVersionUID = -1774100131913294690L;

    private final IRepository repo;
    private final SvnCommitId commitId;
    private final String message;
    private final String author;
    private final Date date;

    /**
     * Constructor.
     *
     * @param repo The associated repository.
     * @param logEntry The Subversion log entry.
     */
    SvnCommit(final IRepository repo, final SVNLogEntry logEntry) {
        super(toChangeItems(logEntry));

        this.repo = repo;
        this.commitId = new SvnCommitId(logEntry.getRevision());
        this.message = logEntry.getMessage();
        this.author = logEntry.getAuthor();
        this.date = logEntry.getDate();
    }

    /**
     * Constructor. Only for testing.
     */
    SvnCommit(
            final IRepository repo,
            final long revision,
            final String message,
            final String author,
            final Date date,
            final SortedMap<String, SvnChangeItem> paths) {
        super(paths);
        this.repo = repo;
        this.commitId = new SvnCommitId(revision);
        this.message = message;
        this.author = author;
        this.date = date;
    }

    private static SortedMap<String, SvnChangeItem> toChangeItems(final SVNLogEntry logEntry) {
        final SortedMap<String, SvnChangeItem> changeItems = new TreeMap<>();
        final long prevRevision = logEntry.getRevision() - 1;
        for (final Entry<String, SVNLogEntryPath> e : logEntry.getChangedPaths().entrySet()) {
            changeItems.put(e.getKey(), new SvnChangeItem(e.getValue(), prevRevision));
        }
        return changeItems;
    }

    @Override
    public IRepository getRepository() {
        return this.repo;
    }

    @Override
    public SvnCommitId getId() {
        return this.commitId;
    }

    @Override
    public String getCommitter() {
        return this.author;
    }

    @Override
    public Date getCommitDate() {
        return new Date(this.date.getTime());
    }

    @Override
    public String getMessage() {
        return this.message;
    }

    @Override
    public IRepoRevision<SvnCommitId> toRevision() {
        return this.commitId.toRevision(this.repo);
    }
}
