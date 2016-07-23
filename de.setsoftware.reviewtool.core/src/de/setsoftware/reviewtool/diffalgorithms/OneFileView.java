package de.setsoftware.reviewtool.diffalgorithms;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Helper class that provided a view into a subset of the items in a sequence (e.g. lines in a file).
 * @param <T> Type of the items in the sequence.
 */
abstract class OneFileView<T> {

    public abstract T getLine(int i);

    public final T getLineFromEnd(int i) {
        return this.getLine(this.getLineCount() - 1 - i);
    }

    public abstract int getLineCount();

    public abstract int toIndexInWholeFile(int index);

    public final OneFileView<T> stripPrefix(int prefixLength) {
        return this.subrange(prefixLength, this.getLineCount());
    }

    public final OneFileView<T> stripSuffix(int suffixLength) {
        return this.subrange(0, this.getLineCount() - suffixLength);
    }

    /**
     * Creates a subrange of the current sequence.
     * @param start The start index (inclusive).
     * @param end The end index (exclusive).
     */
    public abstract OneFileView<T> subrange(int start, int end);

    public Map<T, Integer> determineUniqueLinePositions() {
        final Map<T, Integer> uniqueLinePositions = new HashMap<>();
        final Set<T> nonUniqueLines = new HashSet<>();
        for (int i = 0; i < this.getLineCount(); i++) {
            final T line = this.getLine(i);
            if (nonUniqueLines.contains(line)) {
                continue;
            }
            final Integer unique = uniqueLinePositions.remove(line);
            if (unique == null) {
                uniqueLinePositions.put(line, i);
            } else {
                nonUniqueLines.add(line);
            }
        }
        return uniqueLinePositions;
    }

    @Override
    public String toString() {
        return this.getContent();
    }

    public String getContent() {
        final StringBuilder ret = new StringBuilder();
        for (int i = 0; i < this.getLineCount(); i++) {
            ret.append(this.getLine(i)).append('\n');
        }
        return ret.toString();
    }

}