package de.setsoftware.reviewtool.ui.popup.actions;

import de.setsoftware.reviewtool.model.remarks.ResolutionType;

/**
 * Action to mark the selected remarks as "wont fix" (with adding a comment).
 */
public class MarkRemarkWontfixAction extends MarkRemarkAction {

    public MarkRemarkWontfixAction() {
        super(true, ResolutionType.WONT_FIX, "resolutionWontFix");
    }

}

