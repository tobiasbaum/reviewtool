package de.setsoftware.reviewtool.ui.popup.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import de.setsoftware.reviewtool.model.remarks.DummyMarker;
import de.setsoftware.reviewtool.model.remarks.ReviewData;
import de.setsoftware.reviewtool.model.remarks.ReviewRemark;
import de.setsoftware.reviewtool.model.remarks.ReviewRound;
import de.setsoftware.reviewtool.plugin.ReviewPlugin;
import de.setsoftware.reviewtool.ui.dialogs.CorrectSyntaxDialog;
import de.setsoftware.reviewtool.ui.views.FixingTasksView;

/**
 * Action that jumps to the next review remark that needs to be addressed.
 */
public class JumpToNextOpenRemarkAction extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final Shell shell = HandlerUtil.getActiveShell(event);


        final ReviewData reviewData =
                CorrectSyntaxDialog.getCurrentReviewDataParsed(ReviewPlugin.getPersistence(), DummyMarker.FACTORY);
        if (reviewData == null) {
            MessageDialog.openInformation(shell, "No review data", "No review data available");
            return null;
        }

        final ReviewRemark remark = this.findNextOpenRemark(reviewData);
        if (remark == null) {
            MessageDialog.openInformation(shell, "No further remarks",
                    "There are no remarks left that have not been addressed.");
            return null;
        }

        FixingTasksView.jumpToRemark(remark);
        return null;
    }

    private ReviewRemark findNextOpenRemark(ReviewData reviewData) {
        for (final ReviewRound round : reviewData.getReviewRounds()) {
            for (final ReviewRemark remark : round.getRemarks()) {
                if (remark.needsFixing()) {
                    return remark;
                }
            }
        }
        return null;
    }

}
