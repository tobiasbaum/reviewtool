package de.setsoftware.reviewtool.plugin;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;

import de.setsoftware.reviewtool.model.Constants;
import de.setsoftware.reviewtool.model.IMarkerFactory;
import de.setsoftware.reviewtool.model.Position;
import de.setsoftware.reviewtool.model.PositionTransformer;

public class RealMarkerFactory implements IMarkerFactory {

    @Override
    public IMarker createMarker(Position pos) throws CoreException {
        return PositionTransformer.toResource(pos).createMarker(Constants.REVIEWMARKER_ID);
    }

}
