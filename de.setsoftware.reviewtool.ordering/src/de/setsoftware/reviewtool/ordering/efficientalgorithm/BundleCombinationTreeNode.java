package de.setsoftware.reviewtool.ordering.efficientalgorithm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An intermediate node in the bundling tree. It comes in two sub-flavors, either with reordering of
 * the children allowed or forbidden.
 *
 * @param <T> Type of the stops.
 */
public class BundleCombinationTreeNode<T> extends BundleCombinationTreeElement<T> {

    private final BundleCombinationTreeElement<T>[] children;
    private final boolean reorderingAllowed;

    BundleCombinationTreeNode(BundleCombinationTreeElement<T>[] children, boolean reorderingAllowed) {
        assert children.length >= 2;
        this.children = children;
        this.reorderingAllowed = reorderingAllowed;
    }

    @Override
    protected BundleResult<T> addBundle(Set<T> bundle) {

        if (this.reorderingAllowed) {
            //add recursively and split by result type
            final Map<ResultType, List<BundleCombinationTreeElement<T>>> parts = new EnumMap<>(ResultType.class);
            final List<BundleCombinationTreeElement<T>> newChildren = new ArrayList<>();
            for (final ResultType r : ResultType.values()) {
                parts.put(r, new ArrayList<BundleCombinationTreeElement<T>>());
            }
            for (final BundleCombinationTreeElement<T> child : this.children) {
                final BundleResult<T> childResult = child.addBundle(bundle);
                parts.get(childResult.getType()).add(childResult.getTree());
                newChildren.add(childResult.getTree());
            }

            //check the different possible outcomes
            if (!parts.get(ResultType.CONFLICT).isEmpty()) {
                return result(ResultType.CONFLICT, this);
            }
            final int atLeastPartialMatchCount = this.children.length - parts.get(ResultType.NONE).size();
            if (atLeastPartialMatchCount == 0) {
                return result(ResultType.NONE, this);
            }
            if (parts.get(ResultType.FULL).size() == this.children.length) {
                return result(ResultType.FULL, this);
            }
            final int partialMiddleCount = parts.get(ResultType.PARTIAL_MIDDLE).size();
            if (partialMiddleCount > 1) {
                return result(ResultType.CONFLICT, this);
            } else if (partialMiddleCount == 1) {
                if (atLeastPartialMatchCount > 1) {
                    return result(ResultType.CONFLICT, this);
                } else {
                    return result(ResultType.PARTIAL_MIDDLE, ta(newChildren));
                }
            }
            assert partialMiddleCount == 0;

            int partialTopCount = parts.get(ResultType.PARTIAL_TOP).size();
            int partialBottomCount = parts.get(ResultType.PARTIAL_BOTTOM).size();
            if (partialTopCount + partialBottomCount == 2) {
                if (partialTopCount == 0) {
                    partialTopCount++;
                    partialBottomCount--;
                    parts.get(ResultType.PARTIAL_TOP).add(
                            parts.get(ResultType.PARTIAL_BOTTOM).remove(0).reverse());
                } else if (partialBottomCount == 0) {
                    partialTopCount--;
                    partialBottomCount++;
                    parts.get(ResultType.PARTIAL_BOTTOM).add(
                            parts.get(ResultType.PARTIAL_TOP).remove(1).reverse());
                }
            }
            if (partialTopCount > 1) {
                return result(ResultType.CONFLICT, this);
            }
            if (partialBottomCount > 1) {
                return result(ResultType.CONFLICT, this);
            }

            ResultType resultType;
            if (partialBottomCount > 0) {
                if (partialTopCount > 0) {
                    resultType = ResultType.PARTIAL_MIDDLE;
                } else {
                    resultType = ResultType.PARTIAL_BOTTOM;
                }
            } else {
                if (partialTopCount > 0) {
                    resultType = ResultType.PARTIAL_TOP;
                } else {
                    resultType = this.children[0].checkContainment(bundle) != ResultType.NONE
                            ? ResultType.PARTIAL_TOP : ResultType.PARTIAL_BOTTOM;
                }
            }
            final boolean hasOnlyPartialBottom = partialBottomCount == atLeastPartialMatchCount;
            final boolean hasOnlyPartialTop = partialTopCount == atLeastPartialMatchCount;
            final boolean needsNoSplit = hasOnlyPartialBottom || hasOnlyPartialTop;
            final BundleCombinationTreeElement<T> matchSubtree;
            if (needsNoSplit) {
                matchSubtree = tf(
                    parts.get(ResultType.PARTIAL_BOTTOM),
                    parts.get(ResultType.FULL).isEmpty() ? this.empty() : li(ta(parts.get(ResultType.FULL))),
                    parts.get(ResultType.PARTIAL_TOP)
                );
            } else {
                matchSubtree = tf(
                    split(parts.get(ResultType.PARTIAL_BOTTOM), bundle),
                    parts.get(ResultType.FULL).isEmpty() ? this.empty() : li(ta(parts.get(ResultType.FULL))),
                    split(parts.get(ResultType.PARTIAL_TOP), bundle)
                );
            }
            return result(resultType,
                    ta(
                        resultType == ResultType.PARTIAL_BOTTOM ? parts.get(ResultType.NONE) : this.empty(),
                        li(matchSubtree),
                        resultType != ResultType.PARTIAL_BOTTOM ? parts.get(ResultType.NONE) : this.empty()));
        } else {
            final List<BundleResult<T>> childResultsInOrder = new ArrayList<>();
            for (final BundleCombinationTreeElement<T> child : this.children) {
                childResultsInOrder.add(child.addBundle(bundle));
            }
            final boolean containsMultipleMatches = this.containsMultipleMatches(childResultsInOrder);

            final List<BundleCombinationTreeElement<T>> newChildren = new ArrayList<>();
            boolean hadMatches = false;
            boolean lastEndedWithMatches = false;
            boolean lastCouldHaveEndedWithMatches = false;
            for (final BundleResult<T> childResult : childResultsInOrder) {
                switch (childResult.getType()) {
                case FULL:
                    if (!lastEndedWithMatches && hadMatches) {
                        if (lastCouldHaveEndedWithMatches) {
                            this.reverseAndSplitLast(newChildren, bundle);
                        } else {
                            return result(ResultType.CONFLICT, this);
                        }
                    }
                    hadMatches = true;
                    lastEndedWithMatches = true;
                    newChildren.add(childResult.getTree());
                    break;
                case NONE:
                    lastEndedWithMatches = false;
                    lastCouldHaveEndedWithMatches = false;
                    newChildren.add(childResult.getTree());
                    break;
                case PARTIAL_BOTTOM:
                    if (lastEndedWithMatches) {
                        newChildren.addAll(childResult.getTree().reverse().split(bundle));
                        lastEndedWithMatches = false;
                        lastCouldHaveEndedWithMatches = false;
                    } else if (hadMatches) {
                        if (lastCouldHaveEndedWithMatches) {
                            this.reverseAndSplitLast(newChildren, bundle);
                            newChildren.addAll(childResult.getTree().reverse().split(bundle));
                            lastEndedWithMatches = false;
                            lastCouldHaveEndedWithMatches = false;
                        } else {
                            return result(ResultType.CONFLICT, this);
                        }
                    } else {
                        hadMatches = true;
                        lastEndedWithMatches = true;
                        if (containsMultipleMatches) {
                            newChildren.addAll(childResult.getTree().split(bundle));
                        } else {
                            newChildren.add(childResult.getTree());
                        }
                    }
                    break;
                case PARTIAL_TOP:
                    if (hadMatches) {
                        if (!lastEndedWithMatches) {
                            if (lastCouldHaveEndedWithMatches) {
                                this.reverseAndSplitLast(newChildren, bundle);
                            } else {
                                return result(ResultType.CONFLICT, this);
                            }
                        }
                        lastCouldHaveEndedWithMatches = false;
                        newChildren.addAll(childResult.getTree().split(bundle));
                    } else {
                        hadMatches = true;
                        lastCouldHaveEndedWithMatches = true;
                        newChildren.add(childResult.getTree());
                    }
                    lastEndedWithMatches = false;
                    break;
                case PARTIAL_MIDDLE:
                    if (hadMatches) {
                        return result(ResultType.CONFLICT, this);
                    }
                    hadMatches = true;
                    lastEndedWithMatches = false;
                    lastCouldHaveEndedWithMatches = false;
                    newChildren.add(childResult.getTree());
                    break;
                case CONFLICT:
                    return result(ResultType.CONFLICT, this);
                default:
                    throw new RuntimeException("should not happen " + childResult.getType());
                }
            }

            ResultType firstChildContainment = newChildren.get(0).checkContainment(bundle);
            final ResultType secondChildContainment = newChildren.get(1).checkContainment(bundle);
            if (firstChildContainment == ResultType.PARTIAL_BOTTOM
                    && secondChildContainment == ResultType.NONE) {
                //if there is no need to have a partial bottom at the start, reverse it
                newChildren.set(0, newChildren.get(0).reverse());
                firstChildContainment = ResultType.PARTIAL_TOP;
            }
            final boolean startsWithMatches =
                    (firstChildContainment == ResultType.FULL
                    || firstChildContainment == ResultType.PARTIAL_TOP);
            ResultType resultType;
            if (!hadMatches) {
                resultType = ResultType.NONE;
            } else if (startsWithMatches && lastEndedWithMatches) {
                resultType = ResultType.FULL;
            } else if (startsWithMatches) {
                resultType = ResultType.PARTIAL_TOP;
            } else if (lastEndedWithMatches) {
                resultType = ResultType.PARTIAL_BOTTOM;
            } else {
                if (lastCouldHaveEndedWithMatches) {
                    final int lastIndex = newChildren.size() - 1;
                    newChildren.set(lastIndex, newChildren.get(lastIndex).reverse());
                    resultType = ResultType.PARTIAL_BOTTOM;
                } else {
                    resultType = ResultType.PARTIAL_MIDDLE;
                }
            }
            return result(resultType, tf(newChildren));
        }
    }

