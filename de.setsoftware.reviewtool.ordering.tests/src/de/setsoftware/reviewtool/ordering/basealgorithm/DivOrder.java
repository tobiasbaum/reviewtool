package de.setsoftware.reviewtool.ordering.basealgorithm;

/**
 * Partial order for testing, based on divisibility of integers.
 */
public class DivOrder implements PartialOrder<Integer> {

    @Override
    public boolean isLessOrEquals(Integer value1, Integer value2) {
        return value2 % value1 == 0;
    }

}
