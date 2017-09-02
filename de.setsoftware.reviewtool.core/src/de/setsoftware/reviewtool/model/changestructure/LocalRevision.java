package de.setsoftware.reviewtool.model.changestructure;

import de.setsoftware.reviewtool.model.api.ILocalRevision;
import de.setsoftware.reviewtool.model.api.IRevisionVisitor;
import de.setsoftware.reviewtool.model.api.IRevisionVisitorE;

/**
 * Default implementation of {@link ILocalRevision}.
 */
public final class LocalRevision implements ILocalRevision {

    LocalRevision() {
    }

    @Override
    public String toString() {
        return "$";
    }

    @Override
    public int hashCode() {
        return 1234987;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof LocalRevision;
    }

    @Override
    public <R> R accept(IRevisionVisitor<R> visitor) {
        return visitor.handleLocalRevision(this);
    }

    @Override
    public <R, E extends Throwable> R accept(IRevisionVisitorE<R, E> visitor) throws E {
        return visitor.handleLocalRevision(this);
    }
}
