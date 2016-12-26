package de.setsoftware.reviewtool.ordering.basealgorithm;

public class NaturalOrder<T extends Comparable<T>> implements PartialOrder<T> {

    @Override
    public boolean isLessOrEquals(T value1, T value2) {
        return value1.compareTo(value2) <= 0;
    }

}
