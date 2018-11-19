package de.setsoftware.reviewtool.changesources.git;

/**
 * Manages all known remote repositories.
 */
final class GitRepositoryManager {
    
    private static final GitRepositoryManager INSTANCE = new GitRepositoryManager();
    
    private int logCacheMinSize;
    
    /**
     * Constructor.
     */
    GitRepositoryManager() {
        this.logCacheMinSize = 1000;
    }
    
    /**
     * Returns the singleton instance of this class.
     */
    static GitRepositoryManager getInstance() {
        return INSTANCE;
    }

    /**
     * Initializes the cache.
     * 
     * @param logCacheMinSize maximum initial size of the log cache
     */
    void init(final int logCacheMinSize) {
        this.logCacheMinSize = logCacheMinSize;
    }

}
