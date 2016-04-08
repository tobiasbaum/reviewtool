package de.setsoftware.reviewtool.diffalgorithms;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.setsoftware.reviewtool.base.Pair;
import de.setsoftware.reviewtool.model.changestructure.PositionInText;

/**
 * A simple line-based programming-language-agnostic diff algorithm.
 * Based on the description of patience diff by Bram Cohen (http://bramcohen.livejournal.com/73318.html).
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

        public final Pair<PositionInText, PositionInText> toPositions() {
            return Pair.create(
                    new PositionInText(this.getStartIndexInFullFile() + 1, 1),
                    new PositionInText(this.getEndIndexInFullFile() + 1, 0));
        }

        protected abstract int getStartIndexInFullFile();

        protected abstract int getEndIndexInFullFile();

        public final OneFileView stripPrefix(int prefixLength) {
            return this.subrangeWithoutExclusions(prefixLength, this.getLineCount());
        }

        public final OneFileView stripSuffix(int suffixLength) {
            return this.subrangeWithoutExclusions(0, this.getLineCount() - suffixLength);
        }

        public abstract OneFileView subrangeWithoutExclusions(int start, int end);

        public final OneFileView removeNonUniqueTexts() {
            final Map<String, Boolean> uniqueness = new HashMap<>();
            for (int i = 0; i < this.getLineCount(); i++) {
                final String line = this.getLine(i);
                final Boolean unique = uniqueness.get(line);
                uniqueness.put(line, unique == null ? Boolean.TRUE : Boolean.FALSE);
            }

            final List<Integer> indices = new ArrayList<>();
            for (int i = 0; i < this.getLineCount(); i++) {
                final String line = this.getLine(i);
                if (uniqueness.get(line)) {
                    indices.add(i);
                }
            }
            return new ScatteredView(this, indices);
        }

        @Override
        public String toString() {
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
        protected int getStartIndexInFullFile() {
            return 0;
        }

        @Override
        protected int getEndIndexInFullFile() {
            return this.fullFile.length;
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
        protected int getStartIndexInFullFile() {
            return this.decorated.getStartIndexInFullFile() + this.start;
        }

        @Override
        protected int getEndIndexInFullFile() {
            return this.decorated.getStartIndexInFullFile() + this.end;
        }

        @Override
        public OneFileView subrangeWithoutExclusions(int start, int end) {
            return new RangeView(this.decorated, this.start + start, this.start + end);
        }

    }

    /**
     * View of the lines of a file consisting of a potentially non-continuous subset of the lines.
     */
    private static final class ScatteredView extends OneFileView {
        private final OneFileView decorated;
        private final List<Integer> indices;

        ScatteredView(OneFileView decorated, List<Integer> indices) {
            this.decorated = decorated;
            this.indices = indices;
        }

        @Override
        public String getLine(int i) {
            return this.decorated.getLine(this.indices.get(i));
        }

        @Override
        public int getLineCount() {
            return this.indices.size();
        }

        @Override
        protected int getStartIndexInFullFile() {
            return this.decorated.getStartIndexInFullFile();
        }

        @Override
        protected int getEndIndexInFullFile() {
            return this.decorated.getEndIndexInFullFile();
        }

        @Override
        public OneFileView subrangeWithoutExclusions(int start, int end) {
            return this.decorated.subrangeWithoutExclusions(
                    this.determineStartWithoutExclusion(start),
                    this.determineEndWithoutExclusion(end));
        }

        private int determineStartWithoutExclusion(int start) {
            return start == 0 ? 0 : this.indices.get(start - 1) + 1;
        }

        private int determineEndWithoutExclusion(int end) {
            return end == this.getLineCount() ? this.decorated.getLineCount() : this.indices.get(end + 1) - 1;
        }
    }

    /**
     * Helper class that contains two views of a file's content, one for each file.
     */
    private static final class ContentView {
        private final OneFileView file1;
        private final OneFileView file2;

        public ContentView(String[] lines1, String[] lines2) {
            this.file1 = new FullFileView(lines1);
            this.file2 = new FullFileView(lines2);
        }

        private ContentView(OneFileView file1, OneFileView file2) {
            this.file1 = file1;
            this.file2 = file2;
        }

        public ContentView stripCommonPrefix() {
            final int minSize = Math.min(this.file1.getLineCount(), this.file2.getLineCount());
            int commonPrefixLength = 0;
            for (int i = 0; i < minSize; i++) {
                if (!this.file1.getLine(i).equals(this.file2.getLine(i))) {
                    break;
                }
                commonPrefixLength++;
            }
            return new ContentView(
                    this.file1.stripPrefix(commonPrefixLength),
                    this.file2.stripPrefix(commonPrefixLength));
        }

        public ContentView stripCommonSuffix() {
            final int minSize = Math.min(this.file1.getLineCount(), this.file2.getLineCount());
            int commonPrefixLength = 0;
            for (int i = 0; i < minSize; i++) {
                if (!this.file1.getLineFromEnd(i).equals(this.file2.getLineFromEnd(i))) {
                    break;
                }
                commonPrefixLength++;
            }
            return new ContentView(
                    this.file1.stripSuffix(commonPrefixLength),
                    this.file2.stripSuffix(commonPrefixLength));
        }

        public boolean isEmpty() {
            return this.file1.getLineCount() == 0 && this.file2.getLineCount() == 0;
        }

        public ContentView removeNonUniqueTexts() {
            return new ContentView(
                    this.file1.removeNonUniqueTexts(),
                    this.file2.removeNonUniqueTexts());
        }

        public List<ContentView> longestCommonSubsequence() {
            final int len1 = this.file1.getLineCount();
            final int len2 = this.file2.getLineCount();
            final LcsResult[][] resultCache = new LcsResult[len1 + 1][len2 + 1];
            final LcsResult result = lcs(this.file1, len1, this.file2, len2, resultCache);
            return result.extractChanges(this);
        }

        /**
         * Contains the partial result of a longest common subsequence calculation, consisting
         * of the information needed for the algorithm itself and the information needed to restore
         * the diff after the algorithm finishes.
         */
        private static class LcsResult {
            private static final byte KEEP = 0;
            private static final byte INSERT = 1;
            private static final byte REMOVE = 2;

            private final int commonLength;
            private final LcsResult prev;
            private final byte type;

            private LcsResult(int commonLength, LcsResult prev, byte type) {
                this.commonLength = commonLength;
                this.prev = prev;
                this.type = type;
            }

            public static LcsResult empty() {
                return new LcsResult(0, null, KEEP);
            }

            public static LcsResult insertChain(int len) {
                return chain(len, INSERT);
            }

            public static LcsResult removeChain(int len) {
                return chain(len, REMOVE);
            }

            private static LcsResult chain(int len, byte type) {
                LcsResult ret = empty();
                while (len > 0) {
                    len--;
                    ret = new LcsResult(0, ret, type);
                }
                return ret;
            }

            public LcsResult keepOne() {
                return new LcsResult(this.commonLength + 1, this, KEEP);
            }

            public LcsResult insertOne() {
                return new LcsResult(this.commonLength, this, INSERT);
            }

            public LcsResult removeOne() {
                return new LcsResult(this.commonLength, this, REMOVE);
            }

            public List<ContentView> extractChanges(ContentView contentView) {
                final List<ContentView> ret = new ArrayList<>();
                int curLine1 = contentView.file1.getLineCount();
                int curLine2 = contentView.file2.getLineCount();
                int lineKnownToBeEqual1 = curLine1 + 1;
                int lineKnownToBeEqual2 = curLine2 + 1;
                LcsResult cur = this;
                boolean inChangeMode = false;
                do {
                    switch (cur.type) {
                    case KEEP:
                        if (inChangeMode) {
                            ret.add(new ContentView(
                                    contentView.file1.subrangeWithoutExclusions(curLine1, lineKnownToBeEqual1 - 1),
                                    contentView.file2.subrangeWithoutExclusions(curLine2, lineKnownToBeEqual2 - 1)));
                            inChangeMode = false;
                        }
                        lineKnownToBeEqual1 = curLine1;
                        lineKnownToBeEqual2 = curLine2;
                        curLine1--;
                        curLine2--;
                        break;
                    case INSERT:
                        curLine2--;
                        inChangeMode = true;
                        break;
                    case REMOVE:
                        curLine1--;
                        inChangeMode = true;
                        break;
                    default:
                        throw new AssertionError();
                    }
                    cur = cur.prev;
                } while (cur != null);
                Collections.reverse(ret);
                return ret;
            }

        }

        private static LcsResult lcsWithCache(
                OneFileView a, int aLen, OneFileView b, int bLen, LcsResult[][] resultCache) {
            if (resultCache[aLen][bLen] != null) {
                return resultCache[aLen][bLen];
            } else {
                final LcsResult ret = lcs(a, aLen, b, bLen, resultCache);
                resultCache[aLen][bLen] = ret;
                return ret;
            }
        }

        private static LcsResult lcs(OneFileView a, int aLen, OneFileView b, int bLen, LcsResult[][] resultCache) {
            if (aLen == 0) {
                return LcsResult.insertChain(bLen);
            } else if (bLen == 0) {
                return LcsResult.removeChain(aLen);
            } else if (a.getLine(aLen - 1).equals(b.getLine(bLen - 1))) {
                return lcsWithCache(a, aLen - 1, b, bLen - 1, resultCache).keepOne();
            } else {
                final LcsResult x = lcsWithCache(a, aLen, b, bLen - 1, resultCache).insertOne();
                final LcsResult y = lcsWithCache(a, aLen - 1, b, bLen, resultCache).removeOne();
                return (x.commonLength > y.commonLength) ? x : y;
            }
        }
    }

    @Override
    public List<Pair<PositionInText, PositionInText>> determineDiff(byte[] fileOld, byte[] fileNew, String charset)
            throws IOException {

        final String[] lines1 = this.toLines(fileOld, charset);
        final String[] lines2 = this.toLines(fileNew, charset);
        final ContentView fullFiles = new ContentView(lines1, lines2);
        final ContentView stripped = fullFiles.stripCommonPrefix().stripCommonSuffix();
        if (stripped.isEmpty()) {
            return Collections.emptyList();
        }
        final ContentView uniqueTexts = stripped.removeNonUniqueTexts();
        final List<Pair<PositionInText, PositionInText>> ret = new ArrayList<>();
        for (final ContentView changedFragment : uniqueTexts.longestCommonSubsequence()) {
            ret.add(changedFragment.stripCommonPrefix().stripCommonSuffix().file2.toPositions());
        }
        return ret;
    }

    private String[] toLines(byte[] contents, String charset) throws IOException {
        final BufferedReader r = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(contents), charset));
        final List<String> lines = new ArrayList<>();
        String line;
        while ((line = r.readLine()) != null) {
            lines.add(line);
        }
        return lines.toArray(new String[lines.size()]);
    }

}
