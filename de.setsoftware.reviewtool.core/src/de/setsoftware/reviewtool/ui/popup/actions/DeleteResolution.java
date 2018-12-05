package de.setsoftware.reviewtool.ui.popup.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.views.markers.WorkbenchMarkerResolution;

import de.setsoftware.reviewtool.base.ReviewtoolException;
import de.setsoftware.reviewtool.model.Constants;
import de.setsoftware.reviewtool.model.EclipseMarker;
import de.setsoftware.reviewtool.model.remarks.ReviewRemark;
import de.setsoftware.reviewtool.model.remarks.ReviewRemarkException;
import de.setsoftware.reviewtool.plugin.ReviewPlugin;
import de.setsoftware.reviewtool.telemetry.Telemetry;

/**
 * Quickfix resolution that deletes a review remark.
 */
public class DeleteResolution extends WorkbenchMarkerResolution {

    public static final DeleteResolution INSTANCE = new DeleteResolution();

    private DeleteResolution() {
    }

    @Override
    public String getLabel() {
        return "Delete review remark";
    }

    @Override
    public void run(IMarker marker) {
        final ReviewRemark review = ReviewRemark.getFor(EclipseMarker.wrap(marker));
        try {
            ReviewPlugin.getPersistence().deleteRemark(review);
            Telemetry.event("resolutionDelete")
                .param("resource", marker.getResource())
                .param("line", marker.getAttribute(IMarker.LINE_NUMBER, -1))
                .log();
        } catch (final ReviewRemarkException e) {
            ReviewPlugin.getInstance().logException(e);
            throw new ReviewtoolException(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof DeleteResolution;
    }

    @Override
    public int hashCode() {
        return 762347;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public Image getImage() {
        return null;
    }

    @Override
    public IMarker[] findOtherMarkers(IMarker[] markers) {
        final List<IMarker> ofFittingType = new ArrayList<>();
        for (final IMarker m : markers) {
            try {
                if (m.isSubtypeOf(Constants.REVIEWMARKER_ID)) {
                    ofFittingType.add(m);
                }
            } catch (final CoreException e) {
                throw new ReviewtoolException(e);
            }
        }
        return ofFittingType.toArray(new IMarker[ofFittingType.size()]);
    }

}

