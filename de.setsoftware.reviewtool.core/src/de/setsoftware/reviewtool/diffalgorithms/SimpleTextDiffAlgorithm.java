package de.setsoftware.reviewtool.diffalgorithms;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import de.setsoftware.reviewtool.base.Pair;
import de.setsoftware.reviewtool.model.changestructure.FileFragment;
import de.setsoftware.reviewtool.model.changestructure.FileInRevision;
import de.setsoftware.reviewtool.model.changestructure.PositionInText;

/**
 * A simple line-based programming-language-agnostic diff algorithm.
 * Loosely based on the description of patience diff by Bram Cohen (http://bramcohen.livejournal.com/73318.html).
 */
public class SimpleTextDiffAlgorithm implements IDiffAlgorithm {

    /**
     * Helper class that provided a view into subset of the lines of a file.
     */
    private abstract static class OneFileView {

        public abstract String getLine(int i);

        public final String getLineFromEnd(int i) {
            return this.getLine(this.getLineCount() - 1 - i);
        }

        public abstract int getLineCount();

        public abstract int toIndexInWholeFile(int index);

        public final OneFileView stripPrefix(int prefixLength) {
            return this.subrangeWithoutExclusions(prefixLength, this.getLineCount());
        }

        public final OneFileView stripSuffix(int suffixLength) {
            return this.subrangeWithoutExclusions(0, this.getLineCount() - suffixLength);
        }

        public abstract OneFileView subrangeWithoutExclusions(int start, int end);

