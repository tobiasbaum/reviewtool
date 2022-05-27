package de.setsoftware.reviewtool.model;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.IPath;

import de.setsoftware.reviewtool.model.remarks.DummyMarker;
import de.setsoftware.reviewtool.model.remarks.IReviewMarker;
import de.setsoftware.reviewtool.model.remarks.IReviewResource;
import de.setsoftware.reviewtool.model.remarks.Position;
import de.setsoftware.reviewtool.model.remarks.ReviewRemarkException;

/**
 * A resource that is not imported into Eclipse and therefore has to be
 * referenced by an {@link IPath}.
 */
public class PathResource implements IReviewResource {

    private final IPath path;
    private final IWorkspace workspace;

    public PathResource(IPath path, IWorkspace workspace) {
        this.path = path;
        this.workspace = workspace;
    }

    @Override
    public IReviewMarker createReviewMarker() throws ReviewRemarkException {
        return new DummyMarker();
    }

    @Override
    public Position createPosition(int line) {
        return PositionTransformer.toPosition(this.path, line, this.workspace);
    }

}
