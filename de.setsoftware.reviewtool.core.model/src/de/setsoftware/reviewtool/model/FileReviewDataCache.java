package de.setsoftware.reviewtool.model;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import de.setsoftware.reviewtool.base.ReviewtoolException;

/**
 * Implementation of {@link IReviewDataCache} that caches to disk.
 */
public class FileReviewDataCache implements IReviewDataCache {

    private final File localStateDirectory;

    public FileReviewDataCache(File localStateDirectory) {
        this.localStateDirectory = localStateDirectory;
    }

    private File getFile(String key) {
        return new File(this.localStateDirectory, "localReviewData." + key);
    }

    @Override
    public void saveLocalReviewData(String key, String data) {
        try {
            Files.write(this.getFile(key).toPath(), data.getBytes("UTF-8"));
        } catch (final IOException e) {
            throw new ReviewtoolException(e);
        }
    }

    @Override
    public String getLocalReviewData(String key) {
        try {
            final File file = this.getFile(key);
            if (file.exists()) {
                final byte[] bytes = Files.readAllBytes(file.toPath());
                return new String(bytes, "UTF-8");
            } else {
                return null;
            }
        } catch (final IOException e) {
            throw new ReviewtoolException(e);
        }
    }

    @Override
    public void clearLocalReviewData(String key) {
        this.getFile(key).delete();
    }
}
