package de.setsoftware.reviewtool.model;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * An implementation of {@link ITicketConnector} that does not persist anything.
 */
public class PersistenceStub implements ITicketConnector {

    private String reviewData = "";
    private int roundNumber = 1;

    @Override
    public void saveReviewData(String ticketId, String newData) {
        this.reviewData = newData;
    }

    public void setReviewRound(int i) {
        this.roundNumber = i;
    }

    @Override
    public Set<String> getFilterNamesForReview() {
        return Collections.emptySet();
    }

    @Override
    public Set<String> getFilterNamesForFixing() {
        return Collections.emptySet();
    }

    @Override
    public List<TicketInfo> getTicketsForFilter(String filterName) {
        return Collections.emptyList();
    }


    @Override
    public ITicketData loadTicket(final String ticketKey) {
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
                return PersistenceStub.this.roundNumber;
            }

            @Override
            public TicketInfo getTicketInfo() {
                return new TicketInfo(this.getId(), "asdf", "hjkl", "", "qwer", null,
                        Collections.<String>emptySet(), new Date(42));
            }

            @Override
            public String getId() {
                return ticketKey;
            }

            @Override
            public Date getEndTimeForRound(int number) {
                return new Date(123456789L);
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

    @Override
    public TicketLinkSettings getLinkSettings() {
        return null;
    }

}
