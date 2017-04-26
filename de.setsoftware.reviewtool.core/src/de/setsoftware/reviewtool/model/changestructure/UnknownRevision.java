package de.setsoftware.reviewtool.model.changestructure;

/**
 * Represents an unknown revision. Such revisions are used in the file history graph
 * when the file history is not completely known.
 */
public final class UnknownRevision extends Revision {

    @Override
    public String toString() {
        return "?";
    }

    @Override
    public int hashCode() {
        return 9871234;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof UnknownRevision;
    }

    @Override
    public <R> R accept(RevisionVisitor<R> visitor) {
        return visitor.handleUnknownRevision(this);
    }

    @Override
    public <R, E extends Throwable> R accept(RevisionVisitorE<R, E> visitor) throws E {
        return visitor.handleUnknownRevision(this);
    }
}
