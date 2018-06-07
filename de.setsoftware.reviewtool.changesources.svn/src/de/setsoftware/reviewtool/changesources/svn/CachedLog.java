package de.setsoftware.reviewtool.changesources.svn;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
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

    private static final long REVISION_BLOCK_SIZE = 500L;

    /**
     * Data regarding the repository. Is only cached in memory.
     */
    private static final class RepoDataCache {

        private final SvnRepo repo;
        private final List<CachedLogEntry> entries;

        public RepoDataCache(final SvnRepo repo) {
            this.repo = repo;
            this.entries = new ArrayList<>();
        }

        public SvnRepo getRepo() {
            return this.repo;
        }

        public List<CachedLogEntry> getEntries() {
            return this.entries;
        }
    }

    private static final CachedLog INSTANCE = new CachedLog();

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

        final Job job = Job.create("Loading SVN review cache", new IJobFunction() {
            @Override
            public IStatus run(IProgressMonitor monitor) {
                CachedLog.this.tryToReadCacheFromFile();
                return Status.OK_STATUS;
            }
        });
        job.schedule();
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
            final RepoDataCache c = this.getRepoCache(workingCopyRoot, null);
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

        final RepoDataCache repoCache = this.getRepoCache(workingCopyRoot, null);
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
        final RepoDataCache cache = this.getRepoCache(workingCopyRoot, null);
        return cache.getRepo();
    }

    /**
     * Returns a {@link RepoDataCache} object for a working copy root.
     * @param workingCopyRoot The working copy root.
     * @throws SVNException if the working copy does not exist or is corrupt
     */
    private synchronized RepoDataCache getRepoCache(
            final File workingCopyRoot,
            final SvnFileHistoryGraph fileHistoryGraph) throws SVNException {

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
                    this.determineCheckoutPrefix(wcUrl, rootUrl),
                    fileHistoryGraph));
            this.repoDataPerWcRoot.put(workingCopyRoot.toString(), c);
        } else if (fileHistoryGraph != null) {
            c.getRepo().setRemoteFileHistoryGraph(fileHistoryGraph);
        }
        return c;
    }

    private synchronized List<CachedLogEntry> getEntries(final RepoDataCache repoCache)
        throws SVNException {

        final boolean gotNewEntries = this.loadNewEntries(repoCache);

        if (gotNewEntries) {
            this.tryToStoreCacheToFile();
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

    private void tryToReadCacheFromFile() {
        final File cache = this.getCacheFilePath().toFile();
        try {
            this.readCacheFromFile(cache);
        } catch (final ClassNotFoundException | IOException | ClassCastException e) {
            Logger.error("Problem while loading SVN history data from cache " + cache, e);
        }
    }

    private synchronized void readCacheFromFile(final File cache) throws IOException, ClassNotFoundException {
        if (!cache.exists()) {
            Logger.info("SVN cache " + cache + " is missing, nothing to load");
            return;
        }
        Logger.info("Loading SVN history data from cache " + cache);
        try (ObjectInputStream ois =
                new ObjectInputStream(new BufferedInputStream(new FileInputStream(cache)))) {
            while (true) {
                final File wcRoot;
                try {
                    wcRoot = new File(ois.readUTF());
                } catch (final EOFException ex) {
                    break;
                }

                try {
                    Logger.debug("Loading SVN history data for working copy in " + wcRoot);
                    @SuppressWarnings("unchecked") final List<CachedLogEntry> value =
                            (List<CachedLogEntry>) ois.readObject();

                    final SvnFileHistoryGraph historyGraph = (SvnFileHistoryGraph) ois.readObject();

                    Logger.debug("Loaded SVN history data for working copy in " + wcRoot);
                    final RepoDataCache c = this.getRepoCache(wcRoot, historyGraph);
                    c.getEntries().addAll(value);
                } catch (final SVNException ex) {
                    Logger.warn("Ignoring SVN history data for non-existing or corrupt working copy in " + wcRoot, ex);
                }
            }
        }
    }

    private void tryToStoreCacheToFile() {
        final File cache = this.getCacheFilePath().toFile();
        try {
            this.storeCacheToFile(cache);
        } catch (final IOException e) {
            Logger.error("Problem while storing SVN history data to cache " + cache, e);
        }
    }

    private void storeCacheToFile(final File cache) throws IOException {
        Logger.info("Storing SVN history data to cache " + cache);
        try (ObjectOutputStream oos =
                new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(cache)))) {
            for (final RepoDataCache repo : this.repoDataPerWcRoot.values()) {
                Logger.debug("Storing SVN history data for working copy in " + repo.getRepo().getLocalRoot());
                oos.writeUTF(repo.getRepo().getLocalRoot().toString());
                final List<CachedLogEntry> entries = repo.getEntries();
                final int numEntries = entries.size();
                final List<CachedLogEntry> subList = new ArrayList<>(entries.subList(
                        numEntries - Integer.min(numEntries, this.maxCount),
                        numEntries
                ));
                oos.writeObject(subList);
                oos.writeObject(repo.getRepo().getRemoteFileHistoryGraph());
                Logger.debug("Stored SVN history data for working copy in " + repo.getRepo().getLocalRoot());
            }
        }
    }

    private IPath getCacheFilePath() {
        final Bundle bundle = FrameworkUtil.getBundle(this.getClass());
        final IPath dir = Platform.getStateLocation(bundle);
        return dir.append("svnlog.cache");
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
