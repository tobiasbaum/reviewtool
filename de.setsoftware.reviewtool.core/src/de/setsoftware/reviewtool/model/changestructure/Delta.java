package de.setsoftware.reviewtool.model.changestructure;

import de.setsoftware.reviewtool.model.api.IDelta;

/**
 * Default implementation of {@link IDelta}.
 */
public final class Delta implements IDelta {

    private static final long serialVersionUID = 3698918072368955600L;

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

    @Override
    public int getLineOffset() {
        return this.lineOffset;
    }

    @Override
    public int getColumnOffset() {
        return this.columnOffset;
    }

    @Override
    public boolean isInline() {
        return this.lineOffset == 0;
    }

    @Override
    public IDelta plus(final IDelta other) {
        return new Delta(this.lineOffset + other.getLineOffset(), this.columnOffset + other.getColumnOffset());
    }

    @Override
    public Delta minus(final IDelta other) {
        return new Delta(this.lineOffset - other.getLineOffset(), this.columnOffset - other.getColumnOffset());
    }

    @Override
    public IDelta negate() {
        return new Delta(-this.lineOffset, -this.columnOffset);
    }

    @Override
    public IDelta ignoreColumnOffset() {
        return this.ignoreColumnOffset(true);
    }

    @Override
    public IDelta ignoreColumnOffset(final boolean ignore) {
        return new Delta(this.lineOffset, ignore ? 0 : this.columnOffset);
    }
}