    private boolean containsMultipleMatches(List<BundleResult<T>> results) {
        int cnt = 0;
        for (final BundleResult<T> result : results) {
            if (result.getType() != ResultType.NONE) {
                cnt++;
                if (cnt >= 2) {
                    return true;
                }
            }
        }
        return false;
    }

    private void reverseAndSplitLast(List<BundleCombinationTreeElement<T>> newChildren, Set<T> bundle) {
        final int lastIndex = newChildren.size() - 1;
        final BundleCombinationTreeElement<T> element = newChildren.remove(lastIndex);
        newChildren.addAll(element.reverse().split(bundle));
    }

    private List<BundleCombinationTreeElement<T>> empty() {
        //not static because that makes generic type inference easier
        return Collections.emptyList();
    }

    private static<S> List<BundleCombinationTreeElement<S>> li(BundleCombinationTreeElement<S> child) {
        return Collections.singletonList(child);
    }

    @SafeVarargs
    private static<S> BundleCombinationTreeElement<S> ta(
            List<? extends BundleCombinationTreeElement<S>>... childrenLists) {
        final List<BundleCombinationTreeElement<S>> combined = new ArrayList<>();
        for (final List<? extends BundleCombinationTreeElement<S>> list : childrenLists) {
            combined.addAll(list);
        }
        return ta(combined);
    }

