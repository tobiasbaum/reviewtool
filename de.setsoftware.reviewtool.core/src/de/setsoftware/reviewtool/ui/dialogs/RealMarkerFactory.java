package de.setsoftware.reviewtool.ui.dialogs;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import de.setsoftware.reviewtool.model.Constants;
import de.setsoftware.reviewtool.model.EclipseMarker;
import de.setsoftware.reviewtool.model.PositionTransformer;
import de.setsoftware.reviewtool.model.changestructure.IStopMarkerFactory;
import de.setsoftware.reviewtool.model.remarks.IMarkerFactory;
import de.setsoftware.reviewtool.model.remarks.IReviewMarker;
import de.setsoftware.reviewtool.model.remarks.IReviewResource;
import de.setsoftware.reviewtool.model.remarks.Position;
import de.setsoftware.reviewtool.model.remarks.ReviewRemarkException;

/**
 * A factory to create normal Eclipse markers.
 */
public class RealMarkerFactory implements IStopMarkerFactory, IMarkerFactory {

    @Override
    public IReviewMarker createMarker(Position pos) throws ReviewRemarkException {
        try {
            return new EclipseMarker(PositionTransformer.toResource(pos).createMarker(Constants.REVIEWMARKER_ID));
        } catch (final CoreException e) {
            throw new ReviewRemarkException(e);
        }
    }

    @Override
    public IReviewMarker createMarker(IReviewResource resource) throws ReviewRemarkException {
        //TEST TODO ist diese Operation noch nötig?
        return resource.createReviewMarker();
    }

    @Override
    public IMarker createStopMarker(IResource resource) throws CoreException {
        return resource.createMarker(Constants.STOPMARKER_ID);
    }

}
