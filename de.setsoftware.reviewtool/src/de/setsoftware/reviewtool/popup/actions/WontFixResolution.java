package de.setsoftware.reviewtool.popup.actions;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IMarkerResolution;

import de.setsoftware.reviewtool.dialogs.AddReplyDialog;
import de.setsoftware.reviewtool.dialogs.InputDialogCallback;
import de.setsoftware.reviewtool.model.ResolutionType;
import de.setsoftware.reviewtool.model.ReviewRemark;
import de.setsoftware.reviewtool.plugin.ReviewPlugin;

public class WontFixResolution implements IMarkerResolution {

    public static final WontFixResolution INSTANCE = new WontFixResolution();

    private WontFixResolution() {
    }

    @Override
    public String getLabel() {
        return "Ablehnen";
    }

    @Override
    public void run(IMarker marker) {
        final ReviewRemark review = ReviewRemark.getFor(ReviewPlugin.getPersistence(), marker);
        AddReplyDialog.get(review, new InputDialogCallback() {
            @Override
            public void execute(String text) {
                try {
                    review.addComment(text);
                    review.setResolution(ResolutionType.WONT_FIX);
                    review.save();
                } catch (final CoreException e) {
                    throw new RuntimeException(e);
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
