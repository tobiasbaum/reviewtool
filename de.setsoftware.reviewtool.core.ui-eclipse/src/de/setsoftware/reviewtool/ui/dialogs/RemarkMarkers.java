package de.setsoftware.reviewtool.ui.dialogs;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

import de.setsoftware.reviewtool.model.Constants;
import de.setsoftware.reviewtool.model.ReviewStateManager;

/**
 * Helper methods for review remark markers.
 */
public class RemarkMarkers {

    private RemarkMarkers() {
    }

    public static void loadRemarks(ReviewStateManager persistence) {
        CorrectSyntaxDialog.getCurrentReviewDataParsed(persistence, new RealMarkerFactory());
    }

    public static void clearMarkers() throws CoreException {
        ResourcesPlugin.getWorkspace().getRoot().deleteMarkers(
                Constants.REVIEWMARKER_ID, true, IResource.DEPTH_INFINITE);
    }

}
