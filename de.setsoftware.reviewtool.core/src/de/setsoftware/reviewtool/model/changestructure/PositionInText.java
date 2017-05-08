package de.setsoftware.reviewtool.model.changestructure;

import de.setsoftware.reviewtool.model.api.IDelta;
import de.setsoftware.reviewtool.model.api.IPositionInText;

/**
 * Default implementation of {@link PositionInText}.
 */
public class PositionInText implements IPositionInText {

    public static final IPositionInText UNKNOWN = new PositionInText(0, 0);

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

    @Override
    public int getLine() {
        return this.line;
    }

    @Override
    public int getColumn() {
        return this.column;
    }

    @Override
    public boolean lessThan(IPositionInText other) {
        return this.compareTo(other) < 0;
    }

    @Override
    public int compareTo(IPositionInText o) {
        if (this.getLine() == o.getLine()) {
            return this.getColumn() - o.getColumn();
        } else {
            return this.getLine() - o.getLine();
        }
    }

    @Override
    public IPositionInText adjust(int lineOffset) {
        return new PositionInText(this.line + lineOffset, this.column);
    }

    @Override
    public PositionInText plus(final IDelta delta) {
        return new PositionInText(this.line + delta.getLineOffset(), this.column + delta.getColumnOffset());
    }

    @Override
    public Delta minus(final IPositionInText other) {
        return new Delta(this.line - other.getLine(), this.column - other.getColumn());
    }

    @Override
    public IPositionInText startOfLine() {
        return new PositionInText(this.line, 1);
    }

    @Override
    public IPositionInText endOfLine() {
        if (this.column == 0) {
            return this;
        }
        return new PositionInText(this.line + 1, 0);
    }
}
