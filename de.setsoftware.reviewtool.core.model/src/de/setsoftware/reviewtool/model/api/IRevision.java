package de.setsoftware.reviewtool.model.api;

import java.io.Serializable;

import de.setsoftware.reviewtool.base.IPartiallyComparable;

/**
 * A revision of a file (or a larger unit) in a source code management system.
 */
public interface IRevision extends IPartiallyComparable<IRevision>, Serializable {

    /**
     * Returns the repository this revision is associated with.
     */
    public abstract IRepository getRepository();

    /**
     * Accepts a {@link IRevisionVisitor}.
     * @param visitor The visitor.
     * @return Some result.
     */
    public abstract <R> R accept(final IRevisionVisitor<R> visitor);

    /**
     * Accepts a {@link IRevisionVisitorE}.
     * @param visitor The visitor.
     * @return Some result.
     */
    public abstract <R, E extends Throwable> R accept(final IRevisionVisitorE<R, E> visitor) throws E;

    /**
     * {@inheritDoc}
     * <p/>
     * For usability reasons, this operation should refrain from putting the repository ID into the resulting string.
     */
    @Override
    public abstract String toString();
}
