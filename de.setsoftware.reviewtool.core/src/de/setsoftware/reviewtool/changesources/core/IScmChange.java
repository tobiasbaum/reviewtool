package de.setsoftware.reviewtool.changesources.core;

import java.util.Map;

import de.setsoftware.reviewtool.model.api.IMutableFileHistoryGraph;
import de.setsoftware.reviewtool.model.api.IRevision;

/**
 * Describes a change which consists of a set of {@link IScmChangeItem}s indexed by path.
 *
 * @param <ItemT> Type of a change item.
 */
public interface IScmChange<ItemT extends IScmChangeItem> {

    /**
     * Returns the {@link IRevision} for this change.
     */
    public abstract IRevision toRevision();

    /**
     * Returns a map from paths to changed items.
     */
    public abstract Map<String, ItemT> getChangedItems();

    /**
     * Integrates this change into a file history graph.
     *
     * @param graph The {@link IMutableFileHistoryGraph} to use.
     */
    public abstract void integrateInto(IMutableFileHistoryGraph graph);

    /**
     * Returns the default string representation of a change.
     */
    @Override
    public abstract String toString();
}
