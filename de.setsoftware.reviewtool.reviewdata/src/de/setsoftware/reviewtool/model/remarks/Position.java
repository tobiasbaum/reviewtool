package de.setsoftware.reviewtool.model.remarks;

/**
 * Abstraction for the position of a review remark.
 */
public abstract class Position {

    public abstract String serialize();

    /**
     * Returns the filename (as used for serialization), or null iff there is no file for this remark.
     */
    public abstract String getShortFileName();

    /**
     * Returns the line of this remark, or 0 if there is no line for this remark.
     */
    public abstract int getLine();

    /**
     * Creates a position from its string representation.
     * A string representation can be created using {@link #serialize()}.
     */
    public static Position parse(String position) {
        if (position.isEmpty()) {
            return new GlobalPosition();
        }

        final String noParens = position.substring(1, position.length() - 1);
        final String[] parts = noParens.split(",");
        if (parts.length == 1) {
            return new FilePosition(parts[0].trim());
        } else {
            return new FileLinePosition(parts[0].trim(), Integer.parseInt(parts[1].trim()));
        }
    }

}
