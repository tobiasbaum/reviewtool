package de.setsoftware.reviewtool.model.api;

import java.io.Serializable;
import java.util.Set;

/**
 * A specialized {@link IFileHistoryNode node} in a {@link IMutableFileHistoryGraph}.
 */
public interface IMutableFileHistoryNode extends IFileHistoryNode, Serializable {

    @Override
    public abstract IMutableFileHistoryGraph getGraph();

    @Override
    public abstract Set<? extends IMutableFileHistoryEdge> getAncestors();

    @Override
    public abstract Set<? extends IMutableFileHistoryEdge> getDescendants();

}
