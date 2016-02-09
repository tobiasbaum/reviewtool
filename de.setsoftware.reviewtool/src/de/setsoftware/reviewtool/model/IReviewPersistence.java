package de.setsoftware.reviewtool.model;

import java.util.List;

/**
 * Abstraction of the different ways to get possible work and to save review information.
 */
public interface IReviewPersistence {

	public abstract List<TicketInfo> getReviewableTickets();

	public abstract List<TicketInfo> getFixableTickets();

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
	 * Changes the state of the given ticket to "done".
	 * If it already is in this state or the transition is not allowed, nothing happens.
	 */
	public abstract void changeStateToDone(String ticketKey);

	/**
	 * Changes the state of the given ticket to "rejected".
	 * If it already is in this state or the transition is not allowed, nothing happens.
	 */
	public abstract void changeStateToRejected(String ticketKey);

}
