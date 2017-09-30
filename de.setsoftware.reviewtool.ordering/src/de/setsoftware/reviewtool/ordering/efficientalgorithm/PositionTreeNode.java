package de.setsoftware.reviewtool.ordering.efficientalgorithm;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Superclass for intermediate nodes in the positioning tree.
 *
 * @param <T> Type of the stops.
 */
public abstract class PositionTreeNode<T> extends PositionTreeElement<T> {

    private final PositionTreeElement<T>[] children;

    PositionTreeNode(PositionTreeElement<T>[] children) {
        assert children.length >= 2;
        this.children = children;
    }

    /**
     * Fixes the position of the given element in the given match as requested by position.
     * Returns the new tree if the request could be satisfied and null if not.
     */
    public final PositionTreeNode<T> fixPosition(Set<T> match, T toFix, TargetPosition position) {
        assert match.size() >= 2;

        final PositionTreeNode<T> mostSpecificSubtree = this.findSubtreeForMatch(match);
        assert mostSpecificSubtree != null;
        final PositionTreeNode<T> newSubtree =
                mostSpecificSubtree.moveValueToPosition(match, toFix, position, true);
        if (newSubtree == null) {
            return null;
        }
        //TODO we are currently using immutable trees because we also use them for the bundle combination trees,
        //   but some things would be easier with mutable position trees
        return mostSpecificSubtree != newSubtree
                ? (PositionTreeNode<T>) this.copyReplacing(mostSpecificSubtree, newSubtree)
                : this;
    }

    @Override
    protected abstract PositionTreeNode<T> moveValueToPosition(
            Set<T> match, T toFix, TargetPosition position, boolean alsoFix);

    @Override
    protected void addItemsInOrder(List<T> buffer) {
        for (final PositionTreeElement<T> e : this.children) {
            e.addItemsInOrder(buffer);
        }
    }

    protected PositionTreeNode<T> findSubtreeForMatch(Set<T> match) {
        for (final PositionTreeElement<T> child : this.children) {
            if (child.getValuesInSubtree().containsAll(match)) {
                return ((PositionTreeNode<T>) child).findSubtreeForMatch(match);
            }
        }
        return this;
    }

    @Override
    protected Set<T> getValuesInSubtree() {
        return new HashSet<T>(this.getPossibleOrder());
    }

    protected final PositionTreeElement<T>[] getChildren() {
        return this.children;
    }

    protected final PositionTreeElement<T>[] copyChildren(PositionTreeElement<T> toReplace,
            PositionTreeElement<T> replacement) {
        final PositionTreeElement<T>[] newChildren = new PositionTreeElement[this.children.length];
        for (int i = 0; i < newChildren.length; i++) {
            newChildren[i] = this.children[i].copyReplacing(toReplace, replacement);
        }
        return newChildren;
    }

    @Override
    protected boolean satisfiesCurrently(T t, TargetPosition pos) {
        switch (pos) {
        case FIRST:
            return this.children[0].satisfiesCurrently(t, pos);
        case SECOND:
            if (this.children[0].isSingleElement()) {
                return this.children[1].satisfiesCurrently(t, TargetPosition.FIRST);
            } else {
                return this.children[0].satisfiesCurrently(t, TargetPosition.SECOND);
            }
        case LAST:
            return this.children[this.children.length - 1].satisfiesCurrently(t, pos);
        default:
            throw new AssertionError("invalid enum value " + pos);
        }
    }

}
