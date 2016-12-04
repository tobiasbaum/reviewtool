package de.setsoftware.reviewtool.model.changestructure;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

/**
 * Factory for {@link IMarker}s belonging to review stops.
 */
public interface IStopMarkerFactory {

    public abstract IMarker createStopMarker(IResource resource) throws CoreException;

}
