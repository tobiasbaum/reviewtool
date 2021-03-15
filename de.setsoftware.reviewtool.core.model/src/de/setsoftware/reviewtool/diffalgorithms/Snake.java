package de.setsoftware.reviewtool.diffalgorithms;

/**
 * Represents a snake in a diff path. The position contained in
 * the diff node is the first position outside the snake!
 *
 * {@link DiffNode DiffNodes} and {@link Snake Snakes} allow for compression
 * of diffpaths, as each snake is represented by a single {@link Snake Snake}
 * node and each contiguous series of insertions and deletions is represented
 * by a single {@link DiffNode DiffNodes}.
 */
public final class Snake extends PathNode {
    /**
     * Constructs a snake node.
     *
     * @param the position in the original sequence
     * @param the position in the revised sequence
     * @param prev the previous node in the path.
     */
    public Snake(int i, int j, PathNode prev) {
        super(i, j, prev);
    }

    /**
     * {@inheritDoc}.
     * @return true always
     */
    @Override
    public boolean isSnake() {
        return true;
    }

    @Override
    public final PathNode previousSnake() {
        if (this.isBootstrap()) {
            return null;
        }
        return this;
    }
}