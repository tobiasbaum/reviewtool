package de.setsoftware.reviewtool.ordering2.efficientalgorithm;

import java.util.List;

/**
 * The possible values where a value can be fixed relative to its match.
 *
 * The position fixing problem contains subset sum as a subproblem (which child sizes
 * of a reorderable node sum up to the intended position). By limiting the possible
 * positions, we avoid bad runtime behavior.
 */
public enum TargetPosition {
    FIRST {
        @Override
        public <T> T selectValue(List<T> order) {
            return order.get(0);
        }
    },
    SECOND {
        @Override
        public <T> T selectValue(List<T> order) {
            return order.get(1);
        }
    },
    LAST {
        @Override
        public <T> T selectValue(List<T> order) {
            return order.get(order.size() - 1);
        }
    };

    public abstract<T> T selectValue(List<T> order);
}