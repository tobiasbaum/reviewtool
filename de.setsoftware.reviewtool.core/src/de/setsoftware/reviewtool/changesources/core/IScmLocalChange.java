package de.setsoftware.reviewtool.changesources.core;

import de.setsoftware.reviewtool.model.api.ILocalRevision;
import de.setsoftware.reviewtool.model.api.IWorkingCopy;

/**
 * Describes a local change in a working copy.
 *
 * @param <ItemT> Type of a change item.
 */
public interface IScmLocalChange<ItemT extends IScmChangeItem> extends IScmChange<ItemT> {

    /**
     * Returns the working copy of this change.
     */
    public abstract IWorkingCopy getWorkingCopy();

    /**
     * Returns the {@link ILocalRevision} for this local change.
     */
    @Override
    public abstract ILocalRevision toRevision();
}
