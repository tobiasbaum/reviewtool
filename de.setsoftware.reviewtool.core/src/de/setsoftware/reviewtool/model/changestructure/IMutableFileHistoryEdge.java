package de.setsoftware.reviewtool.model.changestructure;

/**
 * An edge in a {@link IMutableFileHistoryGraph} between an ancestor and a descendant {@link IMutableFileHistoryNode}.
 * It contains a {@link FileDiff}.
 */
public interface IMutableFileHistoryEdge extends IFileHistoryEdge {

    @Override
    public abstract IMutableFileHistoryNode getAncestor();

    @Override
    public abstract IMutableFileHistoryNode getDescendant();

    /**
     * Sets the associated {@link FileDiff} object.
     */
    public abstract void setDiff(FileDiff diff);

}
