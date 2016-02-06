package de.setsoftware.reviewtool.model;

public class ReviewStateManager {

	private final IReviewPersistence persistence;
	private final ITicketChooser ticketChooser;
	private final String defaultReviewer;

	private String ticketKey;

	public ReviewStateManager(
			String defaultReviewer, IReviewPersistence persistence, ITicketChooser ticketChooser) {
		this.defaultReviewer = defaultReviewer;
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
		final String reviewer = ticket.getReviewerForRound(number);
		return reviewer == null ? this.defaultReviewer : reviewer;
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
		return this.loadTicketDataAndCheckExistence(forReview) != null;
	}

	public void resetKey() {
		this.ticketKey = null;
	}

}
