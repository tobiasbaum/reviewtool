package de.setsoftware.reviewtool.model.changestructure;

/**
 * A real revision in the SCM repository.
 */
public class RepoRevision extends Revision {

    private final Object id;

    public RepoRevision(Object id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return this.id.toString();
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof RepoRevision)) {
            return false;
        }
        final RepoRevision r = (RepoRevision) o;
        return this.id.equals(r.id);
    }

}
