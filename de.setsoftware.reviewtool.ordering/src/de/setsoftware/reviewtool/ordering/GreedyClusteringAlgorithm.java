package de.setsoftware.reviewtool.ordering;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.setsoftware.reviewtool.ordering.basealgorithm.PartialOrder;
import de.setsoftware.reviewtool.ordering.basealgorithm.PartialOrders;
import de.setsoftware.reviewtool.ordering.basealgorithm.Tour;

public class GreedyClusteringAlgorithm {

    private static final class CompositeClusterOrder<S> implements PartialOrder<ClusterItem<S>> {

        private final List<PartialOrder<S>> baseOrders;

        public CompositeClusterOrder(List<PartialOrder<S>> baseOrders) {
            this.baseOrders = baseOrders;
        }

        @Override
        public boolean isLessOrEquals(ClusterItem<S> value1, ClusterItem<S> value2) {
            //handle equality as a special case
            if (value1.equals(value2)) {
                return true;
            }

            for (final PartialOrder<S> baseOrder : this.baseOrders) {
                final Boolean resultFromBase = this.compareStops(baseOrder, value1, value2);
                if (resultFromBase != null) {
                    //null means incomparable here
                    return resultFromBase;
                }
            }
            return false;
        }

        private Boolean compareStops(PartialOrder<S> baseOrder, ClusterItem<S> value1, ClusterItem<S> value2) {
            int count1leq2 = 0;
            int count2leq1 = 0;

            for (final S s1 : value1.getStopsInOrder()) {
                for (final S s2 : value2.getStopsInOrder()) {
                    if (baseOrder.isLessOrEquals(s1, s2)) {
                        count1leq2++;
                    }
                    if (baseOrder.isLessOrEquals(s2, s1)) {
                        count2leq1++;
                    }
                }
            }

            if (count1leq2 == 0 && count2leq1 == 0) {
                //all stops incomparable: return null as special marker value
                return null;
            } else {
                final int diff = count1leq2 - count2leq1;
                return diff >= 0;
            }
        }

    }

    private abstract static class ClusterItem<S> {

        private Cluster<S> parent;

        public ClusterItem(Cluster<S> parent) {
            this.parent = parent;
            if (parent != null) {
                parent.addChildHelper(this);
            }
        }

        public abstract List<S> getStopsInOrder();

        public abstract void clusterSelfAndChildren(RelatednessReason r);

        public abstract void sortItemsInSelfAndChildren(RelatednessInfo<S> info);

        public abstract Set<? extends RelatednessReason> getRelatednessReasons();

        public void changeParent(Cluster<S> newParent) {
            this.parent.removeChildHelper(this);
            this.parent = newParent;
            this.parent.addChildHelper(this);
        }
    }

    private static final class Cluster<S> extends ClusterItem<S> {

        private final List<ClusterItem<S>> items;

        public Cluster(Cluster<S> parent) {
            super(parent);
            this.items = new ArrayList<>();
        }

        public void addChildHelper(ClusterItem<S> clusterItem) {
            this.items.add(clusterItem);
        }

        public void removeChildHelper(ClusterItem<S> clusterItem) {
            this.items.remove(clusterItem);
        }

        @Override
        public List<S> getStopsInOrder() {
            final List<S> ret = new ArrayList<>();
            for (final ClusterItem<S> item : this.items) {
                ret.addAll(item.getStopsInOrder());
            }
            return ret;
        }

        @Override
        public Set<? extends RelatednessReason> getRelatednessReasons() {
            final Set<RelatednessReason> ret = new LinkedHashSet<>();
            for (final ClusterItem<S> item : this.items) {
                ret.addAll(item.getRelatednessReasons());
            }
            return ret;
        }

        @Override
        public void clusterSelfAndChildren(RelatednessReason r) {
            final List<ClusterItem<S>> toCluster = new ArrayList<>();
            for (final ClusterItem<S> item : this.items) {
                if (item.getRelatednessReasons().contains(r)) {
                    item.clusterSelfAndChildren(r);
                    toCluster.add(item);
                }
            }
            if (toCluster.size() > 1 && toCluster.size() < this.items.size()) {
                final Cluster<S> newCluster = new Cluster<>(this);
                for (final ClusterItem<S> item : toCluster) {
                    item.changeParent(newCluster);
                }
            }
        }

