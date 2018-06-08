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
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
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
import org.tmatesoft.svn.core.ISVNLogEntryHandler;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNInfo;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNWCClient;

import de.setsoftware.reviewtool.base.Logger;
import de.setsoftware.reviewtool.base.ValueWrapper;
import de.setsoftware.reviewtool.model.api.IChangeSourceUi;

/**
 * A local cache of the SVN log(s) to speed up the gathering of relevant entries.
 */
final class CachedLog {

    /**
     * Data regarding the repository. Is only cached in memory.
     */
    private static final class RepoDataCache {

        private final SvnRepo repo;
        private final List<CachedLogEntry> entries;

        RepoDataCache(final SvnRepo repo) {
            this.repo = repo;
            this.entries = new ArrayList<>();
        }

        SvnRepo getRepo() {
            return this.repo;
        }

        List<CachedLogEntry> getEntries() {
            return this.entries;
        }

        IPath getCacheFilePath() {
            final Bundle bundle = FrameworkUtil.getBundle(this.getClass());
            final IPath dir = Platform.getStateLocation(bundle);
            return dir.append("svnlog-" + encodeString(this.repo.getRemoteUrl().toString()) + ".cache");
        }

        private static String encodeString(final String s) {
            return Base64.getUrlEncoder().encodeToString(s.getBytes());
        }
    }

    /**
     * Scheduling rule preventing two cache jobs to run concurrently.
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

    private static final CachedLog INSTANCE = new CachedLog();
    private static final long REVISION_BLOCK_SIZE = 500L;

    private final Map<String, RepoDataCache> repoDataPerWcRoot;
    private SVNClientManager mgr;
    private int minCount;
    private int maxCount;

    /**
     * Constructor.
     */
    private CachedLog() {
        this.repoDataPerWcRoot = new HashMap<>();
        this.minCount = 1000;
        this.maxCount = 1000;
    }

    /**
     * Returns the singleton instance of this class.
     */
    public static CachedLog getInstance() {
        return INSTANCE;
    }

    /**
     * Initializes the cache.
     * @param mgr The {@link SVNClientManager} for retrieving information about working copies.
     * @param minCount minimum size of the log
     * @param maxCount maximum size of the log
     */
    public void init(final SVNClientManager mgr, final int minCount, final int maxCount) {
        this.mgr = mgr;
        this.minCount = Math.min(minCount, maxCount);
        this.maxCount = Math.max(minCount, maxCount);
    }

    /**
     * Returns a collection of all known Subversion repositories.
     */
    Collection<SvnRepo> getRepositories() {
        final List<SvnRepo> result = new ArrayList<>();
        for (final RepoDataCache info : this.repoDataPerWcRoot.values()) {
            result.add(info.getRepo());
        }
        return result;
    }

    /**
     * Returns a repository by its working copy root.
     * @param workingCopyRoot The root directory of the working copy.
     * @return A {@link SvnRepo} or {@code null} if not found.
     */
    SvnRepo getRepositoryByWorkingCopyRoot(final File workingCopyRoot) {
        try {
            final RepoDataCache c = this.getRepoCache(workingCopyRoot);
            return c.getRepo();
        } catch (final SVNException e) {
            return null;
        }
    }

    /**
     * Calls the given handler for all recent log entries of the given working copy root.
     */
    void traverseRecentEntries(
            final File workingCopyRoot,
            final CachedLogLookupHandler handler,
            final IChangeSourceUi ui) throws SVNException {

        final RepoDataCache repoCache = this.getRepoCache(workingCopyRoot);
        handler.startNewRepo(repoCache.getRepo());
        for (final CachedLogEntry entry : this.getEntries(repoCache)) {
            if (ui.isCanceled()) {
                throw new OperationCanceledException();
            }
            handler.handleLogEntry(entry);
        }
    }

    /**
     * Maps the root of a working copy to the corresponding {@link SvnRepo} object.
     * @param workingCopyRoot The path pointing at the root of some working copy.
     * @return A suitable {@link SvnRepo} object or {@code null} if the path passed is unknown.
     */
    SvnRepo mapWorkingCopyRootToRepository(final File workingCopyRoot)
            throws SVNException {
        final RepoDataCache cache = this.getRepoCache(workingCopyRoot);
        return cache.getRepo();
    }

