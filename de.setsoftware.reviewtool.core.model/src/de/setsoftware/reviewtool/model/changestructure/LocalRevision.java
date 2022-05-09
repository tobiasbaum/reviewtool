package de.setsoftware.reviewtool.model.changestructure;

import de.setsoftware.reviewtool.model.api.ILocalRevision;
import de.setsoftware.reviewtool.model.api.IRepository;
import de.setsoftware.reviewtool.model.api.IRevision;
import de.setsoftware.reviewtool.model.api.IRevisionVisitor;
import de.setsoftware.reviewtool.model.api.IRevisionVisitorE;
import de.setsoftware.reviewtool.model.api.IWorkingCopy;

/**
 * Default implementation of {@link ILocalRevision}.
 */
public final class LocalRevision implements ILocalRevision {

    private static final long serialVersionUID = 1808884414733783082L;

    private final IWorkingCopy wc;

    LocalRevision(final IWorkingCopy wc) {
        this.wc = wc;
    }

    @Override
    public IRepository getRepository() {
        return this.wc.getRepository();
    }

    @Override
    public IWorkingCopy getWorkingCopy() {
        return this.wc;
    }

    @Override
    public String toString() {
        return "$";
    }

    @Override
    public int hashCode() {
        return this.wc.hashCode();
    }

    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof LocalRevision)) {
            return false;
        }
        final LocalRevision r = (LocalRevision) o;
        return this.wc.equals(r.wc);
    }

    @Override
    public <R> R accept(final IRevisionVisitor<R> visitor) {
        return visitor.handleLocalRevision(this);
    }

    @Override
    public <R, E extends Throwable> R accept(final IRevisionVisitorE<R, E> visitor) throws E {
        return visitor.handleLocalRevision(this);
    }

    @Override
    public boolean le(final IRevision other) {
        return this.equals(other);
    }
}
