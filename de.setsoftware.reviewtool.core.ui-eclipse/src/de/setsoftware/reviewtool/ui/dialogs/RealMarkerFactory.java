package de.setsoftware.reviewtool.ui.dialogs;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

import de.setsoftware.reviewtool.base.ReviewtoolException;
import de.setsoftware.reviewtool.eclipse.model.EclipseMarker;
import de.setsoftware.reviewtool.model.Constants;
import de.setsoftware.reviewtool.model.PositionTransformer;
import de.setsoftware.reviewtool.model.api.ICortResource;
import de.setsoftware.reviewtool.model.api.IStopMarker;
import de.setsoftware.reviewtool.model.changestructure.IStopMarkerFactory;
import de.setsoftware.reviewtool.model.remarks.IMarkerFactory;
import de.setsoftware.reviewtool.model.remarks.IReviewMarker;
import de.setsoftware.reviewtool.model.remarks.Position;
import de.setsoftware.reviewtool.model.remarks.ReviewRemarkException;

/**
 * A factory to create normal Eclipse markers.
 */
public class RealMarkerFactory implements IStopMarkerFactory, IMarkerFactory {

    @Override
    public IReviewMarker createMarker(Position pos) throws ReviewRemarkException {
        return EclipseMarker.create(createMarker(PositionTransformer.toResource(pos), Constants.REVIEWMARKER_ID));
    }
    
    private IMarker createMarker(ICortResource res, String type) {
        throw new AssertionError("not yet implemented");
    }

    @Override
    public IStopMarker createStopMarker(ICortResource resource, boolean tourActive, String message) {
        return createStopMarker(resource, tourActive, message, -1, -1, -1);
    }

    @Override
    public IStopMarker createStopMarker(ICortResource resource, boolean tourActive, String message, int line,
            int charStart, int charEnd) {
        IMarker marker = createMarker(resource, tourActive ? Constants.STOPMARKER_ID : Constants.INACTIVESTOPMARKER_ID);
        try {
            marker.setAttribute(IMarker.MESSAGE, message);
            if (line < 0) {
                marker.setAttribute(IMarker.LINE_NUMBER, line);
                marker.setAttribute(IMarker.CHAR_START, charStart);
                marker.setAttribute(IMarker.CHAR_END, charEnd);                
            }
        } catch (CoreException e) {
            throw new ReviewtoolException(e);
        }
        return new IStopMarker() {
        };
    }

    @Override
    public void clearStopMarkers() {
        try {
            ResourcesPlugin.getWorkspace().getRoot().deleteMarkers(
                    Constants.STOPMARKER_ID, true, IResource.DEPTH_INFINITE);
            ResourcesPlugin.getWorkspace().getRoot().deleteMarkers(
                    Constants.INACTIVESTOPMARKER_ID, true, IResource.DEPTH_INFINITE);
        } catch (CoreException e) {
            throw new ReviewtoolException(e);
        }
    }

}
