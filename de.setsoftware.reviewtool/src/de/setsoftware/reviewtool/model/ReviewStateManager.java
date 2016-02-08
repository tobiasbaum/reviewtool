package de.setsoftware.reviewtool.model;

public class ReviewStateManager {

	private final IReviewPersistence persistence;
	private final ITicketChooser ticketChooser;

	private String ticketKey;

	public ReviewStateManager(IReviewPersistence persistence, ITicketChooser ticketChooser) {
		this.persistence = persistence;
		this.ticketChooser = ticketChooser;
	}

	/**
	 * Returns the serialized review information from the ticket currently under review.
	 */
	public String getCurrentReviewData() {
		final ITicketData ticket = this.loadTicketDataAndCheckExistence(true);
		if (ticket == null) {
			return null;
		}
		return ticket.getReviewData();
	}

	public void saveCurrentReviewData(String newData) {
		this.loadTicketDataAndCheckExistence(true);
		this.persistence.saveReviewData(this.ticketKey, newData);
	}

	public int getCurrentRound() {
		final ITicketData ticket = this.loadTicketDataAndCheckExistence(true);
		return ticket.getCurrentRound();
	}

	public String getReviewerForRound(int number) {
		final ITicketData ticket = this.loadTicketDataAndCheckExistence(true);
		return ticket.getReviewerForRound(number);
	}

	private ITicketData loadTicketDataAndCheckExistence(boolean forReview) {
		if (this.ticketKey == null) {
			ITicketData data;
			do {
				this.ticketKey = this.ticketChooser.choose(this.persistence, "", forReview);
				if (this.ticketKey == null) {
					return null;
				}
				data = this.persistence.loadTicket(this.ticketKey);
			} while (data == null);
			return data;
		} else {
			ITicketData data = this.persistence.loadTicket(this.ticketKey);
			while (data == null) {
				this.ticketKey = this.ticketChooser.choose(this.persistence, this.ticketKey, forReview);
				if (this.ticketKey == null) {
					return null;
				}
				data = this.persistence.loadTicket(this.ticketKey);
			}
			return data;
		}
	}

	public boolean selectTicket(boolean forReview) {
		final boolean success = this.loadTicketDataAndCheckExistence(forReview) != null;
		if (!success) {
			return false;
		}
		if (forReview) {
			this.persistence.startReviewing(this.ticketKey);
		} else {
			this.persistence.startFixing(this.ticketKey);
		}
		return true;
	}

	public void resetKey() {
		this.ticketKey = null;
	}

}
