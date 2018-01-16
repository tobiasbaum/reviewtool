package de.setsoftware.reviewtool.model;

import java.util.Date;

/**
 * Interface for things that can provide the review data of a ticket and further ticket information.
 */
public interface ITicketData {

    /**
     * Returns the tickets ID.
     */
    public abstract String getId();

    /**
     * Returns the string representation of the review remarks.
     */
    public abstract String getReviewData();

    /**
     * Returns the reviewer id for the review round with the given number.
     * number starts with 1.
     * When there is no review round with the given number, returns the current user.
     */
    public abstract String getReviewerForRound(int number);

    /**
     * Returns the end timestamp for the review round with the given number.
     * number starts with 1.
     * When there is no review round with the given number, returns the current date.
     */
    public abstract Date getEndTimeForRound(int number);

    /**
     * Returns the current review round number, including running reviews.
     * I.e. when the first review is running, it returns 1.
     */
    public abstract int getCurrentRound();

    public abstract TicketInfo getTicketInfo();

}
