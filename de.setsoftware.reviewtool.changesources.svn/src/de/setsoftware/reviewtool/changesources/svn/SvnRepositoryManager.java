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

import org.tmatesoft.svn.core.ISVNLogEntryHandler;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.SVNClientManager;

import de.setsoftware.reviewtool.base.Logger;
import de.setsoftware.reviewtool.base.Pair;
import de.setsoftware.reviewtool.model.api.BackgroundJobExecutor;
import de.setsoftware.reviewtool.model.api.IChangeSourceUi;
import de.setsoftware.reviewtool.model.api.ICortProgressMonitor;
import de.setsoftware.reviewtool.model.api.IMutableFileHistoryGraph;

/**
 * Manages all known remote repositories.
 */
final class SvnRepositoryManager {

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
     * @return A {@link SvnRepo} that points at the passed URL.
     */
    synchronized SvnRepo getRepo(final SVNURL remoteUrl) {
        SvnRepo c = this.repoPerRemoteUrl.get(remoteUrl);
        if (c == null) {
            try {
                final SVNRepository svnRepo = this.mgr.createRepository(remoteUrl, false);
                c = new SvnRepo(svnRepo, remoteUrl);
            } catch (final SVNException e) {
                Logger.error("Could not access repository " + remoteUrl, e);
                return null;
            }

            this.repoPerRemoteUrl.put(remoteUrl, c);
            SvnRepositoryManager.this.tryToReadCacheFromFile(c);
        }
        return c;
    }

    /**
     * Returns a reusable {@link SVNRepository} referring to a path in a remote repository.
     * @param remoteRootUrl The root URL of the remote repository.
     * @param path The path at the remote repository.
     * @return A {@link SVNRepository} that refers to the specified path in the remote repository.
     * @throws SVNException if an error occurs.
     */
    synchronized SVNRepository getTemporaryRepo(final SVNURL remoteRootUrl, final String path) throws SVNException {
        return this.mgr.createRepository(remoteRootUrl.appendPath(path, false), true);
    }

    /**
     * Calls the given handler for all recent log entries of the given {@link SvnRepo}.
     * If there are revisions in the remote repository that have not been processed yet, they are loaded and processed
     * before being filtered.
     *
     * <p>Completely processed log entries are stored to disk in the background even if not all log entries could be
     * loaded due to cancellation via {@link IProgressMonitor#setCanceled(boolean)}.
     *
     * @param repo The repository.
     * @param handler The log entry handler to use for revision filtering.
     * @return A pair of a boolean value and a list of repository revisions. The boolean flag indicates whether new
     *         history entries have been processed.
     */
    Pair<Boolean, List<SvnRepoRevision>> traverseRecentEntries(
            final ISvnRepo repo,
            final CachedLogLookupHandler handler,
            final IChangeSourceUi ui) throws SVNException {

        final List<SvnRepoRevision> result = new ArrayList<>();
        final Pair<Boolean, List<CachedLogEntry>> entries = this.getEntries(repo, ui);
        for (final CachedLogEntry entry : entries.getSecond()) {
            if (ui.isCanceled()) {
                throw BackgroundJobExecutor.createOperationCanceledException();
            }
            if (handler.handleLogEntry(entry)) {
                result.add(new SvnRepoRevision(repo, entry));
            }
        }
        return Pair.create(entries.getFirst(), result);
    }

    /**
     * Returns all log entries from the repository. If there are revisions in the remote repository that have not been
     * processed yet, they are loaded and processed.
     *
     * <p>Completely processed log entries are stored to disk in the background even if not all log entries could be
     * loaded due to cancellation via {@link IProgressMonitor#setCanceled(boolean)}.
     *
     * @param repo The repository.
     * @return A pair of a boolean value and a list of log entries. The boolean flag indicates whether new history
     *         entries have been processed.
     */
    private synchronized Pair<Boolean, List<CachedLogEntry>> getEntries(final ISvnRepo repo, final IChangeSourceUi ui)
            throws SVNException {

        final boolean gotNewEntries = this.loadNewEntries(repo, ui);
        return Pair.create(gotNewEntries, repo.getEntries());
    }

