package de.setsoftware.reviewtool.changesources.git;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.StatusCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

import de.setsoftware.reviewtool.diffalgorithms.DiffAlgorithmFactory;
import de.setsoftware.reviewtool.model.api.IChangeSourceUi;
import de.setsoftware.reviewtool.model.api.IMutableFileHistoryGraph;
import de.setsoftware.reviewtool.model.api.IRevision;
import de.setsoftware.reviewtool.model.changestructure.ChangestructureFactory;
import de.setsoftware.reviewtool.model.changestructure.FileHistoryGraph;

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
    synchronized GitWorkingCopy getWorkingCopy(final File workingCopyRoot, File cacheDir) {
        GitWorkingCopy wc = this.wcPerRootDirectory.get(workingCopyRoot.toString());
        if (wc == null) {
            wc = new GitWorkingCopy(workingCopyRoot, cacheDir);
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

    Map<GitRevision, String> traverseEntries(Predicate<GitRevision> handler, IChangeSourceUi ui)
        throws GitAPIException, IOException {

        final Map<GitRevision, String> ret = new LinkedHashMap<>();
        for (final GitWorkingCopy wc : this.getWorkingCopies()) {
            final Repository repository = wc.getRepository().getRepository();

            final List<Ref> allRefs = new ArrayList<>(repository.getRefDatabase().getRefs());
            Collections.sort(allRefs, (Ref r1, Ref r2) -> {
                //HEAD is always first, otherwise make deterministic by sorting
                final String n1 = r1.getName();
                final String n2 = r2.getName();
                if (n1.equals("HEAD")) {
                    return n2.equals("HEAD") ? 0 : -1;
                } else if (n2.equals("HEAD")) {
                    return 1;
                } else {
                    return n1.compareTo(n2);
                }
            });

            final Set<ObjectId> visited = new HashSet<>();
            for (final Ref ref : allRefs) {
                try (RevWalk revWalk = new RevWalk(repository)) {
                    if (visited.contains(ref.getObjectId())) {
                        break;
                    }
                    revWalk.markStart(revWalk.parseCommit(ref.getObjectId()));
                    for (final RevCommit commit : revWalk) {
                        if (visited.contains(commit.getId())) {
                            break;
                        }
                        visited.add(commit.getId());
                        final GitRevision r = new GitRevision(wc, commit);
                        if (handler.test(r)) {
                            ret.put(r, ref.getName());
                        }
                    }
                }
            }
        }
        return ret;
    }

    /**
     * Collects all local changes and integrates them into the {@link FileHistoryGraph}.
     * @param relevantPaths The list of additional paths to check. If {@code null}, the whole working copy is analyzed.
     */
    void collectWorkingCopyChanges(final List<File> relevantPaths) throws IOException, GitAPIException {
        for (final GitWorkingCopy wc : this.getWorkingCopies()) {
            this.collectWorkingCopyChanges(wc, relevantPaths);
        }
    }

    /**
     * Collects all local changes of a given working copy and integrates them into the {@link FileHistoryGraph}.
     * @param relevantPaths The list of additional paths to check. If {@code null}, the whole working copy is analyzed.
     */
    private void collectWorkingCopyChanges(final GitWorkingCopy wc, final List<? extends File> relevantPaths)
        throws IOException, GitAPIException {

        final Repository repo = wc.getRepository().getRepository();
        final ObjectId head = repo.resolve(Constants.HEAD);
        if (head == null) {
            return;
        }

        try (final Git git = new Git(repo)) {
            final StatusCommand status = git.status();
            if (relevantPaths != null) {
                for (final File p : relevantPaths) {
                    status.addPath(p.toString());
                }
            }

            final Status statusResult = status.call();
            final IRevision wcRevision = ChangestructureFactory.createLocalRevision(wc);
            RevCommit headCommit;
            try (RevWalk revWalk = new RevWalk(repo)) {
                headCommit = revWalk.parseCommit(head);
            }
            final IRevision headRevision = ChangestructureFactory.createRepoRevision(
                    new RevisionId(headCommit), wc.getRepository());
            final IMutableFileHistoryGraph localFileHistoryGraph =
                    new FileHistoryGraph(DiffAlgorithmFactory.createDefault());
            for (final String modifiedFile : statusResult.getModified()) {
                localFileHistoryGraph.addChange(modifiedFile, wcRevision, Collections.singleton(headRevision));
            }
            wc.setLocalFileHistoryGraph(localFileHistoryGraph);
        }
    }
}
