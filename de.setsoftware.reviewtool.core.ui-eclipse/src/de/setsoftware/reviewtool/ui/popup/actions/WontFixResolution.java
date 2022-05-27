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
 * Quick fix resolution that marks a review remark as "wont fix" (false positive).
 */
public class WontFixResolution implements IMarkerResolution {

    public static final WontFixResolution INSTANCE = new WontFixResolution();

    private WontFixResolution() {
    }

    @Override
    public String getLabel() {
        return "Refuse fixing";
    }

    @Override
    public void run(final IMarker marker) {
        final ReviewRemark review = ReviewRemark.getFor(EclipseMarker.wrap(marker));
        AddReplyDialog.get(review, new InputDialogCallback() {
            @Override
            public void execute(String text) {
                try {
                    review.addComment(ReviewPlugin.getUserPref(), text);
                    review.setResolution(ResolutionType.WONT_FIX);
                    ReviewPlugin.getPersistence().saveRemark(review);
                    Telemetry.event("resolutionWontFix")
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
        return o instanceof WontFixResolution;
    }

    @Override
    public int hashCode() {
        return 6547659;
    }

}
