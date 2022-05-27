package de.setsoftware.reviewtool.model.changestructure;

import de.setsoftware.reviewtool.model.api.IFragment;
import de.setsoftware.reviewtool.model.api.IRevisionedFile;

/**
 * Factory for {@link IStopMarker}s belonging to review stops.
 */
public interface IStopMarkerFactory {

    public abstract IStopMarker createStopMarker(
            IRevisionedFile file, boolean tourActive, String message);

    public abstract IStopMarker createStopMarker(
            IRevisionedFile file, boolean tourActive, String message, IFragment pos);

}
