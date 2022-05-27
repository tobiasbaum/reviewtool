package de.setsoftware.reviewtool.model.changestructure;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import de.setsoftware.reviewtool.model.api.IPositionInText;

/**
 * Allows the transformation from position in the form (line,column) to
 * "number of characters since file start" and caches relevant information.
 */
public class PositionLookupTable {

    private final List<Integer> charCountAtEndOfLine = new ArrayList<>();

    private PositionLookupTable() {
    }

    /**
     * Creates a lookup table for the contents from the given reader.
     */
    public static PositionLookupTable create(Reader reader) throws IOException {
        final PositionLookupTable ret = new PositionLookupTable();
        int ch;
        int charCount = 0;
        ret.charCountAtEndOfLine.add(0);
        while ((ch = reader.read()) >= 0) {
            charCount++;
            if (ch == '\n') {
                ret.charCountAtEndOfLine.add(charCount);
            }
        }
        ret.charCountAtEndOfLine.add(charCount);
        return ret;
    }

    /**
     * Returns the number of characters from the start of the file up to (and including) the given position.
     */
    public int getCharsSinceFileStart(IPositionInText pos) {
        //when tracing of changes does not work properly, there can be positions that are out of the file and
        //  that have to be handled in some way
        if (pos.getLine() <= 0) {
            return 0;
        }
        if (pos.getLine() >= this.charCountAtEndOfLine.size()) {
            return this.charCountAtEndOfLine.get(this.charCountAtEndOfLine.size() - 1);
        }

        return this.charCountAtEndOfLine.get(pos.getLine() - 1) + pos.getColumn() - 1;
    }

}
