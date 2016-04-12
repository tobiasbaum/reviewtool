package de.setsoftware.reviewtool.model;

import java.util.List;

import de.setsoftware.reviewtool.base.WeakListeners;

/**
 * Manages the current ticket under review and provides a facade to the persistence layer.
 */
public class ReviewStateManager {

    private IReviewPersistence persistence;
    private final IUserInteraction userInteraction;

    private String ticketKey;

    private final WeakListeners<IReviewDataSaveListener> saveListeners = new WeakListeners<>();

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

    /**
     * Save the given review data in the current ticket.
     * Notifies all listeners of the save.
     */
    public void saveCurrentReviewData(String newData) {
        this.loadTicketDataAndCheckExistence(true);
        this.persistence.saveReviewData(this.ticketKey, newData);
        for (final IReviewDataSaveListener l : this.saveListeners) {
            l.onSave(newData);
        }
    }

    public void addSaveListener(IReviewDataSaveListener l) {
        this.saveListeners.add(l);
    }

    /**
     * Returns the current review round number, including running reviews (i.e. returns 1 during the first review).
     */
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

    public List<EndTransition> getPossibleTransitionsForReviewEnd() {
        return this.persistence.getPossibleTransitionsForReviewEnd(this.ticketKey);
    }

    public void changeStateAtReviewEnd(EndTransition transition) {
        this.persistence.changeStateAtReviewEnd(this.ticketKey, transition);
    }

    public IUserInteraction getUi() {
        return this.userInteraction;
    }

}
