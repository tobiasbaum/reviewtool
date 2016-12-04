package de.setsoftware.reviewtool.model.remarks;

/**
 * A resource that can contain review remarks.
 */
public interface IReviewResource {

    /**
     * Create a marker for this resource.
     */
    public abstract IReviewMarker createReviewMarker() throws ReviewRemarkException;

    /**
     * Create a position for the given line in this resource.
     */
    public abstract Position createPosition(int line);

}
