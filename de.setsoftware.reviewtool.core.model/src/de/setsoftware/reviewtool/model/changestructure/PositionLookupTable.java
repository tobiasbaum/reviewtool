package de.setsoftware.reviewtool.model.changestructure;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import de.setsoftware.reviewtool.base.ReviewtoolException;
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
     * Creates a lookup table for the contents from the given file.
     */
    public static PositionLookupTable create(IFile file) {
        try {
            if (!file.isSynchronized(IResource.DEPTH_ZERO)) {
                file.refreshLocal(IResource.DEPTH_ZERO, null);
            }
            final InputStream stream = file.getContents();
            try {
                final Reader r = new InputStreamReader(stream, file.getCharset());
                return create(r);
            } finally {
                stream.close();
            }
        } catch (CoreException | IOException e) {
            throw new ReviewtoolException(e);
        }
    }

    /**
     * Creates a lookup table for the contents from the given file.
     */
    public static PositionLookupTable create(IFileStore fileStore)
            throws IOException, CoreException {
        final InputStream stream = fileStore.openInputStream(EFS.NONE, null);
        try {
            final Reader r = new InputStreamReader(stream, "UTF-8");
            return create(r);
        } finally {
            stream.close();
        }
    }

    /**
     * Creates a lookup table for the contents from the given reader.
     */
    static PositionLookupTable create(Reader reader) throws IOException {
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
