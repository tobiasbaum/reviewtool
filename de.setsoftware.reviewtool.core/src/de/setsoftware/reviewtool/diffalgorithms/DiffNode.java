package de.setsoftware.reviewtool.diffalgorithms;

/**
 * A diffnode in a diffpath.
 * A DiffNode and its previous node mark a delta between
 * two input sequences, that is, two differing subsequences
 * between (possibly zero length) matching sequences. The position contained in
 * the diff node is the first position outside the diff!
 *
 * {@link DiffNode DiffNodes} and {@link Snake Snakes} allow for compression
 * of diffpaths, as each snake is represented by a single {@link Snake Snake}
 * node and each contiguous series of insertions and deletions is represented
 * by a single {@link DiffNode DiffNodes}.
 */
public final class DiffNode extends PathNode {
    /**
     * Constructs a DiffNode.
     * DiffNodes are compressed. That means that
     * the path pointed to by the <code>prev</code> parameter
     * will be followed using {@link PathNode#previousSnake}
     * until a non-diff node is found.
     *
     * @param the position in the original sequence
     * @param the position in the revised sequence
     * @param prev the previous node in the path.
     */
    public DiffNode(int i, int j, PathNode prev) {
        super(i, j, (prev == null ? null : prev.previousSnake()));
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public boolean isSnake() {
        return false;
    }

    @Override
    public final PathNode previousSnake() {
        return this.getPrev() == null ? this : this.getPrev().previousSnake();
    }
}