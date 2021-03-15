package de.setsoftware.reviewtool.ui.views;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.eclipse.compare.IEncodedStreamContentAccessor;
import org.eclipse.compare.IModificationDate;
import org.eclipse.compare.ITypedElement;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.graphics.Image;

/**
 * Encapsulates string input for the TextMergeViewer.
 */
public class TextItem implements IEncodedStreamContentAccessor, ITypedElement, IModificationDate {

    private final String name;
    private final String contents;
    private final long modificationTime;
    private final Charset charset = Charset.forName("UTF-8"); //$NON-NLS-1$

    /**
     * Constructs a TextItem.
     * @param name The name of the TextItem.
     * @param contents The contents of the TextItem.
     * @param modificationTime The modification time of the TextItem in milliseconds since epoch.
     */
    public TextItem(final String name, final String contents, final long modificationTime) {
        this.name = name;
        this.contents = contents;
        this.modificationTime = modificationTime;
    }

    @Override
    public long getModificationDate() {
        return this.modificationTime;
    }

    @Override
    public Image getImage() {
        return null;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getType() {
        return ITypedElement.TEXT_TYPE;
    }

    @Override
    public InputStream getContents() throws CoreException {
        return new ByteArrayInputStream(this.contents.getBytes(this.charset));
    }

    @Override
    public String getCharset() throws CoreException {
        return this.charset.name();
    }

}