        public Map<String, Integer> determineUniqueLinePositions() {
            final Map<String, Integer> uniqueLinePositions = new HashMap<>();
            final Set<String> nonUniqueLines = new HashSet<>();
            for (int i = 0; i < this.getLineCount(); i++) {
                final String line = this.getLine(i);
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

    /**
     * Basic {@link OneFileView} with the whole contents of a file.
     */
    private static final class FullFileView extends OneFileView {
        private final String[] fullFile;

        public FullFileView(String[] lines) {
            this.fullFile = lines;
        }

        @Override
        public int getLineCount() {
            return this.fullFile.length;
        }

        @Override
        public String getLine(int i) {
            return this.fullFile[i];
        }

        @Override
        public int toIndexInWholeFile(int index) {
            return index;
        }

        @Override
        public OneFileView subrangeWithoutExclusions(int start, int end) {
            return new RangeView(this, start, end);
        }

    }

    /**
     * Continuous subsequence of the lines in a file.
     */
    private static final class RangeView extends OneFileView {
        private final OneFileView decorated;
        private final int start;
        private final int end;

        RangeView(OneFileView decorated, int start, int end) {
            this.decorated = decorated;
            this.start = start;
            this.end = end;
        }

        @Override
        public int getLineCount() {
            return this.end - this.start;
        }

        @Override
        public String getLine(int i) {
            return this.decorated.getLine(this.start + i);
        }

        @Override
        public int toIndexInWholeFile(int index) {
            return this.decorated.toIndexInWholeFile(index + this.start);
        }

        @Override
        public OneFileView subrangeWithoutExclusions(int start, int end) {
            return new RangeView(this.decorated, this.start + start, this.start + end);
        }

    }

    /**
     * Helper class that contains two views of a file's content, one for each file.
     */
    private static final class ContentView {
        private final OneFileView file1;
        private final OneFileView file2;

        private ContentView(OneFileView file1, OneFileView file2) {
            this.file1 = file1;
            this.file2 = file2;
        }

        public ContentView stripCommonPrefix(LineMatching matchingBuffer) {
            final int minSize = Math.min(this.file1.getLineCount(), this.file2.getLineCount());
            int commonPrefixLength = 0;
            for (int i = 0; i < minSize; i++) {
                if (!this.file1.getLine(i).equals(this.file2.getLine(i))) {
                    break;
                }
                matchingBuffer.match(this.file1, i, this.file2, i);
                commonPrefixLength++;
            }
            return new ContentView(
                    this.file1.stripPrefix(commonPrefixLength),
                    this.file2.stripPrefix(commonPrefixLength));
        }

        public ContentView stripCommonSuffix(LineMatching matchingBuffer) {
            final int minSize = Math.min(this.file1.getLineCount(), this.file2.getLineCount());
            int commonPrefixLength = 0;
            for (int i = 0; i < minSize; i++) {
                if (!this.file1.getLineFromEnd(i).equals(this.file2.getLineFromEnd(i))) {
                    break;
                }
                matchingBuffer.match(
                        this.file1, this.file1.getLineCount() - i - 1,
                        this.file2, this.file2.getLineCount() - i - 1);
                commonPrefixLength++;
            }
            return new ContentView(
                    this.file1.stripSuffix(commonPrefixLength),
                    this.file2.stripSuffix(commonPrefixLength));
        }

        public ContentView stripCommonPrefixAndSuffix(LineMatching matchingBuffer) {
            return this.stripCommonPrefix(matchingBuffer).stripCommonSuffix(matchingBuffer);
        }

        public boolean isEmpty() {
            return this.file1.getLineCount() == 0 && this.file2.getLineCount() == 0;
        }

        public void identifyUniqueLines(LineMatching matching) {
            final Map<String, Integer> uniqueLinePositions1 = this.file1.determineUniqueLinePositions();
            final Map<String, Integer> uniqueLinePositions2 = this.file2.determineUniqueLinePositions();
            for (final Entry<String, Integer> line1 : uniqueLinePositions1.entrySet()) {
                final Integer lineIdx2 = uniqueLinePositions2.get(line1.getKey());
                if (lineIdx2 != null) {
                    matching.match(this.file1, line1.getValue(), this.file2, lineIdx2);
                }
            }
        }
    }

    /**
     * A matching of equal lines in one file to the other file.
     */
    private static final class LineMatching {
        private final FullFileView lines1;
        private final FullFileView lines2;
        private final Map<Integer, Integer> matchedLines = new HashMap<>();

        public LineMatching(FullFileView lines1, FullFileView lines2) {
            this.lines1 = lines1;
            this.lines2 = lines2;
        }

        public void match(OneFileView file1, int indexInFile1, OneFileView file2, int indexInFile2) {
            this.matchedLines.put(
                    file1.toIndexInWholeFile(indexInFile1),
                    file2.toIndexInWholeFile(indexInFile2));
        }

        public List<ContentView> determineNonIdentifiedFragments() {
            final List<ContentView> ret = new ArrayList<>();
            int idx2 = 0;
            int changeSize1 = 0;
            int changeSize2 = 0;
            for (int idx1 = 0; idx1 < this.lines1.getLineCount(); idx1++) {
                final Integer matchForCurrentLine = this.matchedLines.get(idx1);
                if (matchForCurrentLine == null) {
                    changeSize1++;
                } else {
                    while (idx2 < matchForCurrentLine) {
                        changeSize2++;
                        idx2++;
                    }
                    this.createChangeFragment(ret, idx1, idx2, changeSize1, changeSize2);
                    changeSize1 = 0;
                    changeSize2 = 0;
                    idx2++;
                }
            }
            while (idx2 < this.lines2.getLineCount()) {
                changeSize2++;
                idx2++;
            }
            this.createChangeFragment(ret, this.lines1.getLineCount(), idx2, changeSize1, changeSize2);
            return ret;
        }

        private void createChangeFragment(
                final List<ContentView> ret, int idx1, int idx2,
                final int changeSize1, final int changeSize2) {
            if (changeSize1 > 0 || changeSize2 > 0) {
                ret.add(new ContentView(
                        this.lines1.subrangeWithoutExclusions(idx1 - changeSize1, idx1),
                        this.lines2.subrangeWithoutExclusions(idx2 - changeSize2, idx2)));
            }
        }

    }

    @Override
    public List<Pair<FileFragment, FileFragment>> determineDiff(
            FileInRevision fileOldInfo,
            byte[] fileOld,
            FileInRevision fileNewInfo,
            byte[] fileNew,
            String charset) throws IOException {

        final FullFileView lines1 = this.toLines(fileOld, charset);
        final FullFileView lines2 = this.toLines(fileNew, charset);
        final ContentView fullFiles = new ContentView(lines1, lines2);
        final LineMatching matching = new LineMatching(lines1, lines2);
        final ContentView stripped = fullFiles.stripCommonPrefixAndSuffix(matching);
        if (stripped.isEmpty()) {
            return Collections.emptyList();
        }
        stripped.identifyUniqueLines(matching);
        for (final ContentView changedFragment : matching.determineNonIdentifiedFragments()) {
            changedFragment.stripCommonPrefixAndSuffix(matching);
        }
        return this.toFragments(fileOldInfo, fileNewInfo, matching.determineNonIdentifiedFragments());
    }

    private List<Pair<FileFragment, FileFragment>> toFragments(
            FileInRevision fileOldInfo, FileInRevision fileNewInfo, List<ContentView> changedFragments) {
        final List<Pair<FileFragment, FileFragment>> ret = new ArrayList<>();
        for (final ContentView v : changedFragments) {
            ret.add(Pair.create(
                    this.toFileFragment(fileOldInfo, v.file1),
                    this.toFileFragment(fileNewInfo, v.file2)));
        }
        return ret;
    }

    private FileFragment toFileFragment(FileInRevision fileInfo, OneFileView fragmentData) {
        return new FileFragment(fileInfo,
                new PositionInText(fragmentData.toIndexInWholeFile(0) + 1, 1),
                new PositionInText(fragmentData.toIndexInWholeFile(fragmentData.getLineCount() - 1) + 2, 0),
                fragmentData.getContent());
    }

    private FullFileView toLines(byte[] contents, String charset) throws IOException {
        final BufferedReader r = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(contents), charset));
        final List<String> lines = new ArrayList<>();
        String line;
        while ((line = r.readLine()) != null) {
            lines.add(line);
        }
        return new FullFileView(lines.toArray(new String[lines.size()]));
    }

}
