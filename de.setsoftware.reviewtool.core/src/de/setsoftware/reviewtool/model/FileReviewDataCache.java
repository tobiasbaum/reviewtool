package de.setsoftware.reviewtool.model;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import de.setsoftware.reviewtool.base.ReviewtoolException;

/**
 * Implementation of {@link IReviewDataCache} that caches to disk.
 */
public class FileReviewDataCache implements IReviewDataCache {

    private final File localReviewData;

    public FileReviewDataCache(File localStateDirectory) {
        this.localReviewData = new File(localStateDirectory, "localReviewData");
    }

    @Override
    public void saveLocalReviewData(String data) {
        try {
            Files.write(this.localReviewData.toPath(), data.getBytes("UTF-8"));
        } catch (final IOException e) {
            throw new ReviewtoolException(e);
        }
    }

    @Override
    public String getLocalReviewData() {
        try {
            if (this.localReviewData.exists()) {
                final byte[] bytes = Files.readAllBytes(this.localReviewData.toPath());
                return new String(bytes, "UTF-8");
            } else {
                return null;
            }
        } catch (final IOException e) {
            throw new ReviewtoolException(e);
        }
    }

    @Override
    public void clearLocalReviewData() {
        this.localReviewData.delete();
    }
}
