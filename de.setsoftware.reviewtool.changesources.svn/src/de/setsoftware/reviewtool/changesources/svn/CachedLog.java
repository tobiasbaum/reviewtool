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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.tmatesoft.svn.core.ISVNLogEntryHandler;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;

import de.setsoftware.reviewtool.base.Logger;
import de.setsoftware.reviewtool.model.api.IChangeSourceUi;

/**
 * A local cache of the SVN log(s) to speed up the gathering of relevant entries.
 */
public class CachedLog {

    /**
     * Data regarding the repository. Is only cached in memory.
     */
    private static final class RepoDataCache {

        private final String relPath;
        private final SvnRepo repo;

        public RepoDataCache(String relPath, SvnRepo repo) {
            this.relPath = relPath;
            this.repo = repo;
        }

        public SvnRepo getRepo() {
            return this.repo;
        }
    }

    private static final CachedLog INSTANCE = new CachedLog();

    private final Map<String, RepoDataCache> repoDataPerWcRoot;
    private final Map<String, List<CachedLogEntry>> entriesPerWcRoot;
    private int minCount;
    private int maxCount;

    private CachedLog() {
        this.repoDataPerWcRoot = new HashMap<>();
        this.entriesPerWcRoot = new HashMap<>();
        this.minCount = 1000;
        this.maxCount = 1000;

        try {
            this.readCacheFromFile();
        } catch (final ClassNotFoundException | IOException | ClassCastException e) {
            Logger.error("problem while loading svn cache", e);
        }
    }

    public static CachedLog getInstance() {
        return INSTANCE;
    }

    /**
     * Changes the default values for minimum and maximum size of the log.
     */
    public void setSizeLimits(int minCount, int maxCount) {
        this.minCount = Math.min(minCount, maxCount);
        this.maxCount = Math.max(minCount, maxCount);
    }

    /**
     * Returns a collection of all known Subversion repositories.
     */
    public Collection<SvnRepo> getRepositories() {
        final List<SvnRepo> result = new ArrayList<>();
        for (final RepoDataCache info : this.repoDataPerWcRoot.values()) {
            result.add(info.getRepo());
        }
        return result;
    }

