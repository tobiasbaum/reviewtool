package de.setsoftware.reviewtool.changesources.git;

import java.util.Date;

import org.eclipse.jgit.revwalk.RevCommit;

import de.setsoftware.reviewtool.model.api.IRevision;
import de.setsoftware.reviewtool.model.api.IWorkingCopy;
import de.setsoftware.reviewtool.model.changestructure.ChangestructureFactory;

/**
 * Represents a Git revision.
 */
class GitRevision {

    private final GitWorkingCopy wc;
    private final RevCommit commit;

    GitRevision(GitWorkingCopy wc, RevCommit commit) {
        this.wc = wc;
        this.commit = commit;
    }

    /**
     * Returns the associated revision number as a readable string.
     */
    public String getRevisionString() {
        return this.commit.getId().name();
    }

    /**
     * Returns the {@link IRevision} for this Git commit.
     */
    public IRevision toRevision() {
        return ChangestructureFactory.createRepoRevision(new RevisionId(this.commit), this.wc.getRepository());
    }

    /**
     * Returns the associated commit date (specifically: the author date).
     */
    public Date getDate() {
        return this.commit.getAuthorIdent().getWhen();
    }

    /**
     * Returns the associated commit author.
     */
    public String getAuthor() {
        return this.commit.getAuthorIdent().getName();
    }

    /**
     * Returns the associated commit message.
     */
    public String getMessage() {
        return this.commit.getFullMessage();
    }

    //    /**
    //     * Returns the associated commit paths.
    //     */
    //    public abstract Map<String, CachedLogEntryPath> getChangedPaths();

    /**
     * Returns a pretty description of this revision.
     */
    public String toPrettyString() {
        final StringBuilder sb = new StringBuilder();
        final String message = this.getMessage();
        if (!message.isEmpty()) {
            sb.append(message);
            sb.append(" ");
        }
        sb.append(String.format(
                "(%tF %<tR, %s, %s)",
                this.getDate(),
                this.getAuthor(),
                this.getRevisionString()));
        return sb.toString();
    }

    public IWorkingCopy getWorkingCopy() {
        return this.wc;
    }
}
