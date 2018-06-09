package de.setsoftware.reviewtool.model.api;

import java.util.Set;

import de.setsoftware.reviewtool.model.changestructure.FileInRevision;

/**
 * A node in a {@link IMutableFileHistoryGraph}.
 * It is bound to a {@link FileInRevision} and knows its ancestors and descendants.
 */
public interface IMutableFileHistoryNode extends IFileHistoryNode {

    @Override
    public abstract IMutableFileHistoryGraph getGraph();

    @Override
    public abstract Set<? extends IMutableFileHistoryEdge> getAncestors();

    @Override
    public abstract Set<? extends IMutableFileHistoryEdge> getDescendants();

}
