package de.setsoftware.reviewtool.model.changestructure;

import de.setsoftware.reviewtool.model.api.IFileDiff;
import de.setsoftware.reviewtool.model.api.IMutableFileHistoryEdge;

/**
 * An edge in a {@link FileHistoryGraph}. It always goes from a descendant node to an ancestor node.
 */
public final class FileHistoryEdge implements IMutableFileHistoryEdge {

    private final FileHistoryNode ancestor;
    private final FileHistoryNode descendant;
    private IFileDiff diff;

    /**
     * Constructor.
     * @param ancestor The ancestor node of the edge.
     * @param descendant The descendant node of the edge.
     * @param diff The associated {@link IFileDiff}. It can be changed later using {@link #setDiff(IFileDiff)}.
     */
    public FileHistoryEdge(final FileHistoryNode ancestor, final FileHistoryNode descendant, final IFileDiff diff) {
        this.ancestor = ancestor;
        this.descendant = descendant;
        this.diff = diff;
    }

    @Override
    public FileHistoryNode getAncestor() {
        return this.ancestor;
    }

    @Override
    public FileHistoryNode getDescendant() {
        return this.descendant;
    }

    @Override
    public IFileDiff getDiff() {
        return this.diff;
    }

    @Override
    public void setDiff(final IFileDiff diff) {
        this.diff = diff;
    }
}
