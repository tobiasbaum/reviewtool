package de.setsoftware.reviewtool.model;

/**
 * In-memory implementation of {@link IReviewDataCache}.
 */
public class InMemoryReviewDataCache implements IReviewDataCache {

    private String data;

    @Override
    public void saveLocalReviewData(String data) {
        this.data = data;
    }

    @Override
    public String getLocalReviewData() {
        return this.data;
    }

    @Override
    public void clearLocalReviewData() {
        this.data = null;
    }

}
