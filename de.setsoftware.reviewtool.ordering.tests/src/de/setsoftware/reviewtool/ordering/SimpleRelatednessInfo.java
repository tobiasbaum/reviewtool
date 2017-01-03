package de.setsoftware.reviewtool.ordering;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import de.setsoftware.reviewtool.ordering.basealgorithm.PartialOrder;
import de.setsoftware.reviewtool.ordering.relationtypes.RelationType;

public class SimpleRelatednessInfo extends RelatednessInfo<String> {

    private static final class OrderCodeDivisibilityOrder implements PartialOrder<String> {

        private final Map<String, Integer> orderCodes;

        public OrderCodeDivisibilityOrder(Map<String, Integer> orderCodes) {
            this.orderCodes = orderCodes;
        }

        @Override
        public boolean isLessOrEquals(String value1, String value2) {
            final Integer orderCode1 = this.orderCodes.get(value1);
            final Integer orderCode2 = this.orderCodes.get(value2);
            if (orderCode1 == null || orderCode2 == null) {
                return false;
            }
            return orderCode2 % orderCode1 == 0;
        }

    }

    private static final class SimpleReasonType extends RelatednessReasonType {

        private final RelationType id;
        private final int importanceCode;

        public SimpleReasonType(RelationType type, int index) {
            this.id = type;
            this.importanceCode = index;
        }

        @Override
        public int getImportanceCode() {
            return this.importanceCode;
        }

        @Override
        public int hashCode() {
            return this.id.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof SimpleReasonType)) {
                return false;
            }
            final SimpleReasonType t = (SimpleReasonType) o;
            return this.id.equals(t.id);
        }

    }

    private final Map<String, Set<RelatednessReason>> reasons;
    private final Map<RelatednessReason, PartialOrder<String>> orders;
    private final RelationType[] relationTypePreferences;

    public SimpleRelatednessInfo(Set<String> stops, RelationType... relationTypePreferences) {
        this.reasons = new LinkedHashMap<>();
        for (final String stop : stops) {
            this.reasons.put(stop, new LinkedHashSet<RelatednessReason>());
        }
        this.relationTypePreferences = relationTypePreferences;
        this.orders = new HashMap<>();
    }

    public void addReason(RelationType type, String id, String... relatedStopsAndOrderCodes) {
        final RelatednessReasonType mappedType = this.mapType(type);
        if (mappedType == null) {
            return;
        }
        final RelatednessReason reason = new RelatednessReason(mappedType, id);
        final Map<String, Integer> orderCodes = new HashMap<>();
        for (final String stopAndOrderCode : relatedStopsAndOrderCodes) {
            final String[] split = stopAndOrderCode.split("\\|");
            final String stop = split[0];
            final int orderCode = Integer.parseInt(split[1]);
            this.reasons.get(stop).add(reason);
            orderCodes.put(stop, orderCode);
        }
        assert !this.orders.containsKey(reason);
        this.orders.put(reason, new OrderCodeDivisibilityOrder(orderCodes));
    }

    private RelatednessReasonType mapType(RelationType type) {
        final int index = Arrays.asList(this.relationTypePreferences).indexOf(type);
        return index < 0 ? null : new SimpleReasonType(type, index);
    }

    @Override
    public Set<? extends RelatednessReason> getReasonsFor(String stop) {
        return this.reasons.get(stop);
    }

    @Override
    public PartialOrder<String> getOrderFor(RelatednessReason reason) {
        return this.orders.get(reason);
    }

    public Set<String> getStops() {
        return this.reasons.keySet();
    }

}
