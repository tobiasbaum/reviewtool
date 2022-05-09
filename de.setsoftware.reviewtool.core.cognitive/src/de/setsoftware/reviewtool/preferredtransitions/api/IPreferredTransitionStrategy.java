package de.setsoftware.reviewtool.preferredtransitions.api;

import java.util.List;

import de.setsoftware.reviewtool.model.ITicketData;
import de.setsoftware.reviewtool.model.changestructure.ToursInReview;

/**
 * Interface for strategies to propose preferred review end transitions, based on the review content.
 */
public interface IPreferredTransitionStrategy {

    /**
     * Can return a list of preferred transitions to end the review.
     * When there are no preferences, the empty list shall be returned.
     * If several entries are returned, the first is the most preferred, etc.
     */
    public abstract List<String> determinePreferredTransitions(
            boolean forOkCase, ITicketData ticketData, ToursInReview toursInReview);

}
