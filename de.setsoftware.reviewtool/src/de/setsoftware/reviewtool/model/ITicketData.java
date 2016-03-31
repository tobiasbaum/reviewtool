package de.setsoftware.reviewtool.model;

public interface ITicketData {

    public abstract String getReviewData();

    /**
     * Returns the reviewer id for the review round with the given number.
     * number starts with 1.
     * When there is no review round with the given number, returns the current user.
     */
    public abstract String getReviewerForRound(int number);

    /**
     * Returns the current review round number, including running reviews.
     * I.e. when the first review is running, it returns 1.
     */
    public abstract int getCurrentRound();

    public abstract TicketInfo getTicketInfo();

}
