package de.setsoftware.reviewtool.ordering.basealgorithm;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
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

    public static<T> void topoSort(List<T> items, PartialOrder<T> order) {
        //simple selection sort
        final List<T> newOrder = new ArrayList<>();
        while (!items.isEmpty()) {
            final Set<T> min = determineMinElements(items, order);
            items.removeAll(min);
            newOrder.addAll(min);
        }
        items.addAll(newOrder);
    }

}
