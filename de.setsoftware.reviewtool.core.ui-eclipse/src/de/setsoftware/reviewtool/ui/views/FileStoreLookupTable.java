package de.setsoftware.reviewtool.ui.views;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;

import de.setsoftware.reviewtool.model.changestructure.PositionLookupTable;

public class FileStoreLookupTable {

    /**
     * Creates a lookup table for the contents from the given file.
     */
    public static PositionLookupTable create(IFileStore fileStore)
            throws IOException, CoreException {
        final InputStream stream = fileStore.openInputStream(EFS.NONE, null);
        try {
            final Reader r = new InputStreamReader(stream, "UTF-8");
            return PositionLookupTable.create(r);
        } finally {
            stream.close();
        }
    }

}
