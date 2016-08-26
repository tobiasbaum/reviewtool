package de.setsoftware.reviewtool.model.changestructure;

/**
 * A position in a text file, denoted by line and column number.
 */
public class PositionInText implements Comparable<PositionInText> {

    public static final PositionInText UNKNOWN = new PositionInText(0, 0);

    private final int line;
    private final int column;
    private final int absoluteOffset;

    PositionInText(int line, int column) {
        this(line, column, -1);
    }

    PositionInText(final int line, final int column, final int absoluteOffset) {
        this.line = line;
        this.column = column;
        this.absoluteOffset = absoluteOffset;
    }

    @Override
    public int hashCode() {
        return (100 * this.line + this.column) * 37 + this.absoluteOffset;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PositionInText)) {
            return false;
        }
        final PositionInText p = (PositionInText) o;
        return this.line == p.line && this.column == p.column && this.absoluteOffset == p.absoluteOffset;
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

    public int getAbsoluteOffset() {
        return this.absoluteOffset;
    }

    public PositionInText nextInLine() {
        return new PositionInText(this.line, this.column + 1,
                this.absoluteOffset == -1 ? -1 : this.absoluteOffset + 1);
    }

    /**
     * @return A new PositionInText object with the same line but decremented column.
     */
    public PositionInText prevInLine() {
        return new PositionInText(this.line, this.column - 1,
                this.absoluteOffset == -1 ? -1 : this.absoluteOffset - 1);
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
}
