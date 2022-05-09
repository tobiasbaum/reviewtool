package de.setsoftware.reviewtool.diffalgorithms;

/**
 * A node in a diffpath.
 */
public abstract class PathNode {
    /**
     * Position in the original sequence.
     */
    private int posOld;
    /**
     * Position in the revised sequence.
     */
    private int posNew;
    /**
     * The previous node in the path.
     */
    private PathNode prev;

    /**
     * Concatenates a new path node with an existing diffpath.
     * @param i The position in the original sequence for the new node.
     * @param j The position in the revised sequence for the new node.
     * @param prev The previous node in the path.
     */
    public PathNode(int i, int j, PathNode prev) {
        this.posOld = i;
        this.posNew = j;
        this.prev = prev;
    }

    final int getPosOld() {
        return this.posOld;
    }

    final int getPosNew() {
        return this.posNew;
    }

    final PathNode getPrev() {
        return this.prev;
    }

    /**
     * Returns true iff this node is a {@link Snake Snake node}.
     * @return true if this is a {@link Snake Snake node}
     */
    public abstract boolean isSnake();

    /**
     * Is this a bootstrap node?
     * In bootstrap nodes one of the two coordinates is
     * less than zero.
     */
    public final boolean isBootstrap() {
        return this.posOld < 0 || this.posNew < 0;
    }

    /**
     * Skips sequences of {@link DiffNode DiffNodes} until a
     * {@link Snake} or bootstrap node is found, or the end
     * of the path is reached.
     * @return The next first {@link Snake} or bootstrap node in the path, or
     * <code>null</code> if none found.
     */
    public abstract PathNode previousSnake();

    /**
     * {@inheritDoc}.
     */
    @Override
    public String toString() {
        final StringBuffer buf = new StringBuffer("[");
        PathNode node = this;
        while (node != null) {
            buf.append("(");
            buf.append(Integer.toString(node.posOld));
            buf.append(",");
            buf.append(Integer.toString(node.posNew));
            buf.append(")");
            node = node.prev;
        }
        buf.append("]");
        return buf.toString();
    }

    public final int getLengthNew() {
        return this.posNew - this.getStartPosNew();
    }

    public final int getStartPosNew() {
        return this.prev == null ? 0 : this.prev.posNew;
    }

    public final int getLengthOld() {
        return this.posOld - this.getStartPosOld();
    }

    public final int getStartPosOld() {
        return this.prev == null ? 0 : this.prev.posOld;
    }

    /**
     * Moves the node upwards by the given number of steps.
     */
    public final void moveUpwards(int steps) {
        this.posOld -= steps;
        this.posNew -= steps;
        if (this.prev != null) {
            this.prev.posOld -= steps;
            this.prev.posNew -= steps;
        }
    }

    /**
     * Joins this diff node with the next diff node (because the snake in between is empty).
     */
    public final void joinWithNextDiff() {
        assert this.prev.getLengthNew() == 0;
        final PathNode prevDiff = this.prev.prev;
        if (prevDiff == null) {
            this.prev = null;
        } else {
            if (prevDiff.getPrev() == null) {
                this.prev.prev = null;
            } else {
                this.prev = prevDiff.getPrev();
            }
        }
    }

    /**
     * Enlarges this node by the given amount.
     */
    public final void enlargeBy(int length) {
        this.posOld += length;
        this.posNew += length;
    }
}