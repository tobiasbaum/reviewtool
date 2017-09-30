package de.setsoftware.reviewtool.ordering.efficientalgorithm;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

abstract class PositionTreeElement<T> {

    public final List<T> getPossibleOrder() {
        final List<T> ret = new ArrayList<>();
        this.addItemsInOrder(ret);
        return ret;
    }

    protected abstract PositionTreeElement<T> moveValueToPosition(
            Set<T> match, T toFix, TargetPosition position, boolean alsoFix);

    protected abstract PositionTreeElement<T> copyReplacing(
            PositionTreeElement<T> toReplace,
            PositionTreeElement<T> replacement);

    protected abstract void addItemsInOrder(List<T> buffer);

    protected abstract Set<T> getValuesInSubtree();

    protected abstract boolean satisfiesCurrently(T t, TargetPosition second);

    public boolean isSingleElement() {
        return this.getValuesInSubtree().size() == 1;
    }

}
