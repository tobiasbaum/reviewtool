package de.setsoftware.reviewtool.eclipse.model;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import de.setsoftware.reviewtool.model.Constants;
import de.setsoftware.reviewtool.model.PositionTransformer;
import de.setsoftware.reviewtool.model.remarks.IReviewMarker;
import de.setsoftware.reviewtool.model.remarks.IReviewResource;
import de.setsoftware.reviewtool.model.remarks.Position;
import de.setsoftware.reviewtool.model.remarks.ReviewRemarkException;

/**
 * A resource that is referenced as an Eclipse {@link IResource}.
 */
public class EclipseResource implements IReviewResource {

    private final IResource resource;

    public EclipseResource(IResource resource) {
        this.resource = resource;
    }

    @Override
    public IReviewMarker createReviewMarker() throws ReviewRemarkException {
        try {
            return EclipseMarker.create(this.resource.createMarker(Constants.REVIEWMARKER_ID));
        } catch (final CoreException e) {
            throw new ReviewRemarkException(e);
        }
    }

    @Override
    public Position createPosition(int line) {
        return PositionTransformer.toPosition(this.resource.getFullPath(), line, this.resource.getWorkspace());
    }

}
