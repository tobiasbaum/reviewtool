package de.setsoftware.reviewtool.model.changestructure;

/**
 * The most recent version in the local working copy.
 */
public class LocalRevision extends Revision {

    LocalRevision() {
    }

    @Override
    public int hashCode() {
        return 1234987;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof LocalRevision;
    }

}
