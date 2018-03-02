package de.setsoftware.reviewtool.model.api;

/**
 * A delta in a text file, denoted by line and column offset.
 */
public interface IDelta {

    /**
     * Returns the line offset.
     */
    public abstract int getLineOffset();

    /**
     * Returns the column offset.
     */
    public abstract int getColumnOffset();

    /**
     *Returns {@code true} if this is an in-line delta, i.e. if the line offset is zero.
     */
    public abstract boolean isInline();

    /**
     * Returns the sum of this Delta and another one.
     * @param other The other Delta.
     * @return The sum of this Delta and the passed one.
     */
    public abstract IDelta plus(IDelta other);

    /**
     * Returns the difference between this Delta and another one.
     * @param other The other Delta.
     * @return The difference between this Delta and the passed one.
     */
    public abstract IDelta minus(IDelta other);

    /**
     * Returns this delta with negated line and column offsets.
     */
    public abstract IDelta negate();

    /**
     * Returns a copy of {@code this} with the column offset set to zero.
     */
    public abstract IDelta ignoreColumnOffset();

    /**
     * Returns a copy of {@code this} with the column offset set to zero if {@code ignore} is {@code true},
     * {@code this} otherwise.
     */
    public abstract IDelta ignoreColumnOffset(boolean ignore);

}
