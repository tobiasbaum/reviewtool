package de.setsoftware.reviewtool.changesources.core;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobFunction;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import de.setsoftware.reviewtool.base.Logger;
import de.setsoftware.reviewtool.base.Pair;
import de.setsoftware.reviewtool.model.api.IChangeSource;
import de.setsoftware.reviewtool.model.api.IChangeSourceUi;
import de.setsoftware.reviewtool.model.api.IMutableFileHistoryGraph;

/**
 * Default implementation of the {@link IScmRepositoryManager} interface.
 *
 * @param <ItemT> Type of a change item.
 * @param <CommitIdT> Type of a commit ID.
 * @param <CommitT> Type of a commit.
 * @param <RepoT> Type of the repository.
 */
final class DefaultScmRepositoryManager<
        ItemT extends IScmChangeItem,
        CommitIdT extends IScmCommitId<CommitIdT>,
        CommitT extends IScmCommit<ItemT, CommitIdT>,
        RepoT extends DefaultScmRepository<ItemT, CommitIdT, CommitT, RepoT>>
            implements IScmRepositoryManager<ItemT, CommitIdT, CommitT, RepoT> {

    /**
     * Scheduling rule preventing two jobs to run concurrently for the same cache.
     */
    private static class CacheJobMutexRule implements ISchedulingRule {

        private final File cache;

        CacheJobMutexRule(final File cache) {
            this.cache = cache;
        }

        @Override
        public boolean isConflicting(final ISchedulingRule rule) {
            if (rule instanceof CacheJobMutexRule) {
                final CacheJobMutexRule other = (CacheJobMutexRule) rule;
                return this.cache.equals(other.cache);
            }
            return false;
        }

        @Override
        public boolean contains(final ISchedulingRule rule) {
            return rule == this;
        }
    }

    private static final long REVISION_BLOCK_SIZE = 500L;

    private final IChangeSource changeSource;
    private final IScmRepositoryBridge<ItemT, CommitIdT, CommitT, RepoT> scmBridge;
    private final int minCount;
    private final Map<String, RepoT> repoPerId;

    /**
     * Constructor.
     *
     * @param changeSource The associated change source.
     * @param scmBridge The SCM bridge to use.
     * @param minCount maximum initial size of the log
     */
    DefaultScmRepositoryManager(
            final IChangeSource changeSource,
            final IScmRepositoryBridge<ItemT, CommitIdT, CommitT, RepoT> scmBridge,
            final int minCount) {
        this.changeSource = changeSource;
        this.scmBridge = scmBridge;
        this.minCount = minCount;
        this.repoPerId = new LinkedHashMap<>();
        ManagerOfScmManagers.getInstance().addRepositoryManager(changeSource.getId(), this);
    }

    @Override
    public IScmRepositoryBridge<ItemT, CommitIdT, CommitT, RepoT> getScmBridge() {
        return this.scmBridge;
    }

    @Override
    public synchronized Collection<RepoT> getRepositories() {
        return Collections.unmodifiableCollection(this.repoPerId.values());
    }

    @Override
    public synchronized RepoT getRepo(final String id) {
        RepoT c = this.repoPerId.get(id);
        if (c == null) {
            try {
                c = this.scmBridge.createRepository(this.changeSource, id);
                this.repoPerId.put(id, c);
                this.tryToReadCacheFromFile(c);
            } catch (final ScmException e) {
                Logger.error("Could not access repository " + id, e);
            }
        }
        return c;
    }

    @Override
    public Pair<Boolean, List<CommitT>> filterCommits(
            final RepoT repo,
            final IScmCommitHandler<ItemT, CommitIdT, CommitT, Boolean> filter,
            final IChangeSourceUi ui) throws ScmException {

        final List<CommitT> result = new ArrayList<>();
        final Pair<Boolean, List<CommitT>> entries = this.getCommits(repo, ui);
        for (final CommitT commit : entries.getSecond()) {
            if (ui.isCanceled()) {
                throw new OperationCanceledException();
            }
            if (filter.processCommit(commit)) {
                result.add(commit);
            }
        }
        return Pair.create(entries.getFirst(), result);
    }

    /**
     * Returns all commits from the repository. If there are commits in the remote repository that have not been
     * processed yet, they are loaded and processed.
     *
     * <p>Completely processed commits are stored to disk in the background even if not all commits could be loaded
     * due to cancellation via {@link IProgressMonitor#setCanceled(boolean)}.
     *
     * @param repo The repository.
     * @return A pair of a boolean value and a list of commits. The boolean flag indicates whether new commits
     *         have been processed.
     */
    private synchronized Pair<Boolean, List<CommitT>> getCommits(
            final RepoT repo,
            final IChangeSourceUi ui) throws ScmException {

        final boolean gotNewEntries = this.loadNewCommits(repo, ui);
        return Pair.create(gotNewEntries, repo.getCommits());
    }

    /**
     * Loads and processes all commits from the repository that have not been processed yet.
     *
     * <p>Completely processed commits are stored to disk in the background even if not all commits could be loaded
     * due to cancellation via {@link IProgressMonitor#setCanceled(boolean)}.
     *
     * @param repo The repository.
     * @return {@code true} iff new commits have been processed.
     */
    private boolean loadNewCommits(
            final RepoT repo,
            final IChangeSourceUi ui) throws ScmException {

        final List<CommitT> commits = repo.getCommits();
        final CommitT lastKnownRevision = commits.isEmpty() ? null : commits.get(commits.size() - 1);

        final List<CommitT> newCommits = new ArrayList<>();
        final IScmCommitHandler<ItemT, CommitIdT, CommitT, Void> commitHandler =
                new IScmCommitHandler<ItemT, CommitIdT, CommitT, Void>() {
                    @Override
                    public Void processCommit(final CommitT commit) {
                        DefaultScmRepositoryManager.this.processCommit(
                                commit,
                                repo,
                                newCommits.size(),
                                ui);
                        newCommits.add(commit);
                        return null;
                    }
        };

        try {
            if (lastKnownRevision == null) {
                this.scmBridge.getInitialCommits(repo, this.minCount, commitHandler);
            } else {
                this.scmBridge.getNextCommits(repo, lastKnownRevision.getId(), commitHandler);
            }

            return !newCommits.isEmpty();
        } finally {
            repo.appendNewCommits(newCommits);

            if (!newCommits.isEmpty()) {
                final Job job = Job.create("Storing history data for " + repo,
                        new IJobFunction() {
                            @Override
                            public IStatus run(final IProgressMonitor monitor) {
                                DefaultScmRepositoryManager.this.tryToStoreCacheToFile(repo);
                                return Status.OK_STATUS;
                            }
                        });
                job.setRule(new CacheJobMutexRule(this.getCacheFilePath(repo.getId()).toFile()));
                job.schedule();
            }
        }
    }

    private void tryToReadCacheFromFile(final RepoT repo) {
        try {
            this.readCacheFromFile(repo);
        } catch (final ClassNotFoundException | IOException | ClassCastException e) {
            Logger.error("Problem while loading history data for " + repo, e);
        }
    }

    private synchronized void readCacheFromFile(final RepoT repo)
            throws IOException, ClassNotFoundException {

        final File cache = this.getCacheFilePath(repo.getId()).toFile();
        if (!cache.exists()) {
            Logger.info("History cache " + cache + " is missing for " + repo + ", nothing to load");
            return;
        }
        Logger.info("Loading history data for " + repo + " from " + cache);
        try (ObjectInputStream ois =
                new ObjectInputStream(new BufferedInputStream(new FileInputStream(cache)))) {

            @SuppressWarnings("unchecked")
            final List<CommitT> commits = (List<CommitT>) ois.readObject();
            final IMutableFileHistoryGraph historyGraph = (IMutableFileHistoryGraph) ois.readObject();

            repo.appendNewCommits(commits);
            repo.setFileHistoryGraph(historyGraph);
        }
        Logger.info("Loaded history data for " + repo + " from " + cache);
    }

    private void tryToStoreCacheToFile(final RepoT repo) {
        try {
            this.storeCacheToFile(repo);
        } catch (final IOException e) {
            Logger.error("Problem while storing history data for " + repo, e);
        }
    }

    private synchronized void storeCacheToFile(final RepoT repo)
            throws IOException {

        final File cache = this.getCacheFilePath(repo.getId()).toFile();
        Logger.info("Storing history data for " + repo + " to " + cache);
        try (ObjectOutputStream oos =
                new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(cache)))) {

            oos.writeObject(repo.getCommits());
            oos.writeObject(repo.getFileHistoryGraph());
        }
        Logger.info("Stored history data for " + repo + " to " + cache);
    }

    private void processCommit(
            final CommitT commit,
            final RepoT repo,
            final long numEntriesProcessed,
            final IChangeSourceUi ui) {

        if (ui.isCanceled()) {
            throw new OperationCanceledException();
        }

        ui.subTask("Processing commit " + commit.getId() + "...");
        commit.integrateInto(repo.getFileHistoryGraph());

        final long numEntriesProcessedNow = numEntriesProcessed + 1;
        if (numEntriesProcessedNow % REVISION_BLOCK_SIZE == 0) {
            Logger.debug(numEntriesProcessedNow + " commits processed");
        }
    }

    private IPath getCacheFilePath(final String repoId) {
        final Bundle bundle = FrameworkUtil.getBundle(this.getClass());
        final IPath dir = Platform.getStateLocation(bundle);
        return dir.append("history-" + encodeString(repoId) + ".cache");
    }

    private static String encodeString(final String s) {
        return Base64.getUrlEncoder().encodeToString(s.getBytes());
    }
}
