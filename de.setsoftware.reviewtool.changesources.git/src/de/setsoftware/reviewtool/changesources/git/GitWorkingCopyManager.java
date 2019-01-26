package de.setsoftware.reviewtool.changesources.git;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import de.setsoftware.reviewtool.model.api.IChangeSourceUi;

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
     * Resets the singleton instance (for tests).
     */
    static void reset() {
        synchronized (INSTANCE) {
            INSTANCE.wcPerRootDirectory.clear();
        }
    }

    /**
     * Returns a copied view of all known Git working copies.
     */
    synchronized Collection<GitWorkingCopy> getWorkingCopies() {
        return new ArrayList<>(this.wcPerRootDirectory.values());
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

    List<GitRevision> traverseEntries(Predicate<GitRevision> handler, IChangeSourceUi ui)
        throws GitAPIException, IOException {

        final List<GitRevision> ret = new ArrayList<>();
        for (final GitWorkingCopy wc : this.getWorkingCopies()) {
            final Repository repository = wc.getRepository().getRepository();

            try (Git git = new Git(repository)) {
                final Iterable<RevCommit> commits = git.log().all().call();
                for (final RevCommit commit : commits) {
                    final GitRevision r = new GitRevision(wc, commit);
                    if (handler.test(r)) {
                        ret.add(r);
                    }
                }
            }
        }
        return ret;
    }
}
