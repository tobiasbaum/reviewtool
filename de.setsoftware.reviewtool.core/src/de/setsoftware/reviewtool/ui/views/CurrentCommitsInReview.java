package de.setsoftware.reviewtool.ui.views;

import java.util.ArrayList;
import java.util.List;

import de.setsoftware.reviewtool.base.WeakListeners;
import de.setsoftware.reviewtool.model.api.ICommit;
import de.setsoftware.reviewtool.ui.api.CommitsInReviewListener;

/**
 * Manages the currently selected commits for review.
 */
public class CurrentCommitsInReview {

    private static List<ICommit> currentCommits = new ArrayList<>();
    private static WeakListeners<CommitsInReviewListener> listeners = new WeakListeners<>();

    /**
     * Set the currently selected commits for review.
     */
    public static void setCommits(List<ICommit> commits) {
        if (commits == null) {
            currentCommits = new ArrayList<>();
        } else {
            currentCommits = commits;
        }
        notifyListener();
    }

    private static void notifyListener() {
        for (CommitsInReviewListener l : listeners) {
            l.notifyCommits(currentCommits);
        }
    }

    public static List<ICommit> getCommits() {
        return currentCommits;
    }

    public static void registerListener(CommitsInReviewListener l) {
        listeners.add(l);
    }
}
