package de.setsoftware.reviewtool.ui.popup.actions;

import org.eclipse.core.resources.IMarker;
import org.eclipse.ui.IMarkerResolution;

import de.setsoftware.reviewtool.base.ReviewtoolException;
import de.setsoftware.reviewtool.model.remarks.ResolutionType;
import de.setsoftware.reviewtool.model.remarks.ReviewRemark;
import de.setsoftware.reviewtool.model.remarks.ReviewRemarkException;
import de.setsoftware.reviewtool.plugin.ReviewPlugin;
import de.setsoftware.reviewtool.telemetry.Telemetry;
import de.setsoftware.reviewtool.ui.dialogs.AddReplyDialog;
import de.setsoftware.reviewtool.ui.dialogs.InputDialogCallback;
import de.setsoftware.reviewtool.ui.views.EclipseMarker;

/**
 * Quickfix resolution that adds a comment to an existing remark.
 */
public class CommentResolution implements IMarkerResolution {

    public static final CommentResolution INSTANCE = new CommentResolution();

    private CommentResolution() {
    }

    @Override
    public String getLabel() {
        return "Add comment to review remark";
    }

    @Override
    public void run(final IMarker marker) {
        final ReviewRemark review = ReviewRemark.getFor(EclipseMarker.wrap(marker));
        AddReplyDialog.get(review, new InputDialogCallback() {
            @Override
            public void execute(String text) {
                try {
                    review.addComment(ReviewPlugin.getUserPref(), text);
                    review.setResolution(ResolutionType.OPEN);
                    ReviewPlugin.getPersistence().saveRemark(review);
                    Telemetry.event("resolutionComment")
                            .param("resource", marker.getResource())
                            .param("line", marker.getAttribute(IMarker.LINE_NUMBER, -1))
                            .log();
                } catch (final ReviewRemarkException e) {
                    ReviewPlugin.getInstance().logException(e);
                    throw new ReviewtoolException(e);
                }
            }
        });
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof CommentResolution;
    }

    @Override
    public int hashCode() {
        return 4379876;
    }

}
