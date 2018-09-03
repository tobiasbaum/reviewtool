package de.setsoftware.reviewtool.model.changestructure;

import de.setsoftware.reviewtool.model.api.IClassification;

public class Classification implements IClassification {

    public static final IClassification[] NONE = new IClassification[0];
    private final String name;

    public Classification(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Classification)) {
            return false;
        }
        final Classification c = (Classification) o;
        return this.name.equals(c.name);
    }

    @Override
    public String toString() {
        return this.name;
    }

}
