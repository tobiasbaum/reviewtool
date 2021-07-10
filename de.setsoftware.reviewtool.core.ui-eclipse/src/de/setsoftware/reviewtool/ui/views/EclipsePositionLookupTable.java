package de.setsoftware.reviewtool.ui.views;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import de.setsoftware.reviewtool.model.changestructure.PositionLookupTable;

public class EclipsePositionLookupTable {

    /**
     * Creates a lookup table for the contents from the given file.
     */
    public static PositionLookupTable create(IFile file)
            throws IOException, CoreException {
        if (!file.isSynchronized(IResource.DEPTH_ZERO)) {
            file.refreshLocal(IResource.DEPTH_ZERO, null);
        }
        try (final InputStream stream = file.getContents()) {
            return PositionLookupTable.create(stream, file.getCharset());
        }
    }

    /**
     * Creates a lookup table for the contents from the given file.
     */
    public static PositionLookupTable create(IFileStore fileStore)
            throws IOException, CoreException {
        try (final InputStream stream = fileStore.openInputStream(EFS.NONE, null)) {
            return PositionLookupTable.create(stream, "UTF-8");
        }
    }

}
