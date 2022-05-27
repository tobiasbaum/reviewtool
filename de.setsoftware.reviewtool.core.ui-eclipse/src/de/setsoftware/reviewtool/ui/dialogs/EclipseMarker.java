package de.setsoftware.reviewtool.ui.dialogs;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

import de.setsoftware.reviewtool.base.ReviewtoolException;
import de.setsoftware.reviewtool.model.changestructure.IStopMarker;
import de.setsoftware.reviewtool.model.remarks.IReviewMarker;
import de.setsoftware.reviewtool.model.remarks.ReviewRemarkException;

/**
 * Adapter from {@link IReviewMarker} to Eclipse's {@link IMarker}.
 */
public class EclipseMarker implements IReviewMarker, IStopMarker {

    private final IMarker marker;

    private EclipseMarker(IMarker marker) {
        this.marker = marker;
    }

    /**
     * Creates an {@link EclipseMarker} for a new marker.
     */
    public static IReviewMarker create(IMarker m) {
        final EclipseMarker ret = new EclipseMarker(m);
        try {
            ret.marker.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_HIGH);
        } catch (final CoreException e) {
            throw new ReviewtoolException(e);
        }
        return ret;
    }

    /**
     * Wraps the given marker as an {@link EclipseMarker} without changing it further.
     */
    public static EclipseMarker wrap(IMarker m) {
        return new EclipseMarker(m);
    }

    @Override
    public void delete() throws ReviewRemarkException {
        try {
            this.marker.delete();
        } catch (final CoreException e) {
            throw new ReviewRemarkException(e);
        }
    }

    @Override
    public void setMessage(String newText) throws ReviewRemarkException {
        try {
            this.marker.setAttribute(IMarker.MESSAGE, newText);
        } catch (final CoreException e) {
            throw new ReviewRemarkException(e);
        }
    }

    @Override
    public String getMessage() {
        return this.marker.getAttribute(IMarker.MESSAGE, "");
    }

    @Override
    public void setAttribute(String attributeName, int value) throws ReviewRemarkException {
        try {
            this.marker.setAttribute(attributeName, value);
        } catch (final CoreException e) {
            throw new ReviewRemarkException(e);
        }
    }

    @Override
    public void setAttribute(String attributeName, String value) throws ReviewRemarkException {
        try {
            this.marker.setAttribute(attributeName, value);
        } catch (final CoreException e) {
            throw new ReviewRemarkException(e);
        }
    }

    @Override
    public String getAttribute(String attributeName, String defaultValue) throws ReviewRemarkException {
        return this.marker.getAttribute(attributeName, defaultValue);
    }

    @Override
    public void setLineNumber(int line) {
        this.setAttribute(IMarker.LINE_NUMBER, line);
    }

    @Override
    public void setSeverityInfo() {
        this.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO);
    }

    @Override
    public void setSeverityWarning() {
        this.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
    }

    @Override
    public void openEditor(boolean forceTextEditor) {
        final IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        try {
            ViewHelper.openEditorForMarker(page, marker, forceTextEditor);
        } catch (CoreException e) {
            throw new ReviewtoolException(e);
        }
    }

}
