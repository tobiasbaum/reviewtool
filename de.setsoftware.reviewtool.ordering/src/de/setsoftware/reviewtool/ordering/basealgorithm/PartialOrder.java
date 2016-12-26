package de.setsoftware.reviewtool.ordering.basealgorithm;

/**
 * Interface for implementations of the mathematical concept of a partial order.
 * @param<T> The type of the items to be compared.
 */
public interface PartialOrder<T> {

    /**
     * Returns true iff value1 is less or equal to value2 in this order.
     */
    public abstract boolean isLessOrEquals(T value1, T value2);

}
