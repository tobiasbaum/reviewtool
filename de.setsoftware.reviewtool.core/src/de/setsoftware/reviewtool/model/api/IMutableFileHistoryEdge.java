package de.setsoftware.reviewtool.model.api;

import de.setsoftware.reviewtool.model.changestructure.FileDiff;

/**
 * An edge in a {@link IMutableFileHistoryGraph} between an ancestor and a descendant {@link IMutableFileHistoryNode}.
 * Its {@link FileDiff} object can be changed.
 */
public interface IMutableFileHistoryEdge extends IFileHistoryEdge {

    @Override
    public abstract IMutableFileHistoryNode getAncestor();

    @Override
    public abstract IMutableFileHistoryNode getDescendant();

    /**
     * Sets the associated {@link IFileDiff} object.
     */
    public abstract void setDiff(IFileDiff diff);

}
