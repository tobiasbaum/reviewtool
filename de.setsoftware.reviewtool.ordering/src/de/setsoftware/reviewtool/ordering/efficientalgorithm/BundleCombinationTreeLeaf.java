package de.setsoftware.reviewtool.ordering.efficientalgorithm;

import java.util.Collections;
import java.util.List;
import java.util.Set;

class BundleCombinationTreeLeaf<T> extends BundleCombinationTreeElement<T> {

    private final T value;
    private final BundleResult<T> thisFull;
    private final BundleResult<T> thisNone;

    BundleCombinationTreeLeaf(T value) {
        this.value = value;
        this.thisFull = new BundleResult<T>(ResultType.FULL, this);
        this.thisNone = new BundleResult<T>(ResultType.NONE, this);
    }

    @Override
    protected BundleResult<T> addBundle(Set<T> bundle) {
        return bundle.contains(this.value) ? this.thisFull : this.thisNone;
    }

    @Override
    protected void addItemsInOrder(List<T> buffer) {
        buffer.add(this.value);
    }

    @Override
    public String toString() {
        return this.value.toString();
    }

    @Override
    protected List<? extends BundleCombinationTreeElement<T>> split(Set<T> bundle) {
        return Collections.singletonList(this);
    }

    @Override
    protected ResultType checkContainment(Set<T> bundle) {
        return bundle.contains(this.value) ? ResultType.FULL : ResultType.NONE;
    }

    @Override
    protected BundleCombinationTreeElement<T> reverse() {
        return this;
    }

    @Override
    public PositionTreeLeaf<T> toPositionTree() {
        return new PositionTreeLeaf<T>(this.value);
    }

}
