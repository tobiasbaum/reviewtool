package de.setsoftware.reviewtool.changesources.git;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import de.setsoftware.reviewtool.changesources.git.GitWorkingCopy;

/**
 * Manages all known local working copies.
 */
final class GitWorkingCopyManager {

    private static final GitWorkingCopyManager INSTANCE = new GitWorkingCopyManager();
    
    private final Map<String, GitWorkingCopy> wcPerRootDirectory;

    /**
     * Constructor.
     */
    private GitWorkingCopyManager() {
        this.wcPerRootDirectory = new LinkedHashMap<>();
    }
    
    /**
     * Returns the singleton instance of this class.
     */
    static GitWorkingCopyManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * Returns a read-only view of all known Git working copies.
     */
    synchronized Collection<GitWorkingCopy> getWorkingCopies() {
        return Collections.unmodifiableCollection(this.wcPerRootDirectory.values());
    }

    /**
     * Returns a repository by its working copy root.
     * @param workingCopyRoot The root directory of the working copy.
     * @return A {@link GitWorkingCopy} or {@code null} if not found.
     */
    synchronized GitWorkingCopy getWorkingCopy(final File workingCopyRoot) {
        GitWorkingCopy wc = this.wcPerRootDirectory.get(workingCopyRoot.toString());
        if (wc == null) {
            wc = new GitWorkingCopy(workingCopyRoot);
            this.wcPerRootDirectory.put(workingCopyRoot.toString(), wc);
        }
        return wc;
    }
    
    /**
     * Removes a working copy.
     * @param workingCopyRoot The root directory of the working copy.
     */
    synchronized void removeWorkingCopy(final File workingCopyRoot) {
        this.wcPerRootDirectory.remove(workingCopyRoot.toString());
    }
}
