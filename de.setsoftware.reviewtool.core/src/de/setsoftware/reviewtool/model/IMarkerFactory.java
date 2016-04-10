package de.setsoftware.reviewtool.model;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

/**
 * Interface to decouple code that uses markers from a full-blown Eclipse.
 */
public interface IMarkerFactory {

    /**
     * Creates a marker with the given type, attached to the resource from the given position.
     */
    public abstract IMarker createMarker(Position pos, String markerId) throws CoreException;

    /**
     * Creates a marker with the given type, attached to the given resource.
     */
    public abstract IMarker createMarker(IResource resource, String markerId) throws CoreException;

}
