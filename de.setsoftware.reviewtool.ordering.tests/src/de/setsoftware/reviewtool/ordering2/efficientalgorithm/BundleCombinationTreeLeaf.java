package de.setsoftware.reviewtool.ordering2.efficientalgorithm;

import java.util.Collections;
import java.util.List;
import java.util.Set;

class BundleCombinationTreeLeaf<T> extends BundleCombinationTreeElement<T> {

    private final T value;

    BundleCombinationTreeLeaf(T value) {
        this.value = value;
    }

    @Override
    protected BundleResult<T> addBundle(Set<T> bundle) {
        return new BundleResult<T>(bundle.contains(this.value) ? ResultType.FULL : ResultType.NONE, this);
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

}
