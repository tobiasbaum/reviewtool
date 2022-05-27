package de.setsoftware.reviewtool.model;

import java.io.File;

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

    private final File path;

    public PathResource(File path) {
        this.path = path;
    }

    @Override
    public IReviewMarker createReviewMarker() throws ReviewRemarkException {
        return new DummyMarker();
    }

    @Override
    public Position createPosition(int line) {
        return PositionTransformer.toPosition(this.path, line);
    }

}
