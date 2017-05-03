package de.setsoftware.reviewtool.model.changestructure;

import java.util.Set;

/**
 * A node in a {@link IMutableFileHistoryGraph}.
 * It is bound to a {@link FileInRevision} and knows its ancestors and descendants.
 */
public interface IMutableFileHistoryNode extends IFileHistoryNode {

    @Override
    public abstract Set<? extends IMutableFileHistoryEdge> getAncestors();

    @Override
    public abstract Set<? extends IMutableFileHistoryEdge> getDescendants();

}
