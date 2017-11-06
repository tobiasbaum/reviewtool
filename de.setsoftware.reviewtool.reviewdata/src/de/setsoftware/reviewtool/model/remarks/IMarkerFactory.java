package de.setsoftware.reviewtool.model.remarks;

/**
 * Interface to decouple code that uses markers from a full-blown Eclipse.
 */
public interface IMarkerFactory {

    /**
     * Creates a marker with the given type, attached to the resource from the given position.
     */
    public abstract IReviewMarker createMarker(Position pos) throws ReviewRemarkException;

}
