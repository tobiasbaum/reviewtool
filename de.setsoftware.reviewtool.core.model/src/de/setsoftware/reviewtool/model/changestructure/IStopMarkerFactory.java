package de.setsoftware.reviewtool.model.changestructure;

import de.setsoftware.reviewtool.model.api.ICortResource;
import de.setsoftware.reviewtool.model.api.IStopMarker;

/**
 * Factory for {@link IMarker}s belonging to review stops.
 */
public interface IStopMarkerFactory {

    public abstract IStopMarker createStopMarker(
            ICortResource resource, boolean tourActive, String message);

    public abstract IStopMarker createStopMarker(
            ICortResource resource, boolean tourActive, String message, int line, int charStart, int charEnd);

    public abstract void clearStopMarkers();

}
