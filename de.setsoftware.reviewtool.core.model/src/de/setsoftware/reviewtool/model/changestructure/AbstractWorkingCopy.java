package de.setsoftware.reviewtool.model.changestructure;

import de.setsoftware.reviewtool.model.api.IWorkingCopy;

/**
 * Common behaviour for {@link IWorkingCopy} implementations.
 */
public abstract class AbstractWorkingCopy implements IWorkingCopy {

    @Override
    public final boolean equals(final Object o) {
        if (o instanceof IWorkingCopy) {
            final IWorkingCopy other = (IWorkingCopy) o;
            return this.getLocalRoot().equals(other.getLocalRoot());
        }
        return false;
    }

    @Override
    public final int hashCode() {
        return this.getLocalRoot().hashCode();
    }
}
