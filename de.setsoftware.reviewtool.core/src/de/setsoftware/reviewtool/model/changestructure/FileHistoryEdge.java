package de.setsoftware.reviewtool.model.changestructure;

/**
 * An edge in a {@link FileHistoryGraph}. It always goes from a descendant node to an ancestor node.
 */
public final class FileHistoryEdge implements IFileHistoryEdge {

    private final FileHistoryNode ancestor;
    private final FileHistoryNode descendant;
    private FileDiff diff;

    /**
     * Constructor.
     * @param ancestor The ancestor node of the edge.
     * @param descendant The descendant node of the edge.
     * @param diff The associated {@link FileDiff} object. It can be changed later using {@link #setDiff(FileDiff)}.
     */
    public FileHistoryEdge(final FileHistoryNode ancestor, final FileHistoryNode descendant, final FileDiff diff) {
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
    public FileDiff getDiff() {
        return this.diff;
    }

    /**
     * Sets the associated {@link FileDiff} object.
     */
    public void setDiff(final FileDiff diff) {
        this.diff = diff;
    }
}
