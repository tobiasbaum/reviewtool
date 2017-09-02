package de.setsoftware.reviewtool.model.changestructure;

import de.setsoftware.reviewtool.model.api.IRevisionVisitor;
import de.setsoftware.reviewtool.model.api.IRevisionVisitorE;
import de.setsoftware.reviewtool.model.api.IUnknownRevision;

/**
 * Default implementation of {@link IUnknownRevision}.
 */
public final class UnknownRevision implements IUnknownRevision {

    @Override
    public String toString() {
        return "?";
    }

    @Override
    public int hashCode() {
        return 9871234;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof UnknownRevision;
    }

    @Override
    public <R> R accept(IRevisionVisitor<R> visitor) {
        return visitor.handleUnknownRevision(this);
    }

    @Override
    public <R, E extends Throwable> R accept(IRevisionVisitorE<R, E> visitor) throws E {
        return visitor.handleUnknownRevision(this);
    }
}
