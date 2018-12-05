package de.setsoftware.reviewtool.ui.popup.actions;

import de.setsoftware.reviewtool.model.remarks.ResolutionType;

/**
 * Action to mark the selected remarks as unclear (with adding a question).
 */
public class MarkRemarkUnclearAction extends MarkRemarkAction {

    public MarkRemarkUnclearAction() {
        super(true, ResolutionType.QUESTION, "resolutionQuestion");
    }

}

