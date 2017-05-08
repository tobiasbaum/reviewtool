package de.setsoftware.reviewtool.diffalgorithms;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import de.setsoftware.reviewtool.base.Pair;
import de.setsoftware.reviewtool.base.ReviewtoolException;
import de.setsoftware.reviewtool.model.api.IFragment;
import de.setsoftware.reviewtool.model.api.IRevisionedFile;
import de.setsoftware.reviewtool.model.changestructure.ChangestructureFactory;

/**
 * A simple line-based diff algorithm that uses some knowledge about programming languages to provide
 * better results.
 * Loosely based on the description of patience diff by Bram Cohen (http://bramcohen.livejournal.com/73318.html).
 */
class SimpleSourceDiffAlgorithm implements IDiffAlgorithm {

    /**
     * A part of the file that is logically related, e.g. a method.
     * The algorithm first tries to identify whole chunks and only looks at the
     * remaining lines later.
     */
    private static final class LogicalChunk {

        private final FullFileView<String> lines;
        private final int startIndex;
        private final int endIndex;

        public LogicalChunk(FullFileView<String> lines, int start, int end) {
            this.lines = lines;
            this.startIndex = start;
            this.endIndex = end;
        }

        public static OneFileView<String> resolveChunking(
                FullFileView<LogicalChunk> allChunks, OneFileView<LogicalChunk> chunks) {
            //all chunks need to have the same base file, therefore it does not matter which file we take
            final FullFileView<String> allLines = allChunks.getItem(0).lines;

            final int firstIndex = chunks.toIndexInWholeFile(0);
            if (firstIndex >= allChunks.getItemCount()) {
                return allLines.subrange(allLines.getItemCount(), allLines.getItemCount());
            }
            final LogicalChunk first = allChunks.getItem(firstIndex);
            if (chunks.getItemCount() == 0) {
                return allLines.subrange(first.startIndex, first.startIndex);
            }
            final LogicalChunk last = allChunks.getItem(chunks.toIndexInWholeFile(chunks.getItemCount() - 1));
            return allLines.subrange(first.startIndex, last.endIndex);
        }

        @Override
        public int hashCode() {
            return this.endIndex - this.startIndex;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof LogicalChunk)) {
                return false;
            }
            final LogicalChunk c = (LogicalChunk) o;
            final int cLength = c.endIndex - c.startIndex;
            final int thisLength = this.endIndex - this.startIndex;
            if (cLength != thisLength) {
                return false;
            }
            for (int i = 0; i < thisLength; i++) {
                if (!this.lines.getItem(this.startIndex + i).equals(c.lines.getItem(c.startIndex + i))) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public String toString() {
            return this.lines.subrange(this.startIndex, this.endIndex).toString();
        }

    }

    @Override
    public List<Pair<IFragment, IFragment>> determineDiff(
            IRevisionedFile fileOldInfo,
            byte[] fileOld,
            IRevisionedFile fileNewInfo,
            byte[] fileNew,
            String charset) {

        final FullFileView<String> lines1 = this.toLines(fileOld, charset);
        final FullFileView<String> lines2 = this.toLines(fileNew, charset);

        final FullFileView<LogicalChunk> chunks1 = this.chunk(lines1);
        final FullFileView<LogicalChunk> chunks2 = this.chunk(lines2);
        final List<ContentView<LogicalChunk>> logicalChunksWithDifferences =
                new ContentView<>(chunks1, chunks2).lcsDiff();

        final List<ContentView<String>> linesWithDifferences = new ArrayList<>();
        for (final ContentView<LogicalChunk> cur : logicalChunksWithDifferences) {
            linesWithDifferences.addAll(
                    new ContentView<>(
                            LogicalChunk.resolveChunking(chunks1, cur.getFile1()),
                            LogicalChunk.resolveChunking(chunks2, cur.getFile2()))
                    .lcsDiff());
        }

        return this.toFragments(fileOldInfo, fileNewInfo, linesWithDifferences);
    }

    private FullFileView<LogicalChunk> chunk(FullFileView<String> lines) {
        final List<LogicalChunk> logicalChunks = new ArrayList<>();
        int curChunkStart = 0;
        boolean endAtFirstNonEmptyLine = false;
        for (int i = 0; i < lines.getItemCount(); i++) {
            if (endAtFirstNonEmptyLine && !lines.getItem(i).trim().isEmpty()) {
                logicalChunks.add(new LogicalChunk(lines, curChunkStart, i));
                endAtFirstNonEmptyLine = false;
                curChunkStart = i;
            }
            if (this.looksLikeChunkEnd(lines.getItem(i))) {
                endAtFirstNonEmptyLine = true;
            }
        }
        if (curChunkStart < lines.getItemCount() || logicalChunks.isEmpty()) {
            logicalChunks.add(new LogicalChunk(lines, curChunkStart, lines.getItemCount()));
        }
        return new FullFileView<>(logicalChunks.toArray(new LogicalChunk[logicalChunks.size()]));
    }

