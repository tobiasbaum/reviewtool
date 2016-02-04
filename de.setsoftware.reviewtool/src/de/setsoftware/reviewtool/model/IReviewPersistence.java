package de.setsoftware.reviewtool.model;

import java.util.List;

public interface IReviewPersistence {

	public abstract String getCurrentReviewData();

	public abstract void saveCurrentReviewData(String newData);

	public abstract int getCurrentRound();

	public abstract String getReviewerForRound(int number);

	public abstract List<TicketInfo> getReviewableTickets();

	public abstract List<TicketInfo> getFixableTickets();

}
