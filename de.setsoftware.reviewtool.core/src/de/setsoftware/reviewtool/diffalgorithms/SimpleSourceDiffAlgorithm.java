package de.setsoftware.reviewtool.diffalgorithms;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import de.setsoftware.reviewtool.base.Pair;
import de.setsoftware.reviewtool.model.changestructure.FileInRevision;
import de.setsoftware.reviewtool.model.changestructure.Fragment;
import de.setsoftware.reviewtool.model.changestructure.PositionInText;

/**
 * A simple line-based diff algorithm that uses some knowledge about programming languages to provide
 * better results.
 * Loosely based on the description of patience diff by Bram Cohen (http://bramcohen.livejournal.com/73318.html).
 */
public class SimpleSourceDiffAlgorithm implements IDiffAlgorithm {

    /**
     * A part of the file that is logically related, e.g. a method.
     * The algorithm first tries to identify whole chunks and only looks at the
     * remaining lines later.
     */
    private static final class LogicalChunk {

        private final OneFileView<String> lines;
        private final int startIndex;
        private final int endIndex;

        public LogicalChunk(FullFileView<String> lines, int start, int end) {
            this.lines = lines;
            this.startIndex = start;
            this.endIndex = end;
        }

        public static OneFileView<String> resolveChunking(
                FullFileView<LogicalChunk> allChunks, OneFileView<LogicalChunk> chunks) {
            final LogicalChunk first = allChunks.getLine(chunks.toIndexInWholeFile(0));
            final LogicalChunk last = allChunks.getLine(chunks.toIndexInWholeFile(chunks.getLineCount() - 1));
            return first.lines.subrange(first.startIndex, last.endIndex);
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
                if (!this.lines.getLine(this.startIndex + i).equals(c.lines.getLine(c.startIndex + i))) {
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
    public List<Pair<Fragment, Fragment>> determineDiff(
            FileInRevision fileOldInfo,
            byte[] fileOld,
            FileInRevision fileNewInfo,
            byte[] fileNew,
            String charset) throws IOException {

        final FullFileView<String> lines1 = this.toLines(fileOld, charset);
        final FullFileView<String> lines2 = this.toLines(fileNew, charset);

        final FullFileView<LogicalChunk> chunks1 = this.chunk(lines1);
        final FullFileView<LogicalChunk> chunks2 = this.chunk(lines2);
        final List<ContentView<LogicalChunk>> logicalChunksWithDifferences =
                new ContentView<>(chunks1, chunks2).patienceDiff();

        final List<ContentView<String>> linesWithDifferences = new ArrayList<>();
        for (final ContentView<LogicalChunk> cur : logicalChunksWithDifferences) {
            linesWithDifferences.addAll(
                    new ContentView<>(
                            LogicalChunk.resolveChunking(chunks1, cur.getFile1()),
                            LogicalChunk.resolveChunking(chunks2, cur.getFile2()))
                    .patienceDiff());
        }

        return this.toFragments(fileOldInfo, fileNewInfo, linesWithDifferences);
    }

    private FullFileView<LogicalChunk> chunk(FullFileView<String> lines) {
        final List<LogicalChunk> logicalChunks = new ArrayList<>();
        int curChunkStart = 0;
        boolean endAtFirstNonEmptyLine = false;
        for (int i = 0; i < lines.getLineCount(); i++) {
            if (endAtFirstNonEmptyLine && !lines.getLine(i).trim().isEmpty()) {
                logicalChunks.add(new LogicalChunk(lines, curChunkStart, i));
                endAtFirstNonEmptyLine = false;
                curChunkStart = i;
            }
            if (this.looksLikeChunkEnd(lines.getLine(i))) {
                endAtFirstNonEmptyLine = true;
            }
        }
        if (curChunkStart < lines.getLineCount() || logicalChunks.isEmpty()) {
            logicalChunks.add(new LogicalChunk(lines, curChunkStart, lines.getLineCount()));
        }
        return new FullFileView<>(logicalChunks.toArray(new LogicalChunk[logicalChunks.size()]));
    }

    private boolean looksLikeChunkEnd(String line) {
        return line.contains("}") || line.contains("</");
    }

    private List<Pair<Fragment, Fragment>> toFragments(
            FileInRevision fileOldInfo, FileInRevision fileNewInfo, List<ContentView<String>> changedFragments) {
        final List<Pair<Fragment, Fragment>> ret = new ArrayList<>();
        for (final ContentView<String> v : changedFragments) {
            ret.add(Pair.create(
                    this.toFileFragment(fileOldInfo, v.getFile1()),
                    this.toFileFragment(fileNewInfo, v.getFile2())));
        }
        return ret;
    }

    private Fragment toFileFragment(FileInRevision fileInfo, OneFileView<String> fragmentData) {
        return new Fragment(fileInfo,
                new PositionInText(fragmentData.toIndexInWholeFile(0) + 1, 1),
                new PositionInText(fragmentData.toIndexInWholeFile(fragmentData.getLineCount() - 1) + 2, 0),
                fragmentData.getContent());
    }

    private FullFileView<String> toLines(byte[] contents, String charset) throws IOException {
        final BufferedReader r = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(contents), charset));
        final List<String> lines = new ArrayList<>();
        String line;
        while ((line = r.readLine()) != null) {
            lines.add(line);
        }
        return new FullFileView<String>(lines.toArray(new String[lines.size()]));
    }

}
