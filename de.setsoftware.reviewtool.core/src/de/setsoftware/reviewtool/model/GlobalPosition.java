package de.setsoftware.reviewtool.model;

/**
 * A dummy position that is used by global remarks.
 */
public class GlobalPosition extends Position {

    @Override
    public String serialize() {
        return "";
    }

    @Override
    public String getShortFileName() {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof GlobalPosition;
    }

    @Override
    public int hashCode() {
        return 97976;
    }

    @Override
    public String toString() {
        return "global";
    }

}
