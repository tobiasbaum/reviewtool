package de.setsoftware.reviewtool.model.changestructure;

import de.setsoftware.reviewtool.model.api.IRepository;

/**
 * Common behaviour for {@link IRepository} implementations.
 */
public abstract class AbstractRepository implements IRepository {

    private static final long serialVersionUID = 7916699534735945340L;

    @Override
    public final boolean equals(final Object o) {
        if (o instanceof IRepository) {
            final IRepository other = (IRepository) o;
            return this.getId().equals(other.getId());
        }
        return false;
    }

    @Override
    public final int hashCode() {
        return this.getId().hashCode();
    }
}