    /**
     * Returns a {@link RepoDataCache} object for a working copy root.
     * @param workingCopyRoot The working copy root.
     * @throws SVNException if the working copy does not exist or is corrupt
     */
    private synchronized RepoDataCache getRepoCache(final File workingCopyRoot) throws SVNException {

        RepoDataCache c = this.repoDataPerWcRoot.get(workingCopyRoot.toString());
        if (c == null) {
            final SVNWCClient wcClient = this.mgr.getWCClient();
            final SVNURL rootUrl = wcClient.getReposRoot(workingCopyRoot, null, SVNRevision.WORKING);
            final SVNInfo wcInfo = wcClient.doInfo(workingCopyRoot, SVNRevision.WORKING);
            final SVNURL wcUrl = wcInfo.getURL();
            final String relPath = wcUrl.toString().substring(rootUrl.toString().length());
            c = new RepoDataCache(new SvnRepo(
                    this.mgr,
                    wcInfo.getRepositoryUUID(),
                    workingCopyRoot,
                    rootUrl,
                    relPath,
                    this.determineCheckoutPrefix(wcUrl, rootUrl)));
            this.repoDataPerWcRoot.put(workingCopyRoot.toString(), c);
            CachedLog.this.tryToReadCacheFromFile(c);
        }
        return c;
    }

    private synchronized List<CachedLogEntry> getEntries(final RepoDataCache repoCache)
        throws SVNException {

        final boolean gotNewEntries = this.loadNewEntries(repoCache);

        if (gotNewEntries) {
            final Job job = Job.create("Storing SVN review cache for " + repoCache.getRepo().getLocalRoot(),
                    new IJobFunction() {
                        @Override
                        public IStatus run(IProgressMonitor monitor) {
                            CachedLog.this.tryToStoreCacheToFile(repoCache);
                            return Status.OK_STATUS;
                        }
                    });
            job.setRule(new CacheJobMutexRule(repoCache.getCacheFilePath().toFile()));
            job.schedule();
        }

        return repoCache.getEntries();
    }

    private boolean loadNewEntries(final RepoDataCache repoCache) throws SVNException {

        final List<CachedLogEntry> entries = repoCache.getEntries();
        final long lastKnownRevision = entries.isEmpty() ? 0 : entries.get(entries.size() - 1).getRevision();
        final SvnRepo repo = repoCache.getRepo();
        final long latestRevision = this.getLatestRevision(repoCache);

        final List<CachedLogEntry> newEntries = new ArrayList<>();
        if (lastKnownRevision < latestRevision) {
            final long startRevision = lastKnownRevision == 0
                    ? Math.max(0, latestRevision - this.minCount + 1) : lastKnownRevision + 1;

            Logger.info("Processing revisions " + startRevision + ".." + latestRevision
                    + " from " + repo.getRemoteUrl());
            this.mgr.getLogClient().doLog(
                    repo.getRemoteUrl(),
                    new String[] { repo.getRelativePath() },
                    SVNRevision.create(latestRevision),
                    SVNRevision.create(startRevision),
                    SVNRevision.create(latestRevision),
                    false,
                    true,
                    false,
                    0,
                    new String[0],
                    new ISVNLogEntryHandler() {
                        @Override
                        public void handleLogEntry(SVNLogEntry logEntry) throws SVNException {
                            final CachedLogEntry entry = new CachedLogEntry(logEntry);
                            CachedLog.this.processLogEntry(entry, repo, newEntries.size());
                            newEntries.add(entry);
                        }
                    });
        }

        entries.addAll(newEntries);
        return !newEntries.isEmpty();
    }

