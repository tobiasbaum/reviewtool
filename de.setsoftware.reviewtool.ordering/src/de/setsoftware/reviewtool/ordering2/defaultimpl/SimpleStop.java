package de.setsoftware.reviewtool.ordering2.defaultimpl;

import de.setsoftware.reviewtool.ordering2.base.Stop;

public class SimpleStop implements Stop {

    private final String id;

    public SimpleStop(String id) {
        this.id = id;
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SimpleStop)) {
            return false;
        }
        return ((SimpleStop) o).id.equals(this.id);
    }

    @Override
    public String toString() {
        return this.id;
    }

    @Override
    public int compareTo(Stop o) {
        if (o instanceof SimpleStop) {
            return this.id.compareTo(((SimpleStop) o).id);
        } else {
            return this.getClass().toString().compareTo(o.getClass().toString());
        }
    }

}
