package de.setsoftware.reviewtool.ui.dialogs;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import de.setsoftware.reviewtool.model.IMarkerFactory;
import de.setsoftware.reviewtool.model.Position;
import de.setsoftware.reviewtool.model.PositionTransformer;

/**
 * A factory to create normal Eclipse markers.
 */
public class RealMarkerFactory implements IMarkerFactory {

    @Override
    public IMarker createMarker(Position pos, String markerId) throws CoreException {
        return PositionTransformer.toResource(pos).createMarker(markerId);
    }

    @Override
    public IMarker createMarker(IResource resource, String markerId) throws CoreException {
        return resource.createMarker(markerId);
    }

}
