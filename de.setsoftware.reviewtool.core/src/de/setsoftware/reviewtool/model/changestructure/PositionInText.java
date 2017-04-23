package de.setsoftware.reviewtool.model.changestructure;

/**
 * A position in a text file, denoted by line and column number.
 */
public class PositionInText implements Comparable<PositionInText> {

    public static final PositionInText UNKNOWN = new PositionInText(0, 0);

    private final int line;
    private final int column;

    PositionInText(int line, int column) {
        this(line, column, -1);
    }

    PositionInText(final int line, final int column, final int absoluteOffset) {
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

    public PositionInText nextInLine() {
        return new PositionInText(this.line, this.column + 1);
    }

    /**
     * @return A new PositionInText object with the same line but decremented column.
     */
    public PositionInText prevInLine() {
        return new PositionInText(this.line, this.column - 1);
    }

    public PositionInText toPrevLine() {
        return new PositionInText(this.line - 1, this.column);
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
     * Adjusts the position giving a column offset.
     * @param columnOffset The column offset to add to current column.
     * @return A new PositionInText object with the same line but adjusted column.
     */
    public PositionInText adjustColumn(int columnOffset) {
        return new PositionInText(this.line, this.column + columnOffset);
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
