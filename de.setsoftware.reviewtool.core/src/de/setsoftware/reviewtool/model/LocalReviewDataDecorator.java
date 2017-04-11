package de.setsoftware.reviewtool.model;

/**
 * Decorates {@link ITicketData} and provides other review data.
 */
class LocalReviewDataDecorator implements ITicketData {

    private final String reviewData;
    private final ITicketData decorated;

    public LocalReviewDataDecorator(String reviewData, ITicketData decorated) {
        this.reviewData = reviewData;
        this.decorated = decorated;
    }

    @Override
    public String getReviewData() {
        return this.reviewData;
    }

    @Override
    public String getReviewerForRound(int number) {
        return this.decorated.getReviewerForRound(number);
    }

    @Override
    public int getCurrentRound() {
        return this.decorated.getCurrentRound();
    }

    @Override
    public TicketInfo getTicketInfo() {
        return this.decorated.getTicketInfo();
    }

    @Override
    public String getId() {
        return this.decorated.getId();
    }

}
