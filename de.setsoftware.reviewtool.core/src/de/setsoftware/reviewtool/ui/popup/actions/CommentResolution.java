package de.setsoftware.reviewtool.ui.popup.actions;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IMarkerResolution;

import de.setsoftware.reviewtool.base.ReviewtoolException;
import de.setsoftware.reviewtool.model.ResolutionType;
import de.setsoftware.reviewtool.model.ReviewRemark;
import de.setsoftware.reviewtool.plugin.ReviewPlugin;
import de.setsoftware.reviewtool.telemetry.Telemetry;
import de.setsoftware.reviewtool.ui.dialogs.AddReplyDialog;
import de.setsoftware.reviewtool.ui.dialogs.InputDialogCallback;

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
        final ReviewRemark review = ReviewRemark.getFor(ReviewPlugin.getPersistence(), marker);
        AddReplyDialog.get(review, new InputDialogCallback() {
            @Override
            public void execute(String text) {
                try {
                    review.addComment(text);
                    review.setResolution(ResolutionType.OPEN);
                    review.save();
                    Telemetry.get().resolutionComment(
                            marker.getResource().toString(),
                            marker.getAttribute(IMarker.LINE_NUMBER, -1));
                } catch (final CoreException e) {
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