    /**
     * Calls the given handler for all recent log entries of the given working copy root.
     */
    public void traverseRecentEntries(
            final SVNClientManager mgr, final File workingCopyRoot, final CachedLogLookupHandler handler,
            final IChangeSourceUi ui) throws SVNException {

        final RepoDataCache repoCache = this.getRepoCache(mgr, workingCopyRoot);
        handler.startNewRepo(repoCache.getRepo());
        for (final CachedLogEntry entry : this.getEntries(mgr, repoCache)) {
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
    public SvnRepo mapWorkingCopyRootToRepository(final SVNClientManager mgr, final File workingCopyRoot)
            throws SVNException {
        final RepoDataCache cache = this.getRepoCache(mgr, workingCopyRoot);
        return cache == null ? null : cache.getRepo();
    }

    private synchronized RepoDataCache getRepoCache(SVNClientManager mgr, File workingCopyRoot) throws SVNException {
        RepoDataCache c = this.repoDataPerWcRoot.get(workingCopyRoot.toString());
        if (c == null) {
            final SVNURL rootUrl = mgr.getLogClient().getReposRoot(workingCopyRoot, null, SVNRevision.HEAD);
            final SVNURL wcUrl = mgr.getWCClient().doInfo(workingCopyRoot, SVNRevision.WORKING).getURL();
            final String relPath = wcUrl.toString().substring(rootUrl.toString().length());
            c = new RepoDataCache(relPath, new SvnRepo(
                    mgr,
                    mgr.getWCClient().doInfo(workingCopyRoot, SVNRevision.HEAD).getRepositoryUUID(),
                    workingCopyRoot,
                    rootUrl,
                    relPath,
                    this.determineCheckoutPrefix(mgr, workingCopyRoot, rootUrl)));
            this.repoDataPerWcRoot.put(workingCopyRoot.toString(), c);
        }
        return c;
    }

    private synchronized List<CachedLogEntry> getEntries(SVNClientManager mgr, RepoDataCache repoCache)
        throws SVNException {

        final String wcRootString = repoCache.getRepo().getLocalRoot().toString();
        List<CachedLogEntry> list = this.entriesPerWcRoot.get(wcRootString);
        if (list == null) {
            list = new CopyOnWriteArrayList<>();
            this.entriesPerWcRoot.put(wcRootString, list);
        }

        final boolean gotNewEntries = this.loadNewEntries(mgr, repoCache, list);

        if (gotNewEntries) {
            try {
                this.storeCacheToFile();
            } catch (final IOException e) {
                Logger.error("problem while caching svn log", e);
            }
        }

        return list;
    }

    private boolean loadNewEntries(SVNClientManager mgr, RepoDataCache repoCache, List<CachedLogEntry> list)
        throws SVNException {

        final long lastKnownRevision = list.isEmpty() ? 0 : list.get(0).getRevision();

        final ArrayList<CachedLogEntry> newEntries = new ArrayList<>();
        mgr.getLogClient().doLog(
                repoCache.getRepo().getRemoteUrl(),
                new String[] { repoCache.relPath },
                SVNRevision.HEAD,
                SVNRevision.HEAD,
                SVNRevision.create(lastKnownRevision),
                false,
                true,
                false,
                this.minCount,
                new String[0],
                new ISVNLogEntryHandler() {
                    @Override
                    public void handleLogEntry(SVNLogEntry logEntry) throws SVNException {
                        if (logEntry.getRevision() > lastKnownRevision) {
                            newEntries.add(new CachedLogEntry(logEntry));
                        }
                    }
                });

        Collections.sort(newEntries, new Comparator<CachedLogEntry>() {
            @Override
            public int compare(CachedLogEntry o1, CachedLogEntry o2) {
                return Long.compare(o2.getRevision(), o1.getRevision());
            }
        });
        list.addAll(0, newEntries);
        return !newEntries.isEmpty();
    }

    private int determineCheckoutPrefix(SVNClientManager mgr, File workingCopyRoot, SVNURL rootUrl)
        throws SVNException {

        SVNURL checkoutRootUrlPrefix = mgr.getWCClient().doInfo(workingCopyRoot, SVNRevision.HEAD).getURL();
        int i = 0;
        while (!(checkoutRootUrlPrefix.equals(rootUrl) || checkoutRootUrlPrefix.getPath().equals("//"))) {
            checkoutRootUrlPrefix = checkoutRootUrlPrefix.removePathTail();
            i++;
        }
        return i;
    }

    private void readCacheFromFile() throws IOException, ClassNotFoundException {
        final File file = this.getCacheFilePath().toFile();
        if (!file.exists()) {
            return;
        }
        try (ObjectInputStream ois =
                new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            while (true) {
                String key;
                try {
                    key = ois.readUTF();
                } catch (final EOFException ex) {
                    break;
                }
                final List<CachedLogEntry> value = (List<CachedLogEntry>) ois.readObject();
                this.entriesPerWcRoot.put(key, value);
            }
        }
    }

    private void storeCacheToFile() throws IOException {
        final IPath file = this.getCacheFilePath();
        try (ObjectOutputStream oos =
                new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file.toFile())))) {
            int entryCount = 0;
            for (final Entry<String, List<CachedLogEntry>> e : this.entriesPerWcRoot.entrySet()) {
                oos.writeUTF(e.getKey());
                oos.writeObject(e.getValue());
                entryCount++;
                if (entryCount > this.maxCount) {
                    break;
                }
            }
        }
    }

    private IPath getCacheFilePath() {
        final Bundle bundle = FrameworkUtil.getBundle(this.getClass());
        final IPath dir = Platform.getStateLocation(bundle);
        return dir.append("svnlog.cache");
    }

}