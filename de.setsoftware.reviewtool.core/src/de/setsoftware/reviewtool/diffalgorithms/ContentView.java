package de.setsoftware.reviewtool.diffalgorithms;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Helper class that contains two views of a sequence's content, one for each sequence.
 * @param <T> Type of the items in the sequences.
 */
final class ContentView<T> {
    private final OneFileView<T> file1;
    private final OneFileView<T> file2;

    ContentView(OneFileView<T> file1, OneFileView<T> file2) {
        this.file1 = file1;
        this.file2 = file2;
    }

    public ContentView<T> stripCommonPrefix(ItemMatching<T> matchingBuffer) {
        final int minSize = Math.min(this.file1.getItemCount(), this.file2.getItemCount());
        int commonPrefixLength = 0;
        for (int i = 0; i < minSize; i++) {
            if (!this.file1.getItem(i).equals(this.file2.getItem(i))) {
                break;
            }
            matchingBuffer.match(this.file1, i, this.file2, i);
            commonPrefixLength++;
        }
        return new ContentView<T>(
                this.file1.stripPrefix(commonPrefixLength),
                this.file2.stripPrefix(commonPrefixLength));
    }

    public ContentView<T> stripCommonSuffix(ItemMatching<T> matchingBuffer) {
        final int minSize = Math.min(this.file1.getItemCount(), this.file2.getItemCount());
        int commonPrefixLength = 0;
        for (int i = 0; i < minSize; i++) {
            if (!this.file1.getItemFromEnd(i).equals(this.file2.getItemFromEnd(i))) {
                break;
            }
            matchingBuffer.match(
                    this.file1, this.file1.getItemCount() - i - 1,
                    this.file2, this.file2.getItemCount() - i - 1);
            commonPrefixLength++;
        }
        return new ContentView<T>(
                this.file1.stripSuffix(commonPrefixLength),
                this.file2.stripSuffix(commonPrefixLength));
    }

    public ContentView<T> stripCommonPrefixAndSuffix(ItemMatching<T> matchingBuffer) {
        return this.stripCommonPrefix(matchingBuffer).stripCommonSuffix(matchingBuffer);
    }

    public boolean isEmpty() {
        return this.file1.getItemCount() == 0 && this.file2.getItemCount() == 0;
    }

    public void identifyUniqueLines(ItemMatching<T> matching) {
        final Map<T, Integer> uniqueLinePositions1 = this.file1.determineUniqueItemPositions();
        final Map<T, Integer> uniqueLinePositions2 = this.file2.determineUniqueItemPositions();
        for (final Entry<T, Integer> line1 : uniqueLinePositions1.entrySet()) {
            final Integer lineIdx2 = uniqueLinePositions2.get(line1.getKey());
            if (lineIdx2 != null) {
                matching.match(this.file1, line1.getValue(), this.file2, lineIdx2);
            }
        }
    }

    OneFileView<T> getFile1() {
        return this.file1;
    }

    OneFileView<T> getFile2() {
        return this.file2;
    }

    List<ContentView<T>> patienceDiff() {
        final ItemMatching<T> matching = new ItemMatching<T>(this.file1, this.file2);
        final ContentView<T> stripped = this.stripCommonPrefixAndSuffix(matching);
        if (stripped.isEmpty()) {
            return Collections.emptyList();
        }
        stripped.identifyUniqueLines(matching);
        for (final ContentView<T> changedFragment : matching.determineNonIdentifiedFragments()) {
            changedFragment.stripCommonPrefixAndSuffix(matching);
        }
        return matching.determineNonIdentifiedFragments();
    }

    List<ContentView<T>> myersDiff() {
        final ItemMatching<T> matching = new ItemMatching<T>(this.file1, this.file2);
        final ContentView<T> stripped = this.stripCommonPrefixAndSuffix(matching);
        if (stripped.isEmpty()) {
            return Collections.emptyList();
        }
        MyersDiff.doStuff(stripped.file1, stripped.file2, matching);
        return matching.determineNonIdentifiedFragments();
    }

    List<ContentView<T>> lcsDiff() {
        final ItemMatching<T> matching = new ItemMatching<T>(this.file1, this.file2);
        final ContentView<T> stripped = this.stripCommonPrefixAndSuffix(matching);
        if (stripped.isEmpty()) {
            return Collections.emptyList();
        }
        LongestCommonSubsequenceDiff.doStuff(stripped.file1, stripped.file2, matching);
        return matching.determineNonIdentifiedFragments();
    }
}