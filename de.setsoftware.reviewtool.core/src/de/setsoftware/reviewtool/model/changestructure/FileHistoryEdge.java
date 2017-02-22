package de.setsoftware.reviewtool.model.changestructure;

/**
 * An edge in a {@link FileHistoryGraph}. It contains a {@link FileDiff} and a target node.
 */
public interface FileHistoryEdge {

    /**
     * Returns the target {@link FileHistoryNode} of this edge.
     */
    public abstract FileHistoryNode getTarget();

    /**
     * Returns the {@link FileDiff} bound to this edge.
     * <p/>
     * <em>Note:</em> Whether the {@link FileDiff} is to be interpreted in or against the direction of the edge
     * is determined by the context.
     */
    public abstract FileDiff getDiff();

}
