package de.setsoftware.reviewtool.changesources.svn;

import java.util.Date;
import java.util.Map;

import de.setsoftware.reviewtool.model.api.IRepoRevision;
import de.setsoftware.reviewtool.model.changestructure.ChangestructureFactory;

/**
 * Encapsulates a Subversion revision with associated information about the repository, the log message, the commit
 * date, the commit author, and the paths changed. In addition, a SvnRevision can be marked as "invisible". Invisible
 * revisions are revisions which would have been filtered out as they do not belong to the ticket in question but which
 * are necessary for the change history.
 */
final class SvnRevision implements ISvnRevision {
    private final SvnRepo repository;
    private final CachedLogEntry logEntry;
    private final boolean isVisible;

    /**
     * Constructor.
     * @param repository The associated repository.
     * @param logEntry The log entry.
     * @param isVisible True if the revision is visible, else false.
     */
    public SvnRevision(final SvnRepo repository, final CachedLogEntry logEntry, final boolean isVisible) {
        this.repository = repository;
        this.logEntry = logEntry;
        this.isVisible = isVisible;
    }

    @Override
    public SvnRepo getRepository() {
        return this.repository;
    }

    @Override
    public long getRevisionNumber() {
        return this.logEntry.getRevision();
    }

    @Override
    public String getRevisionString() {
        return Long.toString(this.logEntry.getRevision());
    }

    @Override
    public IRepoRevision toRevision() {
        return ChangestructureFactory.createRepoRevision(this.getRevisionNumber(), this.repository);
    }

    @Override
    public Date getDate() {
        return this.logEntry.getDate();
    }

    @Override
    public String getAuthor() {
        return this.logEntry.getAuthor();
    }

    @Override
    public String getMessage() {
        return this.logEntry.getMessage();
    }

    @Override
    public Map<String, CachedLogEntryPath> getChangedPaths() {
        return this.logEntry.getChangedPaths();
    }

    boolean isVisible() {
        return this.isVisible;
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
        final StringBuilder sb = new StringBuilder();
        final String message = this.getMessage();
        if (!message.isEmpty()) {
            sb.append(message);
            sb.append(" ");
        }
        sb.append(String.format(
                "(Rev. %s, %s)%s",
                this.getRevisionString(),
                this.getAuthor(),
                (this.isVisible ? "" : " [invisible]")));
        return sb.toString();
    }

    @Override
    public String toString() {
        return this.repository.toString() + "@" + this.logEntry.toString() + (this.isVisible ? "(+)" : "(-)");
    }
}
