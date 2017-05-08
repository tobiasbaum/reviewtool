package de.setsoftware.reviewtool.model.api;

/**
 * A position in a text file, denoted by character index.
 */
public interface IPositionInText extends Comparable<IPositionInText> {

    /**
     * @return The one-based line number this {@link IPositionInText} refers to.
     */
    public abstract int getLine();

    /**
     * @return The one-based column number this {@link IPositionInText} refers to.
     */
    public abstract int getColumn();

    /**
     * @return {@code true} if this {@link IPositionInText} is before the passed one, else {@code false}.
     */
    public abstract boolean lessThan(IPositionInText other);

    /**
     * Adjusts the position giving a line offset.
     * @param lineOffset The line offset to add to current line.
     * @return A new PositionInText object with the same column but adjusted line.
     */
    public abstract IPositionInText adjust(int lineOffset);

    /**
     * Adds a delta to this position.
     * @param delta The delta to add to position.
     * @return A new PositionInText object.
     */
    public abstract IPositionInText plus(IDelta delta);

    /**
     * Returns the difference between this PositionInText and another one.
     * @param other The other PositionInText.
     * @return The delta between this PositionInText and the passed one.
     * @post {@code other.plus(result).equals(this)}
     */
    public abstract IDelta minus(IPositionInText other);

    /**
     * Returns the position at the start of the line.
     */
    public abstract IPositionInText startOfLine();

    /**
     * Returns the position at the end of the line.
     */
    public abstract IPositionInText endOfLine();

}
