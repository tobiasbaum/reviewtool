package de.setsoftware.reviewtool.model.changestructure;

/**
 * A delta in a text file, denoted by line and column offset.
 */
public final class Delta {

    private final int lineOffset;
    private final int columnOffset;

    /**
     * Creates an empty delta (with line offset and column offset set to zero).
     */
    Delta() {
        this(0, 0);
    }

    /**
     * Creates a delta.
     * @param lineOffset The line offset.
     * @param columnOffset The column offset.
     */
    Delta(final int lineOffset, final int columnOffset) {
        this.lineOffset = lineOffset;
        this.columnOffset = columnOffset;
    }

    @Override
    public int hashCode() {
        return (100 * this.lineOffset + this.columnOffset) * 37;
    }

    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof Delta)) {
            return false;
        }
        final Delta p = (Delta) o;
        return this.lineOffset == p.lineOffset && this.columnOffset == p.columnOffset;
    }

    @Override
    public String toString() {
        return "(" + this.lineOffset + "," + this.columnOffset + ")";
    }

    /**
     * Returns the line offset.
     */
    public int getLineOffset() {
        return this.lineOffset;
    }

    /**
     * Returns the column offset.
     */
    public int getColumnOffset() {
        return this.columnOffset;
    }

    /**
     * @return {@code true} if this is an in-line delta, i.e. if the line offset is zero.
     */
    public boolean isInline() {
        return this.lineOffset == 0;
    }

    /**
     * Returns the sum of this Delta and another one.
     * @param other The other Delta.
     * @return The sum of this Delta and the passed one.
     */
    public Delta plus(final Delta other) {
        return new Delta(this.lineOffset + other.lineOffset, this.columnOffset + other.columnOffset);
    }

    /**
     * Returns the difference between this Delta and another one.
     * @param other The other Delta.
     * @return The difference between this Delta and the passed one.
     */
    public Delta minus(final Delta other) {
        return new Delta(this.lineOffset - other.lineOffset, this.columnOffset - other.columnOffset);
    }

    /**
     * @return This delta with negated line and column offsets.
     */
    public Delta negate() {
        return new Delta(-this.lineOffset, -this.columnOffset);
    }

    /**
     * @return A copy of {@code this} with the column offset set to zero.
     */
    public Delta ignoreColumnOffset() {
        return this.ignoreColumnOffset(true);
    }

    /**
     * @return If {@code ignore} is set to true, a copy of {@code this} with the column offset set to zero,
     *      {@code this} otherwise.
     */
    public Delta ignoreColumnOffset(final boolean ignore) {
        return new Delta(this.lineOffset, ignore ? 0 : this.columnOffset);
    }
}