    private boolean looksLikeChunkEnd(String line) {
        return line.contains("}") || line.contains("</");
    }

    private List<Pair<IFragment, IFragment>> toFragments(
            IRevisionedFile fileOldInfo, IRevisionedFile fileNewInfo, List<ContentView<String>> changedFragments) {
        final List<Pair<IFragment, IFragment>> ret = new ArrayList<>();
        for (final ContentView<String> v : changedFragments) {
            if (this.isSingleLineChange(v)) {
                ret.add(this.createInLineDiffFragment(fileOldInfo, fileNewInfo, v));
            } else {
                ret.add(Pair.create(
                        this.toFileFragment(fileOldInfo, v.getFile1()),
                        this.toFileFragment(fileNewInfo, v.getFile2())));
            }
        }
        return ret;
    }

    private boolean isSingleLineChange(ContentView<String> v) {
        return v.getFile1().getItemCount() == 1 && v.getFile2().getItemCount() == 1;
    }

    private Pair<IFragment, IFragment> createInLineDiffFragment(
            IRevisionedFile fileOldInfo, IRevisionedFile fileNewInfo,
            ContentView<String> v) {
        final String content1 = v.getFile1().getItem(0);
        final String content2 = v.getFile2().getItem(0);
        assert !content1.equals(content2);
        final int commonPrefixLength = this.determineCommonPrefixLength(content1, content2);
        final int commonSuffixLength = this.determineCommonSuffixLength(
                content1.substring(commonPrefixLength),
                content2.substring(commonPrefixLength));
        return Pair.create(
                this.toInLineFileFragment(fileOldInfo, v.getFile1(), commonPrefixLength, commonSuffixLength),
                this.toInLineFileFragment(fileNewInfo, v.getFile2(), commonPrefixLength, commonSuffixLength));
    }

    private int determineCommonPrefixLength(String content1, String content2) {
        final int max = Math.min(content1.length(), content2.length());
        for (int i = 0; i < max; i++) {
            if (content1.charAt(i) != content2.charAt(i)) {
                return i;
            }
        }
        return max;
    }

    private int determineCommonSuffixLength(String content1, String content2) {
        final int max = Math.min(content1.length(), content2.length());
        for (int i = 1; i <= max; i++) {
            if (content1.charAt(content1.length() - i) != content2.charAt(content2.length() - i)) {
                return i - 1;
            }
        }
        return max;
    }

    private IFragment toInLineFileFragment(IRevisionedFile fileInfo, OneFileView<String> fragmentData,
            int prefixLength, int suffixLength) {
        final String line = fragmentData.getItem(0);
        return ChangestructureFactory.createFragment(fileInfo,
                ChangestructureFactory.createPositionInText(
                        fragmentData.toIndexInWholeFile(0) + 1, prefixLength + 1),
                ChangestructureFactory.createPositionInText(
                        fragmentData.toIndexInWholeFile(0) + 1, line.length() - suffixLength + 1));
    }

    private IFragment toFileFragment(IRevisionedFile fileInfo, OneFileView<String> fragmentData) {
        return ChangestructureFactory.createFragment(fileInfo,
                ChangestructureFactory.createPositionInText(
                        fragmentData.toIndexInWholeFile(0) + 1, 1),
                ChangestructureFactory.createPositionInText(
                        fragmentData.toIndexInWholeFile(fragmentData.getItemCount() - 1) + 2, 1));
    }

    private FullFileView<String> toLines(byte[] contents, String charset) {
        try {
            final BufferedReader r =
                    new BufferedReader(new InputStreamReader(new ByteArrayInputStream(contents), charset));
            final List<String> lines = new ArrayList<>();
            String line;
            while ((line = r.readLine()) != null) {
                lines.add(line);
            }
            return new FullFileView<String>(lines.toArray(new String[lines.size()]));
        } catch (final IOException e) {
            // should not happen because reading from a byte buffer should not throw I/O exceptions
            throw new ReviewtoolException(e);
        }
    }

}
