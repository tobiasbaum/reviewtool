package de.setsoftware.reviewtool.ordering.efficientalgorithm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * An intermediate node in the bundling tree. It comes in two sub-flavors, either with reordering of
 * the children allowed or forbidden.
 *
 * @param <T> Type of the stops.
 */
public class BundleCombinationTreeNode<T> extends BundleCombinationTreeElement<T> {

    private final BundleCombinationTreeElement<T>[] children;
    private final boolean reorderingAllowed;
    private final boolean reverseAllowed;

    BundleCombinationTreeNode(
            BundleCombinationTreeElement<T>[] children, boolean reorderingAllowed, boolean reverseAllowed) {
        assert children.length >= 2;
        this.children = children;
        this.reorderingAllowed = reorderingAllowed;
        this.reverseAllowed = reverseAllowed;
    }

    @Override
    protected BundleResult<T> addBundle(SimpleSet<T> bundle) {

        if (this.reorderingAllowed) {
            //add recursively and split by result type
            final Map<ResultType, List<BundleCombinationTreeElement<T>>> parts = new EnumMap<>(ResultType.class);
            final List<BundleCombinationTreeElement<T>> newChildren = new ArrayList<>();
            for (final ResultType r : ResultType.values()) {
                parts.put(r, new ArrayList<BundleCombinationTreeElement<T>>());
            }
            for (final BundleCombinationTreeElement<T> child : this.children) {
                final BundleResult<T> childResult = child.addBundle(bundle);
                if (childResult.getType() == ResultType.CONFLICT) {
                    return result(ResultType.CONFLICT, this);
                }
                parts.get(childResult.getType()).add(childResult.getTree());
                newChildren.add(childResult.getTree());
            }

            //check the different possible outcomes
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
                    this.reverseAllowed,
                    parts.get(ResultType.PARTIAL_BOTTOM),
                    parts.get(ResultType.FULL).isEmpty() ? this.empty() : li(ta(parts.get(ResultType.FULL))),
                    parts.get(ResultType.PARTIAL_TOP)
                );
            } else {
                matchSubtree = tf(
                    this.reverseAllowed,
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
                final BundleResult<T> childResult = child.addBundle(bundle);
                if (childResult.getType() == ResultType.CONFLICT) {
                    return result(ResultType.CONFLICT, this);
                }
                childResultsInOrder.add(childResult);
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
                    throw new AssertionError("should not happen, has been handled above");
                default:
                    throw new AssertionError("should not happen " + childResult.getType());
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
            return result(resultType, tf(this.reverseAllowed, newChildren));
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

    private void reverseAndSplitLast(List<BundleCombinationTreeElement<T>> newChildren, SimpleSet<T> bundle) {
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
            @SuppressWarnings("unchecked")
            final BundleCombinationTreeElement<S>[] elements = new BundleCombinationTreeElement[children.size()];
            return new BundleCombinationTreeNode<>(children.toArray(elements), true, true);
        }
    }

    @SafeVarargs
    private static<S> BundleCombinationTreeElement<S> tf(
            boolean reverseAllowed,
            List<? extends BundleCombinationTreeElement<S>>... childrenLists) {
        final List<BundleCombinationTreeElement<S>> combined = new ArrayList<>();
        for (final List<? extends BundleCombinationTreeElement<S>> list : childrenLists) {
            combined.addAll(list);
        }
        return tf(reverseAllowed, combined);
    }

    private static<S> BundleCombinationTreeElement<S> tf(
            boolean reverseAllowed,
            List<? extends BundleCombinationTreeElement<S>> children) {
        if (children.size() == 1) {
            return children.get(0);
        } else {
            @SuppressWarnings("unchecked")
            final BundleCombinationTreeElement<S>[] elements = new BundleCombinationTreeElement[children.size()];
            return new BundleCombinationTreeNode<>(children.toArray(elements), false, reverseAllowed);
        }
    }

    private static<S> BundleResult<S> result(ResultType type, BundleCombinationTreeElement<S> tree) {
        return new BundleResult<>(type, tree);
    }

    @Override
    public List<T> getPossibleOrder(Comparator<T> tieBreakingComparator) {
        final List<List<T>> subItems = new ArrayList<>();
        for (final BundleCombinationTreeElement<T> child : this.children) {
            subItems.add(child.getPossibleOrder(tieBreakingComparator));
        }
        if (this.reverseAllowed) {
            if (this.reorderingAllowed) {
                Collections.sort(subItems, new Comparator<List<T>>() {
                    @Override
                    public int compare(List<T> o1, List<T> o2) {
                        return tieBreakingComparator.compare(o1.get(0), o2.get(0));
                    }
                });
            } else {
                final int comparisonResult = tieBreakingComparator.compare(
                        subItems.get(0).get(0),
                        subItems.get(subItems.size() - 1).get(0));
                if (comparisonResult > 0) {
                    Collections.reverse(subItems);
                }
            }
        }
        final List<T> ret = new ArrayList<>();
        for (final List<T> subList : subItems) {
            ret.addAll(subList);
        }
        return ret;
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
            List<? extends BundleCombinationTreeElement<S>> list, SimpleSet<S> bundle) {
        assert list.size() <= 1;
        return list.isEmpty() ? Collections.<BundleCombinationTreeElement<S>>emptyList() : list.get(0).split(bundle);
    }

    @Override
    protected List<? extends BundleCombinationTreeElement<T>> split(SimpleSet<T> bundle) {
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
    protected ResultType checkContainment(SimpleSet<T> bundle) {
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
        return this.reverse(this.reverseAllowed);
    }

    private BundleCombinationTreeElement<T> reverse(boolean allowFurtherReversal) {
        if (!this.reverseAllowed) {
            throw new ReverseImpossibleException();
        }
        @SuppressWarnings("unchecked")
        final BundleCombinationTreeElement<T>[] copy = new BundleCombinationTreeElement[this.children.length];
        final int lastIndex = copy.length - 1;
        for (int i = 0; i <= lastIndex; i++) {
            copy[i] = this.children[lastIndex - i].reverse();
        }
        return new BundleCombinationTreeNode<>(copy, this.reorderingAllowed, allowFurtherReversal);
    }

    @Override
    protected BundleCombinationTreeElement<T> fixOrder(SimpleSet<T> center, SimpleSet<T> rest) {
        int minCenterIdx = Integer.MAX_VALUE;
        int maxCenterIdx = -1;
        int minRestIdx = Integer.MAX_VALUE;
        int maxRestIdx = -1;
        for (int i = 0; i < this.children.length; i++) {
            final BundleCombinationTreeElement<T> child = this.children[i];
            if (child.checkContainment(center) != ResultType.NONE) {
                minCenterIdx = Math.min(minCenterIdx, i);
                maxCenterIdx = Math.max(maxCenterIdx, i);
            }
            if (child.checkContainment(rest) != ResultType.NONE) {
                minRestIdx = Math.min(minRestIdx, i);
                maxRestIdx = Math.max(maxRestIdx, i);
            }
        }

        if (minCenterIdx == maxCenterIdx && minRestIdx == maxRestIdx && minCenterIdx == minRestIdx) {
            final BundleCombinationTreeElement<T> newChild = this.children[minCenterIdx].fixOrder(center, rest);
            if (newChild == null) {
                return null;
            } else {
                final BundleCombinationTreeElement<T>[] newChildren = Arrays.copyOf(this.children, this.children.length);
                newChildren[minCenterIdx] = newChild;
                return new BundleCombinationTreeNode<>(newChildren, this.reorderingAllowed, this.reverseAllowed);
            }
        } else if (maxCenterIdx < minRestIdx) {
            return new BundleCombinationTreeNode<>(this.children, this.reorderingAllowed, false);
        } else if (minCenterIdx > maxRestIdx) {
            if (this.reverseAllowed) {
                try {
                    return this.reverse(false);
                } catch (final ReverseImpossibleException e) {
                    return null;
                }
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

}
