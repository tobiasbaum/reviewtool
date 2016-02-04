package de.setsoftware.reviewtool.model;

import java.util.Collections;
import java.util.List;

public class PersistenceStub implements IReviewPersistence {

	private String reviewData = "";
	private int roundIndex = 0;

	@Override
	public String getCurrentReviewData() {
		return this.reviewData;
	}

	@Override
	public void saveCurrentReviewData(String newData) {
		this.reviewData = newData;
	}

	@Override
	public int getCurrentRound() {
		return this.roundIndex;
	}

	public void setReviewRound(int i) {
		this.roundIndex = i - 1;
	}

	@Override
	public String getReviewerForRound(int number) {
		return "TB";
	}

	@Override
	public List<TicketInfo> getReviewableTickets() {
		return Collections.emptyList();
	}

	@Override
	public List<TicketInfo> getFixableTickets() {
		return Collections.emptyList();
	}

}
