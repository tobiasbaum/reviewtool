package de.setsoftware.reviewtool.ordering.basealgorithm;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

public class PartialOrders {

    public static<T> Set<T> determineMinElements(Iterable<T> allElements, PartialOrder<T> order) {
        final Set<T> minElements = new LinkedHashSet<>();
        for (final T cur : allElements) {
            final Iterator<T> minIter = minElements.iterator();
            boolean add = true;
            while (minIter.hasNext()) {
                final T curMin = minIter.next();
                if (order.isLessOrEquals(curMin, cur)) {
                    add = false;
                    break;
                } else if (order.isLessOrEquals(cur, curMin)) {
                    minIter.remove();
                }
            }
            if (add) {
                minElements.add(cur);
            }
        }
        return minElements;
    }

}
