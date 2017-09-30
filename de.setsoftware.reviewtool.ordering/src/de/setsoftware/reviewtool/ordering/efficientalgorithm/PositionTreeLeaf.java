package de.setsoftware.reviewtool.ordering.efficientalgorithm;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * A leaf in the position tree, corresponding to a single stop/change part.
 *
 * @param <T> Type of the stops.
 */
public class PositionTreeLeaf<T> extends PositionTreeElement<T> {

    private final T value;

    PositionTreeLeaf(T value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value.toString();
    }

    @Override
    protected void addItemsInOrder(List<T> buffer) {
        buffer.add(this.value);
    }

    @Override
    protected PositionTreeLeaf<T> moveValueToPosition(
            Set<T> match, T toFix, TargetPosition position, boolean alsoFix) {
        return this.satisfiesCurrently(toFix, position) ? this : null;
    }

    @Override
    protected PositionTreeElement<T> copyReplacing(PositionTreeElement<T> toReplace,
            PositionTreeElement<T> replacement) {
        return toReplace == this ? replacement : this;
    }

    @Override
    protected Set<T> getValuesInSubtree() {
        return Collections.singleton(this.value);
    }

    @Override
    protected boolean satisfiesCurrently(T t, TargetPosition pos) {
        return this.value.equals(t) && pos != TargetPosition.SECOND;
    }
}
