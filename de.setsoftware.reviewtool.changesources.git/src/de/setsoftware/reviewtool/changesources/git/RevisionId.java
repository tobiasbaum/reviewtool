package de.setsoftware.reviewtool.changesources.git;

import org.eclipse.jgit.revwalk.RevCommit;

import de.setsoftware.reviewtool.base.IPartiallyComparable;

/**
 * An ID for a git commit/revision.
 */
class RevisionId implements IPartiallyComparable<RevisionId> {

    private final int time;
    private final String id;

    public RevisionId(RevCommit commit) {
        this.time = commit.getCommitTime();
        this.id = commit.name();
    }

    @Override
    public boolean le(RevisionId other) {
        return this.time < other.time
            || this.id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return this.time;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof RevisionId)) {
            return false;
        }
        final RevisionId r = (RevisionId) o;
        return this.time == r.time && this.id.equals(r.id);
    }

}
