package de.setsoftware.reviewtool.ui.api;

import org.eclipse.core.commands.ExecutionException;

import de.setsoftware.reviewtool.model.ReviewStateManager;
import de.setsoftware.reviewtool.plugin.ReviewPlugin;
import de.setsoftware.reviewtool.ui.dialogs.DialogHelper;
import de.setsoftware.reviewtool.ui.dialogs.SelectTicketDialog;
import de.setsoftware.reviewtool.ui.popup.actions.StartReviewAction;

/**
 * Access to the review UI and related things for other plugins.
 */
public class ReviewUi {

    /**
     * Returns the name of the review ticket filter last used by the user.
     * If it is not set, the empty string is returned.
     */
    public static String getLastUsedReviewFilter() {
        return DialogHelper.getSetting(SelectTicketDialog.getFilterKey(true));
    }

    /**
     * Returns the current {@link ReviewStateManager}.
     */
    public static ReviewStateManager getReviewStateManager() {
        return ReviewPlugin.getPersistence();
    }

    /**
     * Acts as if the user clicked "start review".
     */
    public static void startReview() throws ExecutionException {
        new StartReviewAction().execute(null);
    }

}
