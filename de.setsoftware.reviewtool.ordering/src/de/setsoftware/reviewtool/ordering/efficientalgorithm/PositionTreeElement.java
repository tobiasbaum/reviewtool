package de.setsoftware.reviewtool.ordering.efficientalgorithm;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Superclass for elements of the tree used for positioning the stops after they have been grouped.
 *
 * @param <T> Type of the stops.
 */
abstract class PositionTreeElement<T> {

    public abstract List<T> getPossibleOrder(Comparator<T> tieBreakingComparator);

    protected abstract PositionTreeElement<T> moveValueToPosition(
            Set<T> match, T toFix, TargetPosition position, boolean alsoFix);

    protected abstract PositionTreeElement<T> copyReplacing(
            PositionTreeElement<T> toReplace,
            PositionTreeElement<T> replacement);

    protected abstract void addItemsInOrder(Collection<T> buffer);

    protected abstract Set<T> getValuesInSubtree();

    protected abstract boolean satisfiesCurrently(T t, TargetPosition second);

    public boolean isSingleElement() {
        return this.getValuesInSubtree().size() == 1;
    }

}
