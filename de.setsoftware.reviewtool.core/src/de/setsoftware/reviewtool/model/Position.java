package de.setsoftware.reviewtool.model;

public abstract class Position {

    public abstract String serialize();

    public abstract String getShortFileName();

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
