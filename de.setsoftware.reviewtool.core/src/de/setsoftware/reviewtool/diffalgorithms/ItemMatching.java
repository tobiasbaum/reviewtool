package de.setsoftware.reviewtool.diffalgorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A matching of equal items in one sequence to the other sequence.
 * @param <T> Type of the items in the sequences.
 */
final class ItemMatching<T> {
    private final OneFileView<T> lines1;
    private final OneFileView<T> lines2;
    private final Map<Integer, Integer> matchedLines = new HashMap<>();

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
        this.removeIncompatibleMatchings();

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
    private void removeIncompatibleMatchings() {
        final List<Integer> indices1 = new ArrayList<>(this.matchedLines.keySet());
        Collections.sort(indices1);
        int lastIdx2 = -1;
        for (final int idx1 : indices1) {
            final int idx2 = this.matchedLines.get(idx1);
            if (idx2 <= lastIdx2) {
                this.matchedLines.remove(idx1);
            } else {
                lastIdx2 = idx2;
            }
        }
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

}