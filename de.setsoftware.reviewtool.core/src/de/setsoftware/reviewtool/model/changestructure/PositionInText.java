package de.setsoftware.reviewtool.model.changestructure;

/**
 * A position in a text file, denoted by character index.
 */
public class PositionInText implements Comparable<PositionInText> {

    public static final PositionInText UNKNOWN = new PositionInText(0, 0);

    private final int line;
    private final int column;

    PositionInText(int line, int column) {
        assert column > 0 || column == 0 && line == 0;
        this.line = line;
        this.column = column;
    }

    @Override
    public int hashCode() {
        return (100 * this.line + this.column) * 37;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PositionInText)) {
            return false;
        }
        final PositionInText p = (PositionInText) o;
        return this.line == p.line && this.column == p.column;
    }

    @Override
    public String toString() {
        return this.line + ":" + this.column;
    }

    public int getLine() {
        return this.line;
    }

    public int getColumn() {
        return this.column;
    }

    public boolean lessThan(PositionInText other) {
        return this.compareTo(other) < 0;
    }

    @Override
    public int compareTo(PositionInText o) {
        if (this.getLine() == o.getLine()) {
            return this.getColumn() - o.getColumn();
        } else {
            return this.getLine() - o.getLine();
        }
    }

    /**
     * Adjusts the position giving a line offset.
     * @param lineOffset The line offset to add to current line.
     * @return A new PositionInText object with the same column but adjusted line.
     */
    public PositionInText adjust(int lineOffset) {
        return new PositionInText(this.line + lineOffset, this.column);
    }

    /**
     * Adds a delta to this position.
     * @param delta The delta to add to position.
     * @return A new PositionInText object.
     */
    public PositionInText plus(final Delta delta) {
        return new PositionInText(this.line + delta.getLineOffset(), this.column + delta.getColumnOffset());
    }

    /**
     * Returns the difference between this PositionInText and another one.
     * @param other The other PositionInText.
     * @return The delta between this PositionInText and the passed one.
     * @post {@code other.plus(result).equals(this)}
     */
    public Delta minus(final PositionInText other) {
        return new Delta(this.line - other.line, this.column - other.column);
    }

    /**
     * Returns the position at the start of the line.
     */
    public PositionInText startOfLine() {
        return new PositionInText(this.line, 1);
    }

    /**
     * Returns the position at the end of the line.
     */
    public PositionInText endOfLine() {
        if (this.column == 0) {
            return this;
        }
        return new PositionInText(this.line + 1, 0);
    }
}
