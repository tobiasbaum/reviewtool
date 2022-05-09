package de.setsoftware.reviewtool.ui.popup.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import de.setsoftware.reviewtool.base.ReviewtoolException;
import de.setsoftware.reviewtool.model.remarks.ResolutionType;
import de.setsoftware.reviewtool.model.remarks.ReviewRemark;
import de.setsoftware.reviewtool.model.remarks.ReviewRemarkException;
import de.setsoftware.reviewtool.plugin.ReviewPlugin;
import de.setsoftware.reviewtool.telemetry.Telemetry;
import de.setsoftware.reviewtool.ui.dialogs.AddReplyDialog;
import de.setsoftware.reviewtool.ui.dialogs.InputDialogCallback;

/**
 * Superclass for actions that set the resolution of selected items in the tree with review remarks.
 */
public abstract class MarkRemarkAction extends AbstractHandler {

    private final boolean withComment;
    private final ResolutionType resolution;
    private final String telemetryEvent;

    public MarkRemarkAction(boolean withComment, ResolutionType resolution, String telemetryEvent) {
        this.withComment = withComment;
        this.resolution = resolution;
        this.telemetryEvent = telemetryEvent;
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final ISelection selection = HandlerUtil.getActiveMenuSelectionChecked(event);
        if (!(selection instanceof TreeSelection)) {
            return null;
        }
        final TreeSelection ts = (TreeSelection) selection;

        @SuppressWarnings("unchecked")
        final List<ReviewRemark> remarks = new ArrayList<>(ts.toList());
        for (final ReviewRemark remark : remarks) {
            this.mark(remark);
        }
        return null;
    }

    private void mark(ReviewRemark remark) {
        if (this.withComment) {
            AddReplyDialog.get(remark, new InputDialogCallback() {
                @Override
                public void execute(String text) {
                    try {
                        remark.addComment(ReviewPlugin.getUserPref(), text);
                        MarkRemarkAction.this.saveResolution(remark);
                    } catch (final ReviewRemarkException e) {
                        throw new ReviewtoolException(e);
                    }
                }
            });
        } else {
            this.saveResolution(remark);
        }
    }

    private void saveResolution(ReviewRemark remark) {
        remark.setResolution(this.resolution);
        ReviewPlugin.getPersistence().saveRemark(remark);
        Telemetry.event(this.telemetryEvent)
                .param("pos", remark.getPositionString())
                .log();
    }
}
