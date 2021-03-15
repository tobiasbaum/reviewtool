package de.setsoftware.reviewtool.ui.popup.actions;

import de.setsoftware.reviewtool.model.remarks.ResolutionType;

/**
 * Action to mark the selected remarks as fixed (with adding a comment).
 */
public class MarkRemarkFixedAction extends MarkRemarkAction {

    public MarkRemarkFixedAction() {
        super(true, ResolutionType.FIXED, "resolutionFixedAndComment");
    }

}
