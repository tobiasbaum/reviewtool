package de.setsoftware.reviewtool.model.remarks;

/**
 * Abstraction for the position of a review remark.
 */
public abstract class Position {

    public abstract String serialize();

    public abstract String getShortFileName();

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