    /**
     * Returns the latest revision for given repository. The result respects the associated working copy
     * and is hence more exact than {@link org.tmatesoft.svn.core.io.SVNRepository#getLatestRevision()}.
     * @param repoCache The {@link RepoDataCache} to use.
     * @return The latest revision of the repository wrt. the associated working copy.
     * @throws SVNException if some SVN problem occurred
     */
    private long getLatestRevision(final RepoDataCache repoCache) throws SVNException {
        final ValueWrapper<Long> latestRevision = new ValueWrapper<>(0L);

        final SvnRepo repo = repoCache.getRepo();
        this.mgr.getLogClient().doLog(
                repo.getRemoteUrl(),
                new String[] { repo.getRelativePath() },
                SVNRevision.HEAD,
                SVNRevision.HEAD,
                SVNRevision.create(0),
                false,
                true,
                false,
                1,
                new String[0],
                new ISVNLogEntryHandler() {
                    @Override
                    public void handleLogEntry(SVNLogEntry logEntry) throws SVNException {
                        latestRevision.setValue(logEntry.getRevision());
                    }
                });

        return latestRevision.get();
    }

    private int determineCheckoutPrefix(final SVNURL wcUrl, final SVNURL rootUrl)
        throws SVNException {

        SVNURL checkoutRootUrlPrefix = wcUrl;
        int i = 0;
        while (!(checkoutRootUrlPrefix.equals(rootUrl) || checkoutRootUrlPrefix.getPath().equals("//"))) {
            checkoutRootUrlPrefix = checkoutRootUrlPrefix.removePathTail();
            i++;
        }
        return i;
    }

    private void tryToReadCacheFromFile(final RepoDataCache c) {
        try {
            this.readCacheFromFile(c);
        } catch (final ClassNotFoundException | IOException | ClassCastException e) {
            Logger.error("Problem while loading SVN history data for " + c.getRepo().getRemoteUrl(), e);
        }
    }

    private synchronized void readCacheFromFile(final RepoDataCache c)
            throws IOException, ClassNotFoundException {

        final File cache = c.getCacheFilePath().toFile();
        if (!cache.exists()) {
            Logger.info("SVN cache " + cache + " is missing for " + c.getRepo().getRemoteUrl() + ", nothing to load");
            return;
        }
        Logger.info("Loading SVN history data for " + c.getRepo().getRemoteUrl() + " from " + cache);
        try (ObjectInputStream ois =
                new ObjectInputStream(new BufferedInputStream(new FileInputStream(cache)))) {

            @SuppressWarnings("unchecked") final List<CachedLogEntry> value =
                    (List<CachedLogEntry>) ois.readObject();
            final SvnFileHistoryGraph historyGraph = (SvnFileHistoryGraph) ois.readObject();

            c.getEntries().addAll(value);
            c.getRepo().setRemoteFileHistoryGraph(historyGraph);
        }
        Logger.info("Loaded SVN history data for " + c.getRepo().getRemoteUrl() + " from " + cache);
    }

    private void tryToStoreCacheToFile(final RepoDataCache c) {
        try {
            this.storeCacheToFile(c);
        } catch (final IOException e) {
            Logger.error("Problem while storing SVN history data for " + c.getRepo().getRemoteUrl(), e);
        }
    }

    private void storeCacheToFile(final RepoDataCache c) throws IOException {
        final File cache = c.getCacheFilePath().toFile();
        Logger.info("Storing SVN history data for " + c.getRepo().getRemoteUrl() + " to " + cache);
        try (ObjectOutputStream oos =
                new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(cache)))) {

            final List<CachedLogEntry> entries = c.getEntries();
            final int numEntries = entries.size();
            final List<CachedLogEntry> subList = new ArrayList<>(entries.subList(
                    numEntries - Integer.min(numEntries, this.maxCount),
                    numEntries
            ));
            oos.writeObject(subList);
            oos.writeObject(c.getRepo().getRemoteFileHistoryGraph());
        }
        Logger.info("Stored SVN history data for " + c.getRepo().getRemoteUrl() + " to " + cache);
    }

    private void processLogEntry(
            final CachedLogEntry entry,
            final SvnRepo repo,
            final int numEntriesProcessed) {

        final SvnRevision revision = new SvnRevision(repo, entry);
        repo.getRemoteFileHistoryGraph().processRevision(revision);
        final int numEntriesProcessedNow = numEntriesProcessed + 1;
        if (numEntriesProcessedNow % REVISION_BLOCK_SIZE == 0) {
            Logger.debug(numEntriesProcessedNow + " revisions processed");
        }
    }
}
