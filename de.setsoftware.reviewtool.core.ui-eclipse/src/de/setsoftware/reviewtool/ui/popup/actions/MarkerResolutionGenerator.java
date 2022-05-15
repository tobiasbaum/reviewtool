package de.setsoftware.reviewtool.ui.popup.actions;

import org.eclipse.core.resources.IMarker;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator2;

import de.setsoftware.reviewtool.model.api.Mode;
import de.setsoftware.reviewtool.plugin.ReviewPlugin;

/**
 * Creates quick fix resolutions for the the review markers.
 */
public class MarkerResolutionGenerator implements IMarkerResolutionGenerator2 {

    @Override
    public boolean hasResolutions(IMarker marker) {
        return ReviewPlugin.getInstance().getMode() != Mode.IDLE;
    }

    @Override
    public IMarkerResolution[] getResolutions(IMarker marker) {
        switch (ReviewPlugin.getInstance().getMode()) {
        case FIXING:
            return new IMarkerResolution[] {
                    FixedResolution.INSTANCE,
                    FixedAndCommentResolution.INSTANCE,
                    WontFixResolution.INSTANCE,
                    QuestionResolution.INSTANCE
            };
        case REVIEWING:
            //TODO  ändern
            return new IMarkerResolution[] {
                    DeleteResolution.INSTANCE,
                    CommentResolution.INSTANCE
            };
        case IDLE:
        default:
            return new IMarkerResolution[0];
        }
    }

}
