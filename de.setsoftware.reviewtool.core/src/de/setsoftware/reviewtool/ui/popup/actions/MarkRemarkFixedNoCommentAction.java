package de.setsoftware.reviewtool.ui.popup.actions;

import de.setsoftware.reviewtool.model.remarks.ResolutionType;

/**
 * Action to mark the selected remarks as fixed (without adding a comment).
 */
public class MarkRemarkFixedNoCommentAction extends MarkRemarkAction {

    public MarkRemarkFixedNoCommentAction() {
        super(false, ResolutionType.FIXED, "resolutionFixed");
    }

}

