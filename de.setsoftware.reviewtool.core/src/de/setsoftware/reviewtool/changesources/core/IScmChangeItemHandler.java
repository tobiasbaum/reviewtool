package de.setsoftware.reviewtool.changesources.core;

/**
 * Processes change items.
 *
 * @param <ItemT> Type of a change item.
 * @param <R> Return type of {@link #processChangeItem(IScmChangeItem)}.
 */
public interface IScmChangeItemHandler<ItemT extends IScmChangeItem, R> {

    /**
     * Processes a single change item.
     *
     * @param item The change item to handle.
     */
    public abstract R processChangeItem(ItemT item);
}
