package de.setsoftware.reviewtool.changesources.svn;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobFunction;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.tmatesoft.svn.core.ISVNLogEntryHandler;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.SVNClientManager;

import de.setsoftware.reviewtool.base.Logger;
import de.setsoftware.reviewtool.model.api.IChangeSourceUi;

/**
 * Manages all known remote repositories.
 */
final class SvnRepositoryManager {

    /**
     * Scheduling rule preventing two jobs to run concurrently for the same cache.
     */
    private static class CacheJobMutexRule implements ISchedulingRule {

        private final File cache;

        CacheJobMutexRule(final File cache) {
            this.cache = cache;
        }

        @Override
        public boolean isConflicting(ISchedulingRule rule) {
            if (rule instanceof CacheJobMutexRule) {
                final CacheJobMutexRule other = (CacheJobMutexRule) rule;
                return this.cache.equals(other.cache);
            }
            return false;
        }

        @Override
        public boolean contains(ISchedulingRule rule) {
            return rule == this;
        }
    }

    private static final SvnRepositoryManager INSTANCE = new SvnRepositoryManager();
    private static final long REVISION_BLOCK_SIZE = 500L;

    private final Map<SVNURL, SvnRepo> repoPerRemoteUrl;
    private SVNClientManager mgr;
    private int minCount;

    /**
     * Constructor.
     */
    private SvnRepositoryManager() {
        this.repoPerRemoteUrl = new LinkedHashMap<>();
        this.minCount = 1000;
    }

    /**
     * Returns the singleton instance of this class.
     */
    static SvnRepositoryManager getInstance() {
        return INSTANCE;
    }

    /**
     * Initializes the cache.
     * @param mgr The {@link SVNClientManager} for retrieving information about working copies.
     * @param minCount maximum initial size of the log
     */
    void init(final SVNClientManager mgr, final int minCount) {
        this.mgr = mgr;
        this.minCount = minCount;
    }

    /**
     * Returns a read-only view of all known Subversion repositories.
     */
    synchronized Collection<SvnRepo> getRepositories() {
        return Collections.unmodifiableCollection(this.repoPerRemoteUrl.values());
    }

    /**
     * Returns a {@link SvnRepo} for a remote repository URL.
     * @param remoteUrl The URL of the remote repository.
     * @return A {@link SvnRepo} that always points at the root of the remote repository.
     */
    synchronized SvnRepo getRepo(final SVNURL remoteUrl) {
        try {
            SvnRepo c = this.repoPerRemoteUrl.get(remoteUrl);
            if (c == null) {
                final SVNRepository svnRepo = this.mgr.createRepository(remoteUrl, false);
                final SVNURL repositoryRoot = svnRepo.getRepositoryRoot(true);
                if (!repositoryRoot.equals(remoteUrl)) {
                    return this.getRepo(repositoryRoot);
                }

                c = new SvnRepo(svnRepo, remoteUrl);
                this.repoPerRemoteUrl.put(remoteUrl, c);
                SvnRepositoryManager.this.tryToReadCacheFromFile(c);
            }
            return c;
        } catch (final SVNException e) {
            return null;
        }
    }

    /**
     * Calls the given handler for all recent log entries of the given {@link SvnRepo}.
     */
    List<SvnRepoRevision> traverseRecentEntries(
            final SvnRepo repo,
            final CachedLogLookupHandler handler,
            final IChangeSourceUi ui) throws SVNException {

        final List<SvnRepoRevision> result = new ArrayList<>();
        for (final CachedLogEntry entry : this.getEntries(repo)) {
            if (ui.isCanceled()) {
                throw new OperationCanceledException();
            }
            if (handler.handleLogEntry(entry)) {
                result.add(new SvnRepoRevision(repo, entry));
            }
        }
        return result;
    }

