package de.setsoftware.reviewtool.ui.popup.actions;

import org.eclipse.core.resources.IMarker;
import org.eclipse.ui.IMarkerResolution;

import de.setsoftware.reviewtool.base.ReviewtoolException;
import de.setsoftware.reviewtool.eclipse.model.EclipseMarker;
import de.setsoftware.reviewtool.model.remarks.ResolutionType;
import de.setsoftware.reviewtool.model.remarks.ReviewRemark;
import de.setsoftware.reviewtool.model.remarks.ReviewRemarkException;
import de.setsoftware.reviewtool.plugin.ReviewPlugin;
import de.setsoftware.reviewtool.telemetry.Telemetry;
import de.setsoftware.reviewtool.ui.dialogs.AddReplyDialog;
import de.setsoftware.reviewtool.ui.dialogs.InputDialogCallback;

/**
 * Action to add a question to a review remark.
 */
public class QuestionResolution implements IMarkerResolution {

    public static final QuestionResolution INSTANCE = new QuestionResolution();

    private QuestionResolution() {
    }

    @Override
    public String getLabel() {
        return "Question";
    }

    @Override
    public void run(final IMarker marker) {
        final ReviewRemark review = ReviewRemark.getFor(EclipseMarker.wrap(marker));
        AddReplyDialog.get(review, new InputDialogCallback() {
            @Override
            public void execute(String text) {
                try {
                    review.addComment(ReviewPlugin.getUserPref(), text);
                    review.setResolution(ResolutionType.QUESTION);
                    ReviewPlugin.getPersistence().saveRemark(review);
                    Telemetry.event("resolutionQuestion")
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
        return o instanceof QuestionResolution;
    }

    @Override
    public int hashCode() {
        return 1231232;
    }

}
