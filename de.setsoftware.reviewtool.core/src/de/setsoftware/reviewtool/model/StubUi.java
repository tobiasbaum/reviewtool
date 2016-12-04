package de.setsoftware.reviewtool.model;

import de.setsoftware.reviewtool.model.remarks.IMarkerFactory;
import de.setsoftware.reviewtool.model.remarks.ReviewData;

/**
 * A stub implementation for the UI interfaces, to be used when a real UI is not wanted.
 */
public final class StubUi implements IUserInteraction, ITicketChooser, ISyntaxFixer {

    private final String ticketId;

    public StubUi(String chosenTicketId) {
        this.ticketId = chosenTicketId;
    }

    @Override
    public ITicketChooser getTicketChooser() {
        return this;
    }

    @Override
    public String choose(IReviewPersistence persistence, String ticketKeyDefault, boolean forReview) {
        return this.ticketId;
    }

    @Override
    public ISyntaxFixer getSyntaxFixer() {
        return this;
    }

    @Override
    public ReviewData getCurrentReviewDataParsed(ReviewStateManager persistence, IMarkerFactory factory) {
        return ReviewData.parse(persistence.getReviewersForRounds(), factory, persistence.getCurrentReviewData());
    }

}