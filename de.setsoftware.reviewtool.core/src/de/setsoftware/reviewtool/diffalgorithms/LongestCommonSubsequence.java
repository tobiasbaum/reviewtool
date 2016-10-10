package de.setsoftware.reviewtool.diffalgorithms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.setsoftware.reviewtool.base.Multiset;

/**
 * Helper class containing the algorithm to calculate the longest common subsequence.
 */
class LongestCommonSubsequence {

    private static final int CUTOFF_LIMIT = 1000000;

    /**
     * Helper class for making the recursive algorithm iterative.
     */
    private static class WorkItem {
        private final int pos1;
        private final int pos2;
        private final int sum;

        public WorkItem(int pos1, int pos2, int sum) {
            this.pos1 = pos1;
            this.pos2 = pos2;
            this.sum = sum;
        }

        public WorkItem up() {
            return new WorkItemU(this.pos1, this.pos2 - 1, this.sum + 1);
        }

        public WorkItem left() {
            return new WorkItemL(this.pos1 - 1, this.pos2, this.sum + 1);
        }

        public WorkItem same() {
            return new WorkItemS(this.pos1 - 1, this.pos2 - 1, this.sum);
        }

        public WorkItem diagonal() {
            return new WorkItemD(this.pos1 - 1, this.pos2 - 1, this.sum + 1);
        }

        @Override
        public int hashCode() {
            return this.pos1 ^ Integer.reverseBytes(this.pos2);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof WorkItem)) {
                return false;
            }
            final WorkItem w = (WorkItem) o;
            return w.pos1 == this.pos1 && w.pos2 == this.pos2;
        }
    }

    /**
     * An work item resulting from going to the left.
     */
    private static class WorkItemL extends WorkItem {

        public WorkItemL(int pos1, int pos2, int sum) {
            super(pos1, pos2, sum);
        }

    }

    /**
     * An work item resulting from going up.
     */
    private static class WorkItemU extends WorkItem {

        public WorkItemU(int pos1, int pos2, int sum) {
            super(pos1, pos2, sum);
        }

    }

    /**
     * An work item resulting from going diagonal (with change).
     */
    private static class WorkItemD extends WorkItem {

        public WorkItemD(int pos1, int pos2, int sum) {
            super(pos1, pos2, sum);
        }

    }

    /**
     * An work item resulting from skipping an item that is the same on both sides (also going diagonal).
     */
    private static class WorkItemS extends WorkItem {

        public WorkItemS(int pos1, int pos2, int sum) {
            super(pos1, pos2, sum);
        }

    }

    /**
     * Determines the longest common subsequence between the given files and adds all matched
     * lines to the given matching. Uses the simple dynamic programming algorithm with some
     * additional pruning.
     */
    public static<T> void determineLcs(OneFileView<T> file1, OneFileView<T> file2, ItemMatching<T> matching) {
        final List<WorkItem> workStack = new ArrayList<>();
        final Map<WorkItem, WorkItem> bestSoFar = new HashMap<>();
        int totalBestSum = Integer.MAX_VALUE;
        final int bestPossibleResult = determineBestPossibleResult(file1, file2);

        workStack.add(new WorkItem(file1.getItemCount(), file2.getItemCount(), 0));
        while (!workStack.isEmpty()) {
            final WorkItem cur = workStack.remove(workStack.size() - 1);

            if (cur.sum >= totalBestSum) {
                continue;
            }

            final WorkItem bestItemSoFar = bestSoFar.get(cur);
            if (bestItemSoFar != null && bestItemSoFar.sum <= cur.sum) {
                continue;
            }
            bestSoFar.put(cur, cur);

            if (cur.pos1 == 0) {
                if (cur.pos2 == 0) {
                    //new best result found
                    totalBestSum = cur.sum;
                    if (cur.sum == bestPossibleResult) {
                        break;
                    }
                } else {
                    workStack.add(cur.up());
                }
            } else if (cur.pos2 == 0) {
                workStack.add(cur.left());
            } else if (file1.getItem(cur.pos1 - 1).equals(file2.getItem(cur.pos2 - 1))) {
                workStack.add(cur.same());
            } else {
                if (cur.pos1 == cur.pos2) {
                    workStack.add(cur.left());
                    workStack.add(cur.up());
                    workStack.add(cur.diagonal());
                } else if (cur.pos1 < cur.pos2) {
                    workStack.add(cur.left());
                    workStack.add(cur.diagonal());
                    workStack.add(cur.up());
                } else {
                    workStack.add(cur.up());
                    workStack.add(cur.diagonal());
                    workStack.add(cur.left());
                }
            }

            if (bestSoFar.size() > CUTOFF_LIMIT) {
                break;
            }
        }

        final WorkItem best = bestSoFar.get(new WorkItem(0, 0, 0));
        if (best == null) {
            //can happen if the calculation was stopped for performance reasons
            return;
        }

        WorkItem cur = best;
        while (true) {
            if (cur instanceof WorkItemU) {
                cur = bestSoFar.get(new WorkItem(cur.pos1, cur.pos2 + 1, 0));
            } else if (cur instanceof WorkItemL) {
                cur = bestSoFar.get(new WorkItem(cur.pos1 + 1, cur.pos2, 0));
            } else if (cur instanceof WorkItemD) {
                cur = bestSoFar.get(new WorkItem(cur.pos1 + 1, cur.pos2 + 1, 0));
            } else if (cur instanceof WorkItemS) {
                matching.match(file1, cur.pos1, file2, cur.pos2);
                cur = bestSoFar.get(new WorkItem(cur.pos1 + 1, cur.pos2 + 1, 0));
            } else {
                break;
            }
        }
    }

    private static<T> int determineBestPossibleResult(OneFileView<T> file1, OneFileView<T> file2) {
        //the edit distance cannot be smaller than the difference between the larger file size
        //  and the number of equal items
        final Multiset<T> items1 = new Multiset<>();
        for (int i = 0; i < file1.getItemCount(); i++) {
            items1.add(file1.getItem(i));
        }
        int equalItemCount = 0;
        for (int i = 0; i < file2.getItemCount(); i++) {
            final T item = file2.getItem(i);
            if (items1.contains(item)) {
                equalItemCount++;
                items1.remove(item);
            }
        }

        return Math.max(file1.getItemCount(), file2.getItemCount()) - equalItemCount;
    }

}
