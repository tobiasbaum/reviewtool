package de.setsoftware.reviewtool.base;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages a sequence of lines and remembers the absolute character start and end positions of each line. Line and
 * character indices start at zero. Line endings must be supplied by the caller.
 */
public class LineSequence {

    private final List<String> lines;
    private final Map<Integer, Integer> lineOffsets;

    /**
     * Default constructor. Creates an empty LineSequence.
     */
    public LineSequence() {
        this.lines = new ArrayList<>();
        this.lineOffsets = new HashMap<>();
        this.lineOffsets.put(0, 0);
    }

    /**
     * Constructor which loads the lines from a byte array.
     * @param contents The byte array holding the line data.
     * @param charset The character set to use for the conversion from bytes to characters.
     * @throws IOException if some I/O error occurs.
     */
    public LineSequence(byte[] contents, String charset) throws IOException {
        this();
        final BufferedReader r = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(contents), charset));
        String line;
        while ((line = r.readLine()) != null) {
            this.addLine(line + '\n');
        }
    }

    /**
     * Adds a line.
     * @param line The line to add.
     */
    public void addLine(final String line) {
        this.lines.add(line);
        this.lineOffsets.put(this.lines.size(), this.lineOffsets.get(this.lines.size() - 1) + line.length());
    }

    /**
     * @return The number of lines in the sequence.
     */
    public int getNumberOfLines() {
        return this.lines.size();
    }

    /**
     * Returns some line.
     * @param lineIndex The line index, starting at zero.
     * @return The line.
     */
    public String getLine(final int lineIndex) {
        return this.lines.get(lineIndex);
    }

    /**
     * Returns some range of lines.
     * @param fromIndex The index of the start line (inclusive), starting at zero.
     * @param toIndex The index of the end line (exclusive), starting at zero.
     * @return The lines as a String array.
     */
    public String[] getLines(final int fromIndex, final int toIndex) {
        return this.lines.subList(fromIndex, toIndex).toArray(new String[] { });
    }

    /**
     * Returns some range of lines to a single String.
     * @param fromIndex The index of the start line (inclusive), starting at zero.
     * @param toIndex The index of the end line (exclusive), starting at zero.
     * @return The lines as a single string.
     */
    public String getLinesConcatenated(final int fromIndex, final int toIndex) {
        final StringBuilder builder = new StringBuilder();
        for (final String s : this.getLines(fromIndex, toIndex)) {
            builder.append(s);
        }
        return builder.toString();
    }

    /**
     * Returns the absolute character position of the start of some line.
     * @param lineIndex The line index, starting at zero.
     * @return The absolute character index pointing at the start of the line.
     */
    public int getStartPositionOfLine(final int lineIndex) {
        return this.lineOffsets.get(lineIndex);
    }
}