    /**
     * Loads and processes all log entries from the repository that have not been processed yet.
     *
     * <p>Completely processed log entries are stored to disk in the background even if not all log entries could be
     * loaded due to cancellation via {@link IProgressMonitor#setCanceled(boolean)}.
     *
     * @param repo The repository.
     * @return {@code true} iff new history entries have been processed.
     */
    private boolean loadNewEntries(final ISvnRepo repo, final IChangeSourceUi ui) throws SVNException {
        final List<CachedLogEntry> entries = repo.getEntries();
        final long lastKnownRevision = entries.isEmpty() ? 0 : entries.get(entries.size() - 1).getRevision();

        final long latestRevision = repo.getLatestRevision();
        if (lastKnownRevision < latestRevision) {
            final long startRevision = lastKnownRevision == 0
                    ? Math.max(0, latestRevision - this.minCount + 1) : lastKnownRevision + 1;
            this.loadNewEntries(repo, startRevision, latestRevision, ui);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Loads and processes a range of log entries from the repository that have not been processed yet.
     *
     * <p>Completely processed log entries are stored to disk in the background even if not all log entries could be
     * loaded due to cancellation via {@link IProgressMonitor#setCanceled(boolean)}.
     *
     * @param repo The repository.
     * @param firstRevision The first revision to process.
     * @param lastRevision The last revision to process.
     */
    private void loadNewEntries(
            final ISvnRepo repo,
            final long firstRevision,
            final long lastRevision,
            final IChangeSourceUi ui) throws SVNException {

        if (lastRevision < firstRevision) {
            return;
        }

        final List<CachedLogEntry> newEntries = new ArrayList<>();
        final long numRevisionsTotal = lastRevision - firstRevision + 1;

        Logger.info("Processing revisions " + firstRevision + ".." + lastRevision + " from " + repo);
        ui.increaseTaskNestingLevel();
        try {
            final ISVNLogEntryHandler handler = new ISVNLogEntryHandler() {
                @Override
                public void handleLogEntry(final SVNLogEntry logEntry) throws SVNException {
                    final CachedLogEntry entry = new CachedLogEntry(logEntry);
                    SvnRepositoryManager.this.processLogEntry(
                            entry,
                            repo,
                            newEntries.size(),
                            numRevisionsTotal,
                            ui);
                    newEntries.add(entry);
                }
            };
            repo.getLog(firstRevision, handler);
        } finally {
            ui.decreaseTaskNestingLevel();
            repo.appendNewEntries(newEntries);

            if (!newEntries.isEmpty()) {
                BackgroundJobExecutor.executeWithMutex(
                        "Storing SVN review cache for " + repo,
                        repo.getCacheFilePath().toFile(),
                        (ICortProgressMonitor monitor) -> tryToStoreCacheToFile(repo));
            }
        }
    }

    private void tryToReadCacheFromFile(final ISvnRepo repo) {
        try {
            this.readCacheFromFile(repo);
        } catch (final ClassNotFoundException | IOException | ClassCastException e) {
            Logger.error("Problem while loading SVN history data for " + repo, e);
        }
    }

    private synchronized void readCacheFromFile(final ISvnRepo repo)
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
            final IMutableFileHistoryGraph historyGraph = (IMutableFileHistoryGraph) ois.readObject();

            repo.appendNewEntries(value);
            repo.setFileHistoryGraph(historyGraph);
        }
        Logger.info("Loaded SVN history data for " + repo + " from " + cache);
    }

    private void tryToStoreCacheToFile(final ISvnRepo repo) {
        try {
            this.storeCacheToFile(repo);
        } catch (final IOException e) {
            Logger.error("Problem while storing SVN history data for " + repo, e);
        }
    }

    private synchronized void storeCacheToFile(final ISvnRepo repo) throws IOException {
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
            final ISvnRepo repo,
            final long numEntriesProcessed,
            final long numRevisionsTotal,
            final IChangeSourceUi ui) {

        if (ui.isCanceled()) {
            throw BackgroundJobExecutor.createOperationCanceledException();
        }

        final SvnRepoRevision revision = new SvnRepoRevision(repo, entry);
        final long numEntriesProcessedNow = numEntriesProcessed + 1;
        ui.subTask("Processing revision " + revision.getRevisionNumber()
                + " (" + numEntriesProcessedNow + "/" + numRevisionsTotal + ")...");
        revision.integrateInto(repo.getFileHistoryGraph());

        if (numEntriesProcessedNow % REVISION_BLOCK_SIZE == 0) {
            Logger.debug(numEntriesProcessedNow + " revisions processed");
        }
    }
}