    private synchronized List<CachedLogEntry> getEntries(final SvnRepo repo) throws SVNException {

        final boolean gotNewEntries = this.loadNewEntries(repo);

        if (gotNewEntries) {
            final Job job = Job.create("Storing SVN review cache for " + repo,
                    new IJobFunction() {
                        @Override
                        public IStatus run(IProgressMonitor monitor) {
                            SvnRepositoryManager.this.tryToStoreCacheToFile(repo);
                            return Status.OK_STATUS;
                        }
                    });
            job.setRule(new CacheJobMutexRule(repo.getCacheFilePath().toFile()));
            job.schedule();
        }

        return repo.getEntries();
    }

    private synchronized boolean loadNewEntries(final SvnRepo repo) throws SVNException {

        final List<CachedLogEntry> entries = repo.getEntries();
        final long lastKnownRevision = entries.isEmpty() ? 0 : entries.get(entries.size() - 1).getRevision();
        final List<CachedLogEntry> newEntries = new ArrayList<>();

        final long latestRevision = repo.getLatestRevision();
        if (lastKnownRevision < latestRevision) {
            final long startRevision = lastKnownRevision == 0
                    ? Math.max(0, latestRevision - this.minCount + 1) : lastKnownRevision + 1;

            Logger.info("Processing revisions " + startRevision + ".." + latestRevision + " from " + repo);

            final ISVNLogEntryHandler handler = new ISVNLogEntryHandler() {
                @Override
                public void handleLogEntry(SVNLogEntry logEntry) throws SVNException {
                    final CachedLogEntry entry = new CachedLogEntry(logEntry);
                    SvnRepositoryManager.this.processLogEntry(entry, repo, newEntries.size());
                    newEntries.add(entry);
                }
            };
            repo.getLog(startRevision, handler);
        }

        repo.appendNewEntries(newEntries);
        return !newEntries.isEmpty();
    }

    private void tryToReadCacheFromFile(final SvnRepo repo) {
        try {
            this.readCacheFromFile(repo);
        } catch (final ClassNotFoundException | IOException | ClassCastException e) {
            Logger.error("Problem while loading SVN history data for " + repo, e);
        }
    }

    private synchronized void readCacheFromFile(final SvnRepo repo)
            throws IOException, ClassNotFoundException {

        final File cache = repo.getCacheFilePath().toFile();
        if (!cache.exists()) {
            Logger.info("SVN cache " + cache + " is missing for " + repo + ", nothing to load");
            return;
        }
        Logger.info("Loading SVN history data for " + repo + " from " + cache);
        try (ObjectInputStream ois =
                new ObjectInputStream(new BufferedInputStream(new FileInputStream(cache)))) {

            @SuppressWarnings("unchecked") final List<CachedLogEntry> value =
                    (List<CachedLogEntry>) ois.readObject();
            final SvnFileHistoryGraph historyGraph = (SvnFileHistoryGraph) ois.readObject();

            repo.appendNewEntries(value);
            repo.setFileHistoryGraph(historyGraph);
        }
        Logger.info("Loaded SVN history data for " + repo + " from " + cache);
    }

    private void tryToStoreCacheToFile(final SvnRepo repo) {
        try {
            this.storeCacheToFile(repo);
        } catch (final IOException e) {
            Logger.error("Problem while storing SVN history data for " + repo, e);
        }
    }

    private synchronized void storeCacheToFile(final SvnRepo repo) throws IOException {
        final File cache = repo.getCacheFilePath().toFile();
        Logger.info("Storing SVN history data for " + repo + " to " + cache);
        try (ObjectOutputStream oos =
                new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(cache)))) {

            oos.writeObject(repo.getEntries());
            oos.writeObject(repo.getFileHistoryGraph());
        }
        Logger.info("Stored SVN history data for " + repo + " to " + cache);
    }

    private void processLogEntry(
            final CachedLogEntry entry,
            final SvnRepo repo,
            final int numEntriesProcessed) {

        final SvnRepoRevision revision = new SvnRepoRevision(repo, entry);
        repo.getFileHistoryGraph().processRevision(revision);
        final int numEntriesProcessedNow = numEntriesProcessed + 1;
        if (numEntriesProcessedNow % REVISION_BLOCK_SIZE == 0) {
            Logger.debug(numEntriesProcessedNow + " revisions processed");
        }
    }
}