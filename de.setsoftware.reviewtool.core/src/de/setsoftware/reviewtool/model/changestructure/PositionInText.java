package de.setsoftware.reviewtool.model.changestructure;

/**
 * A position in a text file, denoted by line and column number.
 */
public class PositionInText {

    public static final PositionInText UNKNOWN = new PositionInText(0, 0);

    private final int line;
    private final int column;

    public PositionInText(int line, int column) {
        this.line = line;
        this.column = column;
    }

    @Override
    public int hashCode() {
        return 100 * this.line + this.column;
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

}
