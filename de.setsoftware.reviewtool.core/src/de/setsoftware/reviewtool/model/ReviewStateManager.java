package de.setsoftware.reviewtool.model;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import de.setsoftware.reviewtool.base.WeakListeners;
import de.setsoftware.reviewtool.model.remarks.DummyMarker;
import de.setsoftware.reviewtool.model.remarks.ReviewData;
import de.setsoftware.reviewtool.model.remarks.ReviewRemark;
import de.setsoftware.reviewtool.model.remarks.ReviewRemarkException;

/**
 * Manages the current ticket under review and provides a facade to the persistence layer.
 */
public class ReviewStateManager {

    private final IReviewDataCache localReviewData;
    private IReviewPersistence persistence;
    private final IUserInteraction userInteraction;

    private String ticketKey;

    private final WeakListeners<IReviewDataSaveListener> saveListeners = new WeakListeners<>();

    public ReviewStateManager(
            IReviewDataCache localReviewData,
            IReviewPersistence persistence,
            IUserInteraction userInteraction) {
        this.localReviewData = localReviewData;
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
     * Save the given review data in the local cache.
     * Notifies all listeners of the save.
     */
    public void saveCurrentReviewData(String newData) {
        this.loadTicketDataAndCheckExistence(true);
        this.localReviewData.saveLocalReviewData(this.ticketKey, newData);
        for (final IReviewDataSaveListener l : this.saveListeners) {
            l.onSave(newData);
        }
    }

    /**
     * Writes the current review data from the local cache to the ticket
     * and clear the local cache.
     */
    public void flushReviewData() {
        final String cachedData = this.localReviewData.getLocalReviewData(this.ticketKey);
        if (cachedData != null) {
            this.persistence.saveReviewData(this.ticketKey, cachedData);
        }
        this.localReviewData.clearLocalReviewData(this.ticketKey);
    }

    /**
     * Clears the locally cached review data, so that the next load will go through to the persistence layer.
     */
    public void clearLocalReviewData() {
        this.localReviewData.clearLocalReviewData(this.ticketKey);
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

    public String getReviewerForCurrentRound() {
        return this.getReviewerForRound(this.getCurrentRound());
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
            }
            while (data == null);
            return this.decorateIfNeeded(data);
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
            return this.decorateIfNeeded(data);
        }
    }

    private ITicketData decorateIfNeeded(ITicketData data) {
        final String localData = this.localReviewData.getLocalReviewData(data.getTicketInfo().getId());
        return localData != null ? new LocalReviewDataDecorator(localData, data) : data;
    }

    /**
     * Asks the user for a ticket. Does not change the review mode.
     * @param forReview True iff selection should be for review, false iff it should be for fixing.
     */
    public boolean selectTicket(boolean forReview) {
        return this.loadTicketDataAndCheckExistence(forReview) != null;
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

    public void startReviewing() {
        this.persistence.startReviewing(this.ticketKey);
    }

    public void startFixing() {
        this.persistence.startFixing(this.ticketKey);
    }

    public TicketLinkSettings getTicketLinkSettings() {
        return this.persistence.getLinkSettings();
    }

    /**
     * Deletes the given review remark. Persists the changed review data and deletes the marker as well.
     */
    public void deleteRemark(ReviewRemark remark) throws ReviewRemarkException {
        final ReviewData data = this.getUi().getSyntaxFixer().getCurrentReviewDataParsed(this, DummyMarker.FACTORY);
        data.deleteRemark(remark);
        this.saveCurrentReviewData(data.serialize());
        remark.deleteMarker();
    }

    /**
     * Merges the given remark into the existing remarks and saves it to the persistence.
     */
    public void saveRemark(ReviewRemark remark) {
        final ReviewData r = this.getUi().getSyntaxFixer().getCurrentReviewDataParsed(
                this, DummyMarker.FACTORY);
        r.merge(remark, this.getCurrentRound());
        this.saveCurrentReviewData(r.serialize());
    }

    /**
     * Returns the reviewers for all review rounds.
     */
    public Map<Integer, String> getReviewersForRounds() {
        final Map<Integer, String> ret = new TreeMap<>();
        for (int round = 1; round <= this.getCurrentRound(); round++) {
            ret.put(round, this.getReviewerForRound(round));
        }
        return ret;
    }

    /**
     * Returns the tickets that are ready for review or fixing based on the given filter.
     * When the filter name is invalid, falls back to the first filter for the given mode.
     */
    public List<TicketInfo> getTicketsForFilter(String filter, boolean review) {
        final Set<String> allowedNames =
                review ? this.persistence.getFilterNamesForReview() : this.persistence.getFilterNamesForFixing();
        return this.persistence.getTicketsForFilter(
                allowedNames.contains(filter) ? filter : allowedNames.iterator().next());
    }

}
