package de.setsoftware.reviewtool.ui.popup.actions;

import org.eclipse.core.resources.IMarker;
import org.eclipse.ui.IMarkerResolution;

import de.setsoftware.reviewtool.base.ReviewtoolException;
import de.setsoftware.reviewtool.model.EclipseMarker;
import de.setsoftware.reviewtool.model.remarks.ResolutionType;
import de.setsoftware.reviewtool.model.remarks.ReviewRemark;
import de.setsoftware.reviewtool.model.remarks.ReviewRemarkException;
import de.setsoftware.reviewtool.plugin.ReviewPlugin;
import de.setsoftware.reviewtool.telemetry.Telemetry;
import de.setsoftware.reviewtool.ui.dialogs.AddReplyDialog;
import de.setsoftware.reviewtool.ui.dialogs.InputDialogCallback;

/**
 * Quick fix resolution that marks a review remark as fixed and also adds a comment/reply.
 */
public class FixedAndCommentResolution implements IMarkerResolution {

    public static final FixedAndCommentResolution INSTANCE = new FixedAndCommentResolution();

    private FixedAndCommentResolution() {
    }

    @Override
    public String getLabel() {
        return "Fixed + Reply";
    }

    @Override
    public void run(final IMarker marker) {
        final ReviewRemark review = ReviewRemark.getFor(new EclipseMarker(marker));
        AddReplyDialog.get(review, new InputDialogCallback() {
            @Override
            public void execute(String text) {
                try {
                    review.addComment(ReviewPlugin.getUserPref(), text);
                    review.setResolution(ResolutionType.FIXED);
                    ReviewPlugin.getPersistence().saveRemark(review);
                    Telemetry.event("resolutionFixedAndComment")
                        .param("resource", marker.getResource())
                        .param("line", marker.getAttribute(IMarker.LINE_NUMBER, -1))
                        .log();
                } catch (final ReviewRemarkException e) {
                    throw new ReviewtoolException(e);
                }
            }
        });
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof FixedAndCommentResolution;
    }

    @Override
    public int hashCode() {
        return 786538124;
    }

}
