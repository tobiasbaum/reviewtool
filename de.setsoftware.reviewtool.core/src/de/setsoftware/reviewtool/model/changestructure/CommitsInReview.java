package de.setsoftware.reviewtool.model.changestructure;

import java.util.ArrayList;
import java.util.List;

import de.setsoftware.reviewtool.base.WeakListeners;
import de.setsoftware.reviewtool.model.api.ICommit;

/**
 * Manages the currently selected commits for review.
 */
public class CommitsInReview {
    /**
     * Observer interface for classes listening for selected commits.
     */
    public static interface CommitsInReviewListener {

        /**
         * Is called when new commits are selected.
         */
        public abstract void notifyCommits(List<ICommit> currentCommits);
    }

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