        @Override
        public void sortItemsInSelfAndChildren(RelatednessInfo<S> info) {
            for (final ClusterItem<S> item : this.items) {
                item.sortItemsInSelfAndChildren(info);
            }
            this.sortItemsInSelf(info);
        }

        private void sortItemsInSelf(RelatednessInfo<S> info) {
            final Set<RelatednessReason> commonReasons = this.getCommonReasons();
            final List<RelatednessReason> sortedReasons = sortByImportance(commonReasons);
            final List<PartialOrder<S>> orders = this.mapToOrders(sortedReasons, info);

            final PartialOrder<ClusterItem<S>> compositeClusterOrder = new CompositeClusterOrder<S>(orders);
            PartialOrders.topoSort(this.items, compositeClusterOrder);
        }

        private List<PartialOrder<S>> mapToOrders(List<RelatednessReason> sortedReasons, RelatednessInfo<S> info) {
            final List<PartialOrder<S>> ret = new ArrayList<>();
            for (final RelatednessReason reason: sortedReasons) {
                ret.add(info.getOrderFor(reason));
            }
            return ret;
        }

        private Set<RelatednessReason> getCommonReasons() {
            final Set<RelatednessReason> commonReasons = new LinkedHashSet<>(this.items.get(0).getRelatednessReasons());
            for (int i = 1; i < this.items.size(); i++) {
                commonReasons.retainAll(this.items.get(i).getRelatednessReasons());
            }
            return commonReasons;
        }

    }

    private static final class ClusterLeaf<S> extends ClusterItem<S> {

        private final S stop;
        private final Set<? extends RelatednessReason> reasons;

        public ClusterLeaf(Cluster<S> parent, S stop, Set<? extends RelatednessReason> reasons) {
            super(parent);
            this.stop = stop;
            this.reasons = reasons;
        }

        @Override
        public List<S> getStopsInOrder() {
            return Collections.singletonList(this.stop);
        }

        @Override
        public void clusterSelfAndChildren(RelatednessReason r) {
        }

        @Override
        public void sortItemsInSelfAndChildren(RelatednessInfo<S> info) {
        }

        @Override
        public Set<? extends RelatednessReason> getRelatednessReasons() {
            return this.reasons;
        }

    }

    public static<S, R extends Comparable<R>> Tour<S> determineBestTour(
            Set<S> stops, RelatednessInfo<S> info) {

        final Map<S, ClusterLeaf<S>> lookupMap = new HashMap<>();
        final Cluster<S> root = createClusterWrappers(stops, info, lookupMap);

        final Set<? extends RelatednessReason> allReasons = root.getRelatednessReasons();
        final List<RelatednessReason> sortedReasons = sortByImportance(allReasons);
        for (final RelatednessReason r : sortedReasons) {
            root.clusterSelfAndChildren(r);
        }

        root.sortItemsInSelfAndChildren(info);

        return Tour.of(root.getStopsInOrder());
    }

    private static List<RelatednessReason> sortByImportance(Set<? extends RelatednessReason> allReasons) {
        final List<RelatednessReason> ret = new ArrayList<>(allReasons);
        Collections.sort(ret, new Comparator<RelatednessReason>() {

            @Override
            public int compare(RelatednessReason o1, RelatednessReason o2) {
                final int importance1 = o1.getType().getImportanceCode();
                final int importance2 = o2.getType().getImportanceCode();
                final int importanceCmp = Integer.compare(importance1, importance2);
                if (importanceCmp != 0) {
                    return importanceCmp;
                }
                return o1.getId().compareTo(o2.getId());
            }
        });
        return ret;
    }

    private static<S> Cluster<S> createClusterWrappers(
            Set<S> stops, RelatednessInfo<S> info, Map<S, ClusterLeaf<S>> lookupMap) {
        final Cluster<S> root = new Cluster<>(null);
        for (final S stop : stops) {
            lookupMap.put(stop, new ClusterLeaf<>(root, stop, info.getReasonsFor(stop)));
        }
        return root;
    }

}
