package de.setsoftware.reviewtool.model.api;

/**
 * A specialized {@link IFileHistoryEdge edge} in a {@link IMutableFileHistoryGraph}
 * between an ancestor and a descendant {@link IMutableFileHistoryNode}.
 */
public interface IMutableFileHistoryEdge extends IFileHistoryEdge {

    @Override
    public abstract IMutableFileHistoryGraph getGraph();

    @Override
    public abstract IMutableFileHistoryNode getAncestor();

    @Override
    public abstract IMutableFileHistoryNode getDescendant();

}