    private static<S> BundleCombinationTreeElement<S> ta(
            List<? extends BundleCombinationTreeElement<S>> children) {
        if (children.size() == 1) {
            return children.get(0);
        } else {
            return new BundleCombinationTreeNode<>(
                    children.toArray(new BundleCombinationTreeElement[children.size()]), true);
        }
    }

    @SafeVarargs
    private static<S> BundleCombinationTreeElement<S> tf(
            List<? extends BundleCombinationTreeElement<S>>... childrenLists) {
        final List<BundleCombinationTreeElement<S>> combined = new ArrayList<>();
        for (final List<? extends BundleCombinationTreeElement<S>> list : childrenLists) {
            combined.addAll(list);
        }
        return tf(combined);
    }

    private static<S> BundleCombinationTreeElement<S> tf(
            List<? extends BundleCombinationTreeElement<S>> children) {
        if (children.size() == 1) {
            return children.get(0);
        } else {
            return new BundleCombinationTreeNode<>(
                    children.toArray(new BundleCombinationTreeElement[children.size()]), false);
        }
    }

    private static<S> BundleResult<S> result(ResultType type, BundleCombinationTreeElement<S> tree) {
        return new BundleResult<>(type, tree);
    }

    @Override
    protected void addItemsInOrder(List<T> buffer) {
        for (final BundleCombinationTreeElement<T> e : this.children) {
            e.addItemsInOrder(buffer);
        }
    }

