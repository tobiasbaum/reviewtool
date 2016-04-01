package de.setsoftware.reviewtool.model;

public class ReviewStateManager {

    private IReviewPersistence persistence;
    private final IUserInteraction userInteraction;

    private String ticketKey;

    public ReviewStateManager(IReviewPersistence persistence, IUserInteraction userInteraction) {
        this.persistence = persistence;
        this.userInteraction = userInteraction;
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

    public ITicketData getCurrentTicketData() {
        return this.loadTicketDataAndCheckExistence(true);
    }

    private ITicketData loadTicketDataAndCheckExistence(boolean forReview) {
        if (this.ticketKey == null) {
            ITicketData data;
            do {
                this.ticketKey = this.userInteraction.getTicketChooser().choose(this.persistence, "", forReview);
                if (this.ticketKey == null) {
                    return null;
                }
                data = this.persistence.loadTicket(this.ticketKey);
            } while (data == null);
            return data;
        } else {
            ITicketData data = this.persistence.loadTicket(this.ticketKey);
            while (data == null) {
                this.ticketKey = this.userInteraction.getTicketChooser().choose(
                        this.persistence, this.ticketKey, forReview);
                if (this.ticketKey == null) {
                    return null;
                }
                data = this.persistence.loadTicket(this.ticketKey);
            }
            return data;
        }
    }

    /**
     * Asks the user for a ticket and changes the review mode accordingly.
     * @param forReview True iff selection should be for review, false iff it should be for fixing.
     */
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

    /**
     * Returns the currently selection ticket key, or null if none is selected.
     */
    public String getTicketKey() {
        return this.ticketKey;
    }

    public void setPersistence(IReviewPersistence newPersistence) {
        this.persistence = newPersistence;
    }

    public void changeStateToReadyForReview() {
        this.persistence.changeStateToReadyForReview(this.ticketKey);
    }

    public void changeStateToDone() {
        this.persistence.changeStateToDone(this.ticketKey);
    }

    public void changeStateToRejected() {
        this.persistence.changeStateToRejected(this.ticketKey);
    }

    public IUserInteraction getUi() {
        return this.userInteraction;
    }

}
