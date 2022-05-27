package de.setsoftware.reviewtool.model.changestructure;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;

import de.setsoftware.reviewtool.model.api.IFragment;

/**
 * Factory for {@link IMarker}s belonging to review stops.
 */
public interface IStopMarkerFactory {

    public abstract IStopMarker createStopMarker(IResource resource, boolean tourActive, String message);

    public abstract IStopMarker createStopMarker(IResource resource, boolean tourActive, String message, IFragment pos);

}
