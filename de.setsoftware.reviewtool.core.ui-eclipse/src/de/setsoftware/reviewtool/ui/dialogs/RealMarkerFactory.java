package de.setsoftware.reviewtool.ui.dialogs;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

import de.setsoftware.reviewtool.model.Constants;
import de.setsoftware.reviewtool.model.PositionTransformer;
import de.setsoftware.reviewtool.model.api.IFragment;
import de.setsoftware.reviewtool.model.changestructure.IStopMarker;
import de.setsoftware.reviewtool.model.changestructure.IStopMarkerFactory;
import de.setsoftware.reviewtool.model.changestructure.PositionLookupTable;
import de.setsoftware.reviewtool.model.changestructure.ToursInReview;
import de.setsoftware.reviewtool.model.remarks.IMarkerFactory;
import de.setsoftware.reviewtool.model.remarks.IReviewMarker;
import de.setsoftware.reviewtool.model.remarks.Position;
import de.setsoftware.reviewtool.model.remarks.ReviewRemarkException;

/**
 * A factory to create normal Eclipse markers.
 */
public class RealMarkerFactory implements IStopMarkerFactory, IMarkerFactory {

    private final Map<IResource, PositionLookupTable> lookupTables = new HashMap<>();
    
    @Override
    public IReviewMarker createMarker(Position pos) throws ReviewRemarkException {
        try {
            IResource res = ToursInReview.getResourceForPath(PositionTransformer.toPath(pos.getShortFileName()));
            return EclipseMarker.create(res.createMarker(Constants.REVIEWMARKER_ID));
        } catch (final CoreException e) {
            throw new ReviewRemarkException(e);
        }
    }

    @Override
    public IStopMarker createStopMarker(IResource resource, boolean tourActive, String message) {
        try {
            IMarker marker = resource.createMarker(
                    tourActive ? Constants.STOPMARKER_ID : Constants.INACTIVESTOPMARKER_ID);
            marker.setAttribute(IMarker.MESSAGE, message);
            return EclipseMarker.wrap(marker);
        } catch (final CoreException e) {
            throw new ReviewRemarkException(e);
        }
    }

    @Override
    public IStopMarker createStopMarker(IResource resource, boolean tourActive, String message, IFragment pos) {
        EclipseMarker marker = (EclipseMarker) createStopMarker(resource, tourActive, message);
        if (!lookupTables.containsKey(resource)) {
            lookupTables.put(resource, PositionLookupTable.create((IFile) resource));
        }
        marker.setAttribute(IMarker.LINE_NUMBER, pos.getFrom().getLine());
        marker.setAttribute(IMarker.CHAR_START,
                lookupTables.get(resource).getCharsSinceFileStart(pos.getFrom()));
        marker.setAttribute(IMarker.CHAR_END,
                lookupTables.get(resource).getCharsSinceFileStart(pos.getTo()));
        return marker;
    }
    
}
