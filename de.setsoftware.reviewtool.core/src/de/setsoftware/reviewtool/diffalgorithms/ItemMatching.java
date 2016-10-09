package de.setsoftware.reviewtool.diffalgorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import de.setsoftware.reviewtool.base.Multimap;

/**
 * A matching of equal items in one sequence to the other sequence.
 * @param <T> Type of the items in the sequences.
 */
final class ItemMatching<T> {
    private final OneFileView<T> lines1;
    private final OneFileView<T> lines2;
    private final Map<Integer, Integer> matchedLines = new TreeMap<>();

    public ItemMatching(OneFileView<T> lines1, OneFileView<T> lines2) {
        this.lines1 = lines1;
        this.lines2 = lines2;
    }

    public void match(OneFileView<T> file1, int indexInFile1, OneFileView<T> file2, int indexInFile2) {
        this.matchedLines.put(
                file1.toIndexInWholeFile(indexInFile1),
                file2.toIndexInWholeFile(indexInFile2));
    }

    /**
     * Determine all fragments of the two files that have not been identified and return them in order.
     * In other words, all lines which have a proper matching partner are left out.
     */
    public List<ContentView<T>> determineNonIdentifiedFragments() {
//        this.removeIncompatibleMatchings();

        final List<ContentView<T>> ret = new ArrayList<>();
        int idx2 = 0;
        int changeSize1 = 0;
        int changeSize2 = 0;
        for (int idx1 = 0; idx1 < this.lines1.getItemCount(); idx1++) {
            final Integer matchForCurrentLine = this.matchedLines.get(this.lines1.toIndexInWholeFile(idx1));
            if (matchForCurrentLine == null) {
                changeSize1++;
            } else {
                while (this.lines2.toIndexInWholeFile(idx2) < matchForCurrentLine) {
                    changeSize2++;
                    idx2++;
                    assert idx2 < this.lines2.getItemCount();
                }
                this.createChangeFragment(ret, idx1, idx2, changeSize1, changeSize2);
                assert idx2 < this.lines2.getItemCount();
                changeSize1 = 0;
                changeSize2 = 0;
                idx2++;
            }
        }
        assert idx2 <= this.lines2.getItemCount();
        while (idx2 < this.lines2.getItemCount()) {
            changeSize2++;
            idx2++;
        }
        this.createChangeFragment(ret, this.lines1.getItemCount(), idx2, changeSize1, changeSize2);
        return ret;
    }

    /**
     * As moves are currently not supported, matchings are not allowed to cross each other (e.g.
     * line A.1 -> B.7 and A.3 -> B.5). This method removes these "incompatible matches".
     */
    void removeIncompatibleMatchings() {
        //the best removal is the one that keeps the maximum number of assignments
        //this is the co-clique problem in the conflict graph and therefore NP-complete
        //we use a simple greedy heuristic instead

        //determine conflicts
        final List<Integer> indices1 = new ArrayList<>(this.matchedLines.keySet());
        final Multimap<Integer, Integer> conflictsForNodes = new Multimap<>();
        for (int i = 0; i < indices1.size(); i++) {
            for (int j = i + 1; j < indices1.size(); j++) {
                final int indexI = indices1.get(i);
                final int indexJ = indices1.get(j);
                if (this.isMatchingConflict(indexI, indexJ)) {
                    conflictsForNodes.put(indexI, indexJ);
                    conflictsForNodes.put(indexJ, indexI);
                }
            }
        }

        //remove matchings with the maximum number of conflicts until no conflicts are left
        while (true) {
            final Integer maxConflictSizeIndex = conflictsForNodes.keyWithMaxNumberOfValues();
            final List<Integer> conflictPartners = conflictsForNodes.get(maxConflictSizeIndex);
            if (conflictPartners.isEmpty()) {
                return;
            }
            this.matchedLines.remove(maxConflictSizeIndex);
            for (final Integer conflictPartner : conflictPartners) {
                conflictsForNodes.removeValue(conflictPartner, maxConflictSizeIndex);
            }
            conflictsForNodes.removeKey(maxConflictSizeIndex);
        }
    }

    private boolean isMatchingConflict(int indexI, int indexJ) {
        assert indexI != indexJ;
        final int matchI = this.matchedLines.get(indexI);
        final int matchJ = this.matchedLines.get(indexJ);
        return (indexI < indexJ) != (matchI < matchJ);
    }

    private void createChangeFragment(
            final List<ContentView<T>> ret, int idx1, int idx2,
            final int changeSize1, final int changeSize2) {
        if (changeSize1 > 0 || changeSize2 > 0) {
            ret.add(new ContentView<>(
                    this.lines1.subrange(idx1 - changeSize1, idx1),
                    this.lines2.subrange(idx2 - changeSize2, idx2)));
        }
    }

    @Override
    public String toString() {
        return this.matchedLines.toString();
    }

}