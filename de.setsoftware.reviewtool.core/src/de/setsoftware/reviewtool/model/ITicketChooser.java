package de.setsoftware.reviewtool.model;

/**
 * Interface to decouple the model from the user interaction needed to choose a ticket.
 */
public interface ITicketChooser {

    /**
     * Let's the user choose a ticket key, either for review or fix fixing.
     */
    public abstract String choose(
            IReviewPersistence persistence, String ticketKeyDefault, boolean forReview);

}
