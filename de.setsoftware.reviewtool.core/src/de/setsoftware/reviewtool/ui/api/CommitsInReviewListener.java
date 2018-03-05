package de.setsoftware.reviewtool.ui.api;

import java.util.List;

import de.setsoftware.reviewtool.model.api.ICommit;

/**
 * Observer interface for classes listening for selected commits.
 */
public interface CommitsInReviewListener {

    /**
     * Is called when new commits are selected.
     */
    public abstract void notifyCommits(List<ICommit> currentCommits);
}
