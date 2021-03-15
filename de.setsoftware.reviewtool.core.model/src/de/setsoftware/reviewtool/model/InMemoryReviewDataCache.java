package de.setsoftware.reviewtool.model;

import java.util.HashMap;
import java.util.Map;

/**
 * In-memory implementation of {@link IReviewDataCache}.
 */
public class InMemoryReviewDataCache implements IReviewDataCache {

    private final Map<String, String> data = new HashMap<>();

    @Override
    public void saveLocalReviewData(String key, String data) {
        this.data.put(key, data);
    }

    @Override
    public String getLocalReviewData(String key) {
        return this.data.get(key);
    }

    @Override
    public void clearLocalReviewData(String key) {
        this.data.remove(key);
    }

}
