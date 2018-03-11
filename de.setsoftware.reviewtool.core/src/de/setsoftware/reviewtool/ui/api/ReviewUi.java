package de.setsoftware.reviewtool.ui.api;

import java.lang.ref.WeakReference;
import java.util.List;

import org.eclipse.core.commands.ExecutionException;

import de.setsoftware.reviewtool.model.ReviewStateManager;
import de.setsoftware.reviewtool.model.api.ICommit;
import de.setsoftware.reviewtool.model.changestructure.CommitsInReview;
import de.setsoftware.reviewtool.model.changestructure.CommitsInReview.CommitsInReviewListener;
import de.setsoftware.reviewtool.plugin.ReviewPlugin;
import de.setsoftware.reviewtool.plugin.ReviewPlugin.Mode;
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
     * Return true if the plugin is in idle mode, i.e. neither review nor fixing is active.
     */
    public static boolean isIdle() {
        return ReviewPlugin.getInstance().getMode() == Mode.IDLE;
    }

    /**
     * Acts as if the user clicked "start review".
     */
    public static void startReview() throws ExecutionException {
        new StartReviewAction().execute(null);
    }

    /**
     * Registers a new listener for selected commits in review. The listeners are
     * stored in {@link WeakReference} objects and also removed if they are not
     * strongly or softly reachable.
     */
    public static void registerCommitsInReviewListener(CommitsInReviewListener l) {
        CommitsInReview.registerListener(l);
    }

    /**
     * Returns commits under review. Only meaningful in review mode.
     */
    public static List<? extends ICommit> getCommitsInReview() {
        return CommitsInReview.getCommits();
    }
}
