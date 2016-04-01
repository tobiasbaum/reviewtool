package de.setsoftware.reviewtool.popup.actions;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IMarkerResolution;

import de.setsoftware.reviewtool.base.ReviewtoolException;
import de.setsoftware.reviewtool.dialogs.AddReplyDialog;
import de.setsoftware.reviewtool.dialogs.InputDialogCallback;
import de.setsoftware.reviewtool.model.ResolutionType;
import de.setsoftware.reviewtool.model.ReviewRemark;
import de.setsoftware.reviewtool.plugin.ReviewPlugin;

public class CommentResolution implements IMarkerResolution {

    public static final CommentResolution INSTANCE = new CommentResolution();

    private CommentResolution() {
    }

    @Override
    public String getLabel() {
        return "Kommentar zu Reviewanmerkung ergänzen";
    }

    @Override
    public void run(IMarker marker) {
        final ReviewRemark review = ReviewRemark.getFor(ReviewPlugin.getPersistence(), marker);
        AddReplyDialog.get(review, new InputDialogCallback() {
            @Override
            public void execute(String text) {
                try {
                    review.addComment(text);
                    review.setResolution(ResolutionType.OPEN);
                    review.save();
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
