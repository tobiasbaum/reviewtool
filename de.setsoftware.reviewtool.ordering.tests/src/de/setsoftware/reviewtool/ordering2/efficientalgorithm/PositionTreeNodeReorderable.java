package de.setsoftware.reviewtool.ordering2.efficientalgorithm;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

public class PositionTreeNodeReorderable<T> extends PositionTreeNode<T> {

    private Map<TargetPosition, T> fixedPositions;

    PositionTreeNodeReorderable(PositionTreeElement<T>[] children) {
        this(children, Collections.<TargetPosition, T>emptyMap());
    }

    PositionTreeNodeReorderable(PositionTreeElement<T>[] children, Map<TargetPosition, T> fixedPositions) {
        super(children);
        this.fixedPositions = fixedPositions;
    }

    @Override
    public PositionTreeNodeReorderable<T> moveValueToPosition(
            Set<T> match, T value, TargetPosition position, boolean alsoFix) {

        //when there would be non-matched elements, we would have to solve the NP-complete subset sum
        //  problem as a subproblem. Luckily, this cannot happen.
        assert match.containsAll(this.getValuesInSubtree())
            : "when there are non-matched values, it must be a fixed node when the tree has been properly constructed";

        if (this.fixedPositions.containsKey(position) && !this.fixedPositions.get(position).equals(value)) {
            return null;
        }
        if (position == TargetPosition.SECOND && this.getValuesInSubtree().size() == 2) {
            //in subtrees with only two values, there is no SECOND, only LAST
            return this.moveValueToPosition(match, value, TargetPosition.LAST, alsoFix);
        }

        final Map<TargetPosition, T> fixesToCheck;
        if (this.fixedPositions.isEmpty()) {
            fixesToCheck = new EnumMap<>(TargetPosition.class);
        } else {
            fixesToCheck = new EnumMap<>(this.fixedPositions);
        }
        fixesToCheck.put(position, value);

        final Map<TargetPosition, T> newFixes;
        if (alsoFix) {
            newFixes = fixesToCheck;
        } else {
            newFixes = this.fixedPositions;
        }

        if (this.satisfiesFixes(this.getChildren(), fixesToCheck)) {
            return new PositionTreeNodeReorderable<>(this.getChildren(), newFixes);
        }

        //the algorithm assumes that positions are fixed in a bottom up manner, so that child nodes will not change
        //  any more except under the control of the current node when this method is called

        final int childIndexWithValue = this.findChildIndexWithValue(value);
        final PositionTreeElement<T> childWithValue = this.getChildren()[childIndexWithValue];
        final PositionTreeElement<T> replacement;
        final PositionTreeElement<T>[] copies;
        switch (position) {
        case FIRST:
            replacement = childWithValue.moveValueToPosition(match, value, TargetPosition.FIRST, false);
            if (replacement == null) {
                return null;
            }
            copies = this.copyChildren(childWithValue, replacement);
            this.moveToFront(copies, childIndexWithValue);
            break;
        case SECOND:
            if (this.getChildren()[0].isSingleElement()) {
                replacement = childWithValue.moveValueToPosition(match, value, TargetPosition.FIRST, false);
                if (replacement == null) {
                    return null;
                }
                copies = this.copyChildren(childWithValue, replacement);
                this.moveToSecond(copies, childIndexWithValue);
            } else {
                replacement = childWithValue.moveValueToPosition(match, value, TargetPosition.SECOND, false);
                if (replacement == null) {
                    //element cannot be put at second position in child, test if putting something else
                    //  before it would work
                    final PositionTreeElement<T> replacement2 =
                            childWithValue.moveValueToPosition(match, value, TargetPosition.FIRST, false);
                    if (replacement2 == null) {
                        return null;
                    }
                    copies = this.copyChildren(childWithValue, replacement2);
                    this.moveToFront(copies, childIndexWithValue);
                    final int indexFrontFiller = this.findSingleElementToPutInFront(copies, childWithValue, newFixes);
                    assert indexFrontFiller != 0;
                    if (indexFrontFiller < 0) {
                        return null;
                    }
                    this.moveToFront(copies, indexFrontFiller);
                } else {
                    copies = this.copyChildren(childWithValue, replacement);
                    this.moveToFront(copies, childIndexWithValue);
                }
            }
            break;
        case LAST:
            replacement = childWithValue.moveValueToPosition(match, value, TargetPosition.LAST, false);
            if (replacement == null) {
                return null;
            }
            copies = this.copyChildren(childWithValue, replacement);
            this.moveToBack(copies, childIndexWithValue);
            break;
        default:
            throw new AssertionError("invalid enum value " + position);
        }

        if (this.satisfiesFixes(copies, fixesToCheck)) {
            return new PositionTreeNodeReorderable<>(copies, newFixes);
        }
        if (this.fixedPositions.containsKey(TargetPosition.SECOND)) {
            //by moving the children, other fixes might have become invalidated
            //  in the case of SECOND, this can possible by repaired
            final T valueForSecond = this.fixedPositions.get(TargetPosition.SECOND);
            newFixes.remove(TargetPosition.SECOND);
            return new PositionTreeNodeReorderable<>(copies, newFixes)
                    .moveValueToPosition(match, valueForSecond, TargetPosition.SECOND, true);
        }

        return null;
    }

