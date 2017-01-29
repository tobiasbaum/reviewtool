package de.setsoftware.reviewtool.reminder;

import java.util.List;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import de.setsoftware.reviewtool.base.Logger;
import de.setsoftware.reviewtool.model.TicketInfo;
import de.setsoftware.reviewtool.ui.api.ReviewUi;

/**
 * Checks if there are too many open reviews and shows a reminder.
 */
public class Reminder implements Runnable {

    private static final int CHECK_DELAY = 23 * 60 * 60 * 1000;

    private final int minCount;

    public Reminder(int minCount) {
        this.minCount = minCount;
    }

    @Override
    public void run() {
        Logger.debug("reminder check runs");
        final List<TicketInfo> tickets = ReviewUi.getReviewStateManager().getTicketsForFilter(
                ReviewUi.getLastUsedReviewFilter(), true);

        final boolean review;
        if (tickets.size() >= this.minCount) {
            review = MessageDialog.openQuestion(null, "Time to review",
                    String.format("There are %d open reviews. That's a lot. Start review?", tickets.size()));
        } else {
            review = false;
        }

        if (review) {
            try {
                ReviewUi.startReview();
            } catch (final ExecutionException e) {
                Logger.error("error caught", e);
            }
        }

        Display.getCurrent().timerExec(CHECK_DELAY, this);
    }

}
