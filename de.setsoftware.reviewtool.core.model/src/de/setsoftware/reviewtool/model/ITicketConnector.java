package de.setsoftware.reviewtool.model;

import java.util.List;
import java.util.Set;

/**
 * Abstraction of the different ways to get possible work and to save review information.
 */
public interface ITicketConnector {

    /**
     * Returns the names of filters from which the user can choose when starting a review.
     * There must be at least one such filter.
     */
    public abstract Set<String> getFilterNamesForReview();

    /**
     * Returns the names of filters from which the user can choose when starting a fixing.
     * There must be at least one such filter.
     */
    public abstract Set<String> getFilterNamesForFixing();

    /**
     * Returns all tickets that match the given filter.
     */
    public abstract List<TicketInfo> getTicketsForFilter(String filterName);

    public abstract void saveReviewData(String ticketKey, String newData);

    public abstract ITicketData loadTicket(String ticketKey);

    /**
     * Changes the state of the given ticket to "in review".
     * If it already is in this state or the transition is not allowed, nothing happens.
     */
    public abstract void startReviewing(String ticketKey);

    /**
     * Changes the state of the given ticket to "in implementation (for fixing)".
     * If it already is in this state or the transition is not allowed, nothing happens.
     */
    public abstract void startFixing(String ticketKey);

    /**
     * Changes the state of the given ticket to "ready for review".
     * If it already is in this state or the transition is not allowed, nothing happens.
     */
    public abstract void changeStateToReadyForReview(String ticketKey);

    /**
     * Returns the transitions that are possible to end the review of the given ticket.
     * At least one of these transitions should be of type "OK" and one of type "REJECTION".
     */
    public abstract List<EndTransition> getPossibleTransitionsForReviewEnd(String ticketKey);

    /**
     * Changes the state of the given ticket according to the given transition.
     * If it already is in this state or the transition is not allowed, nothing happens.
     */
    public abstract void changeStateAtReviewEnd(String ticketKey, EndTransition transition);

    /**
     * Returns the settings that can be used to create a link to ticket's details.
     * Returns null when no link should be shown.
     */
    public abstract TicketLinkSettings getLinkSettings();

}
