package de.setsoftware.reviewtool.ordering2.efficientalgorithm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PositionTreeNodeFixedOrder<T> extends PositionTreeNode<T> {

    private static final class Fix<S> {
        private final S value;
        private final int positionRelativeToMatch;
        private final Set<S> match;

        public Fix(Set<S> match, S toFix, int position) {
            this.match = match;
            this.value = toFix;
            this.positionRelativeToMatch = position;
        }

        public int determineExpectedAbsolutePosition(PositionTreeElement<S>[] children) {
            return this.countNonMatchPrefix(children) + this.positionRelativeToMatch;
        }

        private int countNonMatchPrefix(PositionTreeElement<S>[] children) {
            int ret = 0;
            for (final PositionTreeElement<S> child : children) {
                final Set<S> valuesInChild = child.getValuesInSubtree();
                if (Collections.disjoint(this.match, valuesInChild)) {
                    ret += valuesInChild.size();
                } else {
                    assert this.match.containsAll(valuesInChild);
                    break;
                }
            }
            return ret;
        }

    }

    private List<Fix<T>> fixedPositions;

    PositionTreeNodeFixedOrder(PositionTreeElement<T>[] children) {
        this(children, Collections.<Fix<T>>emptyList());
    }

    PositionTreeNodeFixedOrder(PositionTreeElement<T>[] children, List<Fix<T>> fixedPositions) {
        super(children);
        this.fixedPositions = fixedPositions;
    }

    @Override
    public PositionTreeNodeFixedOrder<T> moveValueToPosition(
            Set<T> match, T toFix, TargetPosition position, boolean alsoFix) {
        //TODO recursive fixing
        final Set<T> relevantMatch = new HashSet<>(match);
        relevantMatch.retainAll(this.getValuesInSubtree());

        int expectedIndex;
        switch (position) {
        case FIRST:
            expectedIndex = 0;
            break;
        case SECOND:
            expectedIndex = 1;
            break;
        case LAST:
            expectedIndex = relevantMatch.size() - 1;
            break;
        default:
            throw new AssertionError("invalid enum value " + position);
        }

        final List<Fix<T>> fixesToCheck = new ArrayList<>(this.fixedPositions);
        fixesToCheck.add(new Fix<>(relevantMatch, toFix, expectedIndex));

        final List<Fix<T>> newFixes;
        if (alsoFix) {
            newFixes = fixesToCheck;
        } else {
            newFixes = this.fixedPositions;
        }

        final PositionTreeElement<T>[] copies = this.copyOfChildren();
        if (this.adjustChildrenAndCheckIfSatisfiesFixes(copies, fixesToCheck)) {
            return new PositionTreeNodeFixedOrder<>(copies, newFixes);
        }

        //TODO is there a need to reverse recursively?
        final PositionTreeElement<T>[] copiesReverse = this.copyOfChildren();
        Collections.reverse(Arrays.asList(copiesReverse));
        if (this.adjustChildrenAndCheckIfSatisfiesFixes(copiesReverse, fixesToCheck)) {
            return new PositionTreeNodeFixedOrder<>(copiesReverse, newFixes);
        }

        return null;
    }

    private PositionTreeElement<T>[] copyOfChildren() {
        return Arrays.copyOf(this.getChildren(), this.getChildren().length);
    }

    private boolean adjustChildrenAndCheckIfSatisfiesFixes(PositionTreeElement<T>[] children, List<Fix<T>> fixes) {
        final Map<Fix<T>, Integer> indices = new HashMap<>();
        for (final Fix<T> fix : fixes) {
            indices.put(fix, fix.determineExpectedAbsolutePosition(children));
        }
        Collections.sort(fixes, new Comparator<Fix<T>>() {
            @Override
            public int compare(Fix<T> o1, Fix<T> o2) {
                return Integer.compare(indices.get(o1), indices.get(o2));
            }
        });

        int childIndex = 0;
        int startIndex = 0;
        for (final Fix<T> fix : fixes) {
            final int fixIndex = indices.get(fix);
            int childSize = children[childIndex].getValuesInSubtree().size();
            while (fixIndex >= startIndex + childSize) {
                startIndex += childSize;
                childIndex++;
                if (childIndex >= children.length) {
                    return false;
                }
                childSize = children[childIndex].getValuesInSubtree().size();
            }
            if (!children[childIndex].getValuesInSubtree().contains(fix.value)) {
                return false;
            }
            TargetPosition position;
            if (fixIndex - startIndex == 0) {
                position = TargetPosition.FIRST;
            } else if (fixIndex - startIndex == 1) {
                position = TargetPosition.SECOND;
            } else if (fixIndex - startIndex == childSize - 1) {
                position = TargetPosition.LAST;
            } else {
                throw new AssertionError("tree invalid " + this);
            }
            final PositionTreeElement<T> replacement = children[childIndex].moveValueToPosition(
                    fix.match, fix.value, position, false);
            if (replacement == null) {
                return false;
            }
            children[childIndex] = replacement;
        }

        final List<T> values = new ArrayList<>();
        for (final PositionTreeElement<T> child : children) {
            child.addItemsInOrder(values);
        }
        for (final Fix<T> fix : fixes) {
            if (!values.get(indices.get(fix)).equals(fix.value)) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected PositionTreeElement<T> copyReplacing(PositionTreeElement<T> toReplace,
            PositionTreeElement<T> replacement) {
        if (toReplace == this) {
            return replacement;
        }
        return new PositionTreeNodeFixedOrder<T>(this.copyChildren(toReplace, replacement), this.fixedPositions);
    }

    @Override
    public String toString() {
        return Arrays.toString(this.getChildren());
    }

}
