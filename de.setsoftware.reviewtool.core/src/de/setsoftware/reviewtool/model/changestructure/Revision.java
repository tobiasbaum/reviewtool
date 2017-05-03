package de.setsoftware.reviewtool.model.changestructure;

/**
 * A revision of a file (or a larger unit) in a source code management system.
 */
public abstract class Revision {

    /**
     * Accepts a {@link RevisionVisitor}.
     * @param visitor The visitor.
     * @return Some result.
     */
    public abstract <R> R accept(final RevisionVisitor<R> visitor);

    /**
     * Accepts a {@link RevisionVisitorE}.
     * @param visitor The visitor.
     * @return Some result.
     */
    public abstract <R, E extends Throwable> R accept(final RevisionVisitorE<R, E> visitor) throws E;

}
