package de.setsoftware.reviewtool.ordering.basealgorithm;

public class LexicographicOrder<T> implements PartialOrder<T[]> {

    private final PartialOrder<T> itemOrder;

    public LexicographicOrder(PartialOrder<T> itemOrder) {
        this.itemOrder = itemOrder;
    }

    @Override
    public boolean isLessOrEquals(T[] value1, T[] value2) {
        for (int i = 0; i < value1.length; i++) {
            if (i >= value2.length) {
                return false;
            }
            final T v1 = value1[i];
            final T v2 = value2[i];
            if (this.itemOrder.isLessOrEquals(v1, v2)) {
                if (!this.itemOrder.isLessOrEquals(v2, v1)) {
                    return true;
                }
            } else {
                return false;
            }
        }
        return true;
    }

}
