package de.setsoftware.reviewtool.ui.dialogs;

import de.setsoftware.reviewtool.model.EndTransition;

/**
 * Interface for objects that are created by {@link EndReviewExtension} strategies to
 * keep the state needed for a dialog.
 */
public interface EndReviewExtensionData {

    /**
     * Is called when OK was pressed to submit the review data.
     * @return true iff the closing of the dialog shall be cancelled
     */
    public abstract boolean okPressed(EndTransition typeOfEnd);

}
