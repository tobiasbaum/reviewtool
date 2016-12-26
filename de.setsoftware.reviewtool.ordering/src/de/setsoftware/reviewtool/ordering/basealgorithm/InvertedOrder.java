package de.setsoftware.reviewtool.ordering.basealgorithm;

public class InvertedOrder<T> implements PartialOrder<T> {

    private final PartialOrder<T> decorated;

    public InvertedOrder(PartialOrder<T> decorated) {
        this.decorated = decorated;
    }

    @Override
    public boolean isLessOrEquals(T value1, T value2) {
        return this.decorated.isLessOrEquals(value2, value1);
    }

}
