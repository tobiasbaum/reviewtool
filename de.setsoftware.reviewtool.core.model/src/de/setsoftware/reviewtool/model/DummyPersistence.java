package de.setsoftware.reviewtool.model;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Dummy ticket connector to be used when none is configured.
 */
public class DummyPersistence implements ITicketConnector {

    @Override
    public Set<String> getFilterNamesForReview() {
        throw new RuntimeException("Please configure CoRT");
    }

    @Override
    public Set<String> getFilterNamesForFixing() {
        throw new RuntimeException("Please configure CoRT");
    }

    @Override
    public List<TicketInfo> getTicketsForFilter(String filterName) {
        return Collections.emptyList();
    }

    @Override
    public void saveReviewData(String ticketKey, String newData) {
        throw new RuntimeException("Please configure CoRT");
    }

    @Override
    public ITicketData loadTicket(String ticketKey) {
        throw new RuntimeException("Please configure CoRT");
    }

    @Override
    public void startReviewing(String ticketKey) {
        throw new RuntimeException("Please configure CoRT");
    }

    @Override
    public void startFixing(String ticketKey) {
        throw new RuntimeException("Please configure CoRT");
    }

    @Override
    public void changeStateToReadyForReview(String ticketKey) {
        throw new RuntimeException("Please configure CoRT");
    }

    @Override
    public List<EndTransition> getPossibleTransitionsForReviewEnd(String ticketKey) {
        throw new RuntimeException("Please configure CoRT");
    }

    @Override
    public void changeStateAtReviewEnd(String ticketKey, EndTransition transition) {
        throw new RuntimeException("Please configure CoRT");
    }

    @Override
    public TicketLinkSettings getLinkSettings() {
        throw new RuntimeException("Please configure CoRT");
    }

}
