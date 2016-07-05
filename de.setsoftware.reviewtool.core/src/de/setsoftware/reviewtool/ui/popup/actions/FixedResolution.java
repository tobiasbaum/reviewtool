package de.setsoftware.reviewtool.ui.popup.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.views.markers.WorkbenchMarkerResolution;

import de.setsoftware.reviewtool.base.ReviewtoolException;
import de.setsoftware.reviewtool.model.Constants;
import de.setsoftware.reviewtool.model.ResolutionType;
import de.setsoftware.reviewtool.model.ReviewRemark;
import de.setsoftware.reviewtool.plugin.ReviewPlugin;
import de.setsoftware.reviewtool.telemetry.Telemetry;

/**
 * Quick fix resolution that marks a review remark as fixed.
 */
public class FixedResolution extends WorkbenchMarkerResolution {

    public static final FixedResolution INSTANCE = new FixedResolution();

    private FixedResolution() {
    }

    @Override
    public String getLabel() {
        return "Eingepflegt";
    }

    @Override
    public void run(IMarker marker) {
        try {
            final ReviewRemark review = ReviewRemark.getFor(ReviewPlugin.getPersistence(), marker);
            review.setResolution(ResolutionType.FIXED);
            review.save();
            Telemetry.get().resolutionFixed(
                    marker.getResource().toString(),
                    marker.getAttribute(IMarker.LINE_NUMBER, -1));
        } catch (final CoreException e) {
            throw new ReviewtoolException(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof FixedResolution;
    }

    @Override
    public int hashCode() {
        return 89754;
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