    @Override
    public String toString() {
        final String ts = Arrays.toString(this.children);
        if (this.reorderingAllowed) {
            return '{' + ts.substring(1, ts.length() - 1) + '}';
        } else {
            return ts;
        }
    }

    private static<S> List<? extends BundleCombinationTreeElement<S>> split(
            List<? extends BundleCombinationTreeElement<S>> list, Set<S> bundle) {
        assert list.size() <= 1;
        return list.isEmpty() ? Collections.<BundleCombinationTreeElement<S>>emptyList() : list.get(0).split(bundle);
    }

    @Override
    protected List<? extends BundleCombinationTreeElement<T>> split(Set<T> bundle) {
        if (this.reorderingAllowed) {
            final List<BundleCombinationTreeElement<T>> ret = new ArrayList<>();
            final List<BundleCombinationTreeElement<T>> temp = new ArrayList<>();
            ResultType previousContainment = null;
            for (final BundleCombinationTreeElement<T> child : this.children) {
                final ResultType containment = child.checkContainment(bundle);
                if (!containment.equals(previousContainment)) {
                    if (!temp.isEmpty()) {
                        ret.add(ta(temp));
                        temp.clear();
                    }
                    previousContainment = containment;
                }
                if (containment.isPartial()) {
                    ret.addAll(child.split(bundle));
                } else {
                    temp.add(child);
                }
            }
            if (!temp.isEmpty()) {
                ret.add(ta(temp));
            }
            return ret;
        } else {
            final List<BundleCombinationTreeElement<T>> ret = new ArrayList<>();
            for (final BundleCombinationTreeElement<T> child : this.children) {
                if (child.checkContainment(bundle).isPartial()) {
                    ret.addAll(child.split(bundle));
                } else {
                    ret.add(child);
                }
            }
            return ret;
        }
    }

    @Override
    protected ResultType checkContainment(Set<T> bundle) {
        final boolean startsWithMatch = this.children[0].checkContainment(bundle).hasTopMatch();
        final boolean endsWithMatch = this.children[this.children.length - 1].checkContainment(bundle).hasBottomMatch();
        if (startsWithMatch && !endsWithMatch) {
            return ResultType.PARTIAL_TOP;
        }
        if (endsWithMatch && !startsWithMatch) {
            return ResultType.PARTIAL_BOTTOM;
        }
        if (startsWithMatch && endsWithMatch) {
            //we don't check for conflicts here
            return ResultType.FULL;
        }
        for (final BundleCombinationTreeElement<T> child : this.children) {
            final ResultType childContainment = child.checkContainment(bundle);
            if (childContainment != ResultType.NONE) {
                return ResultType.PARTIAL_MIDDLE;
            }
        }
        return ResultType.NONE;
    }

    @Override
    protected BundleCombinationTreeElement<T> reverse() {
        final BundleCombinationTreeElement<T>[] copy = new BundleCombinationTreeElement[this.children.length];
        final int lastIndex = copy.length - 1;
        for (int i = 0; i <= lastIndex; i++) {
            copy[i] = this.children[lastIndex - i].reverse();
        }
        return new BundleCombinationTreeNode<>(copy, this.reorderingAllowed);
    }

    @Override
    public PositionTreeNode<T> toPositionTree() {
        final PositionTreeElement<T>[] positionChildren = new PositionTreeElement[this.children.length];
        for (int i = 0; i < this.children.length; i++) {
            positionChildren[i] = this.children[i].toPositionTree();
        }
        if (this.reorderingAllowed) {
            return new PositionTreeNodeReorderable<>(positionChildren);
        } else {
            return new PositionTreeNodeFixedOrder<>(positionChildren);
        }
    }

}
