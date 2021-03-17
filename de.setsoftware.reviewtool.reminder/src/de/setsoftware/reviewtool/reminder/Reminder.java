package de.setsoftware.reviewtool.reminder;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import de.setsoftware.reviewtool.base.Logger;
import de.setsoftware.reviewtool.lifecycle.IPostInitTask;
import de.setsoftware.reviewtool.model.TicketInfo;
import de.setsoftware.reviewtool.ui.facade.ReviewUi;

/**
 * Checks if there are too many open reviews and shows a reminder.
 */
public class Reminder implements IPostInitTask {

    private static final int CHECK_DELAY = 23 * 60 * 60 * 1000;

    private final int minCount;
    private final int minDays;

    public Reminder(int minCount, int minDays) {
        this.minCount = minCount;
        this.minDays = minDays;
    }

    @Override
    public void run() {
        Logger.debug("reminder check runs");
        if (!ReviewUi.isIdle()) {
            Logger.debug("reminder check skipped because not idle");
            Display.getCurrent().timerExec(CHECK_DELAY, this);
            return;
        }
        List<TicketInfo> tickets;
        try {
            tickets = ReviewUi.getReviewStateManager().getTicketsForFilter(
                    ReviewUi.getLastUsedReviewFilter(), true);
        } catch (RuntimeException e) {
            Logger.warn("problem while getting tickets for reminder", e);
            tickets = Collections.emptyList();
        }
        final int maxDaysWaiting = this.determineMaxDaysWaiting(tickets);

        final boolean review;
        if (tickets.size() >= this.minCount) {
            review = MessageDialog.openQuestion(null, "Time to review",
                    String.format("There are %d open reviews. That's a lot. Start review?", tickets.size()));
        } else if (maxDaysWaiting >= this.minDays) {
            review = MessageDialog.openQuestion(null, "Time to review",
                        String.format("There are tickets that have been waiting for review for %d days."
                                + " That's a long time. Start review?", maxDaysWaiting));
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

    private int determineMaxDaysWaiting(List<TicketInfo> tickets) {
        int maxDaysWaiting = -1;
        final Date today = new Date();
        for (final TicketInfo ticket : tickets) {
            maxDaysWaiting = Math.max(maxDaysWaiting, ticket.getWaitingForDays(today));
        }
        return maxDaysWaiting;
    }

}
