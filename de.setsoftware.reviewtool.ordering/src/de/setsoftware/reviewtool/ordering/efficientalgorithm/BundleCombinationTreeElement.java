package de.setsoftware.reviewtool.ordering.efficientalgorithm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Superclass for elements of the bundle combination tree.
 *
 * @param <T> The type of stops.
 */
public abstract class BundleCombinationTreeElement<T> implements BundleCombination<T> {

    /**
     * The different possibilites for a set of stops to be contained in the sequence.
     */
    protected enum ResultType {
        NONE {
            @Override
            public boolean hasTopMatch() {
                return false;
            }

            @Override
            public boolean hasBottomMatch() {
                return false;
            }

            @Override
            public boolean isPartial() {
                return false;
            }
        },
        FULL {
            @Override
            public boolean hasTopMatch() {
                return true;
            }

            @Override
            public boolean hasBottomMatch() {
                return true;
            }

            @Override
            public boolean isPartial() {
                return false;
            }
        },
        PARTIAL_TOP {
            @Override
            public boolean hasTopMatch() {
                return true;
            }

            @Override
            public boolean hasBottomMatch() {
                return false;
            }

            @Override
            public boolean isPartial() {
                return true;
            }
        },
        PARTIAL_BOTTOM {
            @Override
            public boolean hasTopMatch() {
                return false;
            }

            @Override
            public boolean hasBottomMatch() {
                return true;
            }

            @Override
            public boolean isPartial() {
                return true;
            }
        },
        PARTIAL_MIDDLE {
            @Override
            public boolean hasTopMatch() {
                return false;
            }

            @Override
            public boolean hasBottomMatch() {
                return false;
            }

            @Override
            public boolean isPartial() {
                return true;
            }
        },
        CONFLICT {
            @Override
            public boolean hasTopMatch() {
                return false;
            }

            @Override
            public boolean hasBottomMatch() {
                return false;
            }

            @Override
            public boolean isPartial() {
                return false;
            }
        };

        public abstract boolean hasTopMatch();

        public abstract boolean hasBottomMatch();

        public abstract boolean isPartial();
    }

    /**
     * Helper class for the results of a bundling step.
     * @param <S> Type of the stops.
     */
    protected static final class BundleResult<S> {
        private final BundleCombinationTreeElement<S> newTree;
        private final ResultType resultType;

        public BundleResult(ResultType resultType, BundleCombinationTreeElement<S> newTree) {
            this.newTree = newTree;
            this.resultType = resultType;
        }

        public ResultType getType() {
            return this.resultType;
        }

        public BundleCombinationTreeElement<S> getTree() {
            return this.newTree;
        }

        @Override
        public String toString() {
            return this.resultType + " " + this.newTree;
        }
    }

    /**
     * Creates a tree element for the given list of stops. Depending on the size of a list it is
     * either directly a leaf or a node with the stops as children.
     */
    public static<S> BundleCombinationTreeElement<S> create(Collection<S> asList) {
        assert asList.size() > 0;
        if (asList.size() == 1) {
            return new BundleCombinationTreeLeaf<S>(asList.iterator().next());
        } else {
            final BundleCombinationTreeElement<S>[] items = new BundleCombinationTreeElement[asList.size()];
            final Iterator<S> iter = asList.iterator();
            int i = 0;
            while (iter.hasNext()) {
                items[i] = new BundleCombinationTreeLeaf<S>(iter.next());
                i++;
            }
            return new BundleCombinationTreeNode<S>(items, true);
        }
    }

    @Override
    public final BundleCombinationTreeElement<T> bundle(SimpleSet<T> bundle) {
        final BundleResult<T> result = this.addBundle(bundle);
        if (result.resultType == ResultType.CONFLICT) {
            return null;
        } else {
            return result.newTree;
        }
    }

    protected abstract BundleResult<T> addBundle(SimpleSet<T> bundle);

    @Override
    public final List<T> getPossibleOrder() {
        final List<T> ret = new ArrayList<>();
        this.addItemsInOrder(ret);
        return ret;
    }

    protected abstract void addItemsInOrder(List<T> buffer);

    protected abstract List<? extends BundleCombinationTreeElement<T>> split(SimpleSet<T> bundle);

    protected abstract ResultType checkContainment(SimpleSet<T> bundle);

    protected abstract BundleCombinationTreeElement<T> reverse();

    @Override
    public final boolean equals(Object o) {
        //equals for tests, not really nice
        if (!(o instanceof BundleCombinationTreeElement)) {
            return false;
        }
        return this.toString().equals(o.toString());
    }

    @Override
    public final int hashCode() {
        return 42;
    }

    public abstract PositionTreeElement<T> toPositionTree();

}
