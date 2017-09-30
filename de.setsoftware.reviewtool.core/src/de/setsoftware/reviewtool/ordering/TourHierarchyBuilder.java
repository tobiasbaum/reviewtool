package de.setsoftware.reviewtool.ordering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.setsoftware.reviewtool.model.changestructure.Stop;
import de.setsoftware.reviewtool.model.changestructure.Tour;
import de.setsoftware.reviewtool.model.changestructure.TourElement;

/**
 * Helper class to incrementally transform the linear stop sequence into
 * a hierarchical structure with sub-tours.
 */
class TourHierarchyBuilder {

    /**
     * A linked list representation of a tree is used to build the tour structure.
     * This is the class for the nodes.
     */
    private static final class TreeNode {
        private final int minIndex;
        private final int maxIndex;
        private TreeNode sibling;
        private TreeNode firstChild;
        private final String description;

        public TreeNode(int minIndex, int maxIndex, TreeNode sibling, TreeNode firstChild, String description) {
            this.minIndex = minIndex;
            this.maxIndex = maxIndex;
            this.sibling = sibling;
            this.firstChild = firstChild;
            this.description = description;
        }

        public boolean isLeaf() {
            return this.minIndex == this.maxIndex;
        }
    }

    private final List<Stop> stops;
    private final Map<Stop, Integer> indexMap;
    private TreeNode firstRoot;

    public TourHierarchyBuilder(List<Stop> sorted) {
        this.stops = sorted;

        this.indexMap = new HashMap<>();
        TreeNode cur = null;
        for (int i = sorted.size() - 1; i >= 0; i--) {
            cur = new TreeNode(i, i, cur, null, null);
            this.indexMap.put(sorted.get(i), i);
        }
        this.firstRoot = cur;
    }

    public void createSubtourIfPossible(OrderingInfo o) {
        int minIndex = Integer.MAX_VALUE;
        int maxIndex = Integer.MIN_VALUE;

        for (final Stop s : o.getMatchSet().getChangeParts()) {
            minIndex = Math.min(minIndex, this.indexMap.get(s));
            maxIndex = Math.max(maxIndex, this.indexMap.get(s));
        }

        if (maxIndex - minIndex != o.getMatchSet().getChangeParts().size() - 1) {
            //match is not adjacent
            return;
        }

        TreeNode upper = null;
        TreeNode prev = null;
        TreeNode cur = this.firstRoot;
        while (cur != null) {
            if (cur.minIndex <= minIndex && cur.maxIndex >= maxIndex) {
                //match is fully contained in current node, step down in tree
                upper = cur;
                prev = null;
                cur = cur.firstChild;
            } else if (cur.minIndex == minIndex) {
                //current node contains start of sequence, find end
                final TreeNode end = this.findSiblingWithMaxIndex(cur, maxIndex);
                if (end == null) {
                    //no match found, cannot add node
                    return;
                }
                if (prev == null && end.sibling == null) {
                    //don't create trivial nestings
                    return;
                }
                final TreeNode newNode = new TreeNode(minIndex, maxIndex, end.sibling, cur, o.getDescription());
                end.sibling = null;
                if (prev != null) {
                    prev.sibling = newNode;
                } else if (upper != null) {
                    upper.firstChild = newNode;
                } else {
                    this.firstRoot = newNode;
                }
                //done
                return;
            } else {
                prev = cur;
                cur = cur.sibling;
            }
        }
    }

    private TreeNode findSiblingWithMaxIndex(TreeNode node, int maxIndex) {
        for (TreeNode cur = node; cur != null; cur = cur.sibling) {
            if (cur.maxIndex == maxIndex) {
                return cur;
            }
        }
        return null;
    }

    public List<? extends TourElement> getTopmostElements() {
        return this.toTourElements(this.firstRoot);
    }

    private List<? extends TourElement> toTourElements(TreeNode node) {
        final List<TourElement> ret = new ArrayList<>();
        for (TreeNode cur = node; cur != null; cur = cur.sibling) {
            if (cur.isLeaf()) {
                ret.add(this.stops.get(cur.minIndex));
            } else {
                ret.add(new Tour(cur.description, this.toTourElements(cur.firstChild)));
            }
        }
        return ret;
    }

}