    private int findSingleElementToPutInFront(
            PositionTreeElement<T>[] copies,
            PositionTreeElement<T> dontUse,
            Map<TargetPosition, T> newFixes) {

        final T last = newFixes.get(TargetPosition.LAST);
        final T second = newFixes.get(TargetPosition.SECOND);
        for (int i = 1; i < copies.length; i++) {
            final PositionTreeElement<T> cur = copies[i];
            if (cur == dontUse) {
                continue;
            }
            if (!cur.isSingleElement()) {
                continue;
            }
            final Set<T> curValues = cur.getValuesInSubtree();
            if (curValues.contains(last) || curValues.contains(second)) {
                continue;
            }
            return i;
        }
        return -1;
    }

    private void moveToFront(PositionTreeElement<T>[] array, int index) {
        final PositionTreeElement<T> tmp = array[index];
        for (int i = index; i > 0; i--) {
            array[i] = array[i - 1];
        }
        array[0] = tmp;
    }

    private void moveToSecond(PositionTreeElement<T>[] array, int index) {
        final PositionTreeElement<T> tmp = array[index];
        if (index == 0) {
            array[0] = array[1];
        } else {
            for (int i = index; i > 1; i--) {
                array[i] = array[i - 1];
            }
        }
        array[1] = tmp;
    }

    private void moveToBack(PositionTreeElement<T>[] array, int index) {
        final PositionTreeElement<T> tmp = array[index];
        for (int i = index; i < array.length - 1; i++) {
            array[i] = array[i + 1];
        }
        array[array.length - 1] = tmp;
    }

    private boolean satisfiesFixes(
            PositionTreeElement<T>[] children,
            Map<TargetPosition, T> newFixes) {

        if (newFixes.containsKey(TargetPosition.FIRST)) {
            if (!children[0].satisfiesCurrently(newFixes.get(TargetPosition.FIRST), TargetPosition.FIRST)) {
                return false;
            }
        }
        if (newFixes.containsKey(TargetPosition.SECOND)) {
            if (children[0].isSingleElement()) {
                if (!children[1].satisfiesCurrently(newFixes.get(TargetPosition.SECOND), TargetPosition.FIRST)) {
                    return false;
                }
            } else {
                if (!children[0].satisfiesCurrently(newFixes.get(TargetPosition.SECOND), TargetPosition.SECOND)) {
                    return false;
                }
            }
        }
        if (newFixes.containsKey(TargetPosition.LAST)) {
            if (!children[children.length - 1].satisfiesCurrently(newFixes.get(TargetPosition.LAST), TargetPosition.LAST)) {
                return false;
            }
        }
        return true;
    }

    private int findChildIndexWithValue(T toFix) {
        for (int i = 0; i < this.getChildren().length; i++) {
            if (this.getChildren()[i].getValuesInSubtree().contains(toFix)) {
                return i;
            }
        }
        throw new AssertionError("value not found " + toFix + " in " + this);
    }

    @Override
    protected PositionTreeElement<T> copyReplacing(PositionTreeElement<T> toReplace,
            PositionTreeElement<T> replacement) {
        if (toReplace == this) {
            return replacement;
        }
        return new PositionTreeNodeReorderable<T>(this.copyChildren(toReplace, replacement), this.fixedPositions);
    }

    @Override
    public String toString() {
        final String ts = Arrays.toString(this.getChildren());
        return '{' + ts.substring(1, ts.length() - 1) + '}';
    }

}
