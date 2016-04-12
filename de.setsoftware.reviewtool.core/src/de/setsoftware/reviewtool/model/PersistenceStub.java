package de.setsoftware.reviewtool.model;

import java.util.Collections;
import java.util.List;

public class PersistenceStub implements IReviewPersistence {

    private String reviewData = "";
    private int roundIndex = 0;

    @Override
    public void saveReviewData(String ticketId, String newData) {
        this.reviewData = newData;
    }

    public void setReviewRound(int i) {
        this.roundIndex = i - 1;
    }

    @Override
    public List<TicketInfo> getReviewableTickets() {
        return Collections.emptyList();
    }

    @Override
    public List<TicketInfo> getFixableTickets() {
        return Collections.emptyList();
    }

    @Override
    public ITicketData loadTicket(String ticketKey) {
        return new ITicketData() {
            @Override
            public String getReviewerForRound(int number) {
                return "TB";
            }

            @Override
            public String getReviewData() {
                return PersistenceStub.this.reviewData;
            }

            @Override
            public int getCurrentRound() {
                return PersistenceStub.this.roundIndex;
            }

            @Override
            public TicketInfo getTicketInfo() {
                return new TicketInfo("123", "asdf", "hjkl", "qwer", null);
            }
        };
    }

    @Override
    public void startReviewing(String ticketKey) {
    }

    @Override
    public void startFixing(String ticketKey) {
    }

    @Override
    public void changeStateToReadyForReview(String ticketKey) {
    }

    @Override
    public void changeStateAtReviewEnd(String ticketKey, EndTransition transition) {
    }

    @Override
    public List<EndTransition> getPossibleTransitionsForReviewEnd(String ticketKey) {
        throw new RuntimeException("not yet implemented");
    }

}
