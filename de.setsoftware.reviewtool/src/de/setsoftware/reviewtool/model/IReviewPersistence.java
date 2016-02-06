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

}
