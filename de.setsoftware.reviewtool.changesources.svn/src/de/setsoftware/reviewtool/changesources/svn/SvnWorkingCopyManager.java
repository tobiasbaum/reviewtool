package de.setsoftware.reviewtool.changesources.svn;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.ISVNStatusHandler;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNInfo;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNStatus;

import de.setsoftware.reviewtool.base.Pair;
import de.setsoftware.reviewtool.diffalgorithms.DiffAlgorithmFactory;
import de.setsoftware.reviewtool.model.api.BackgroundJobExecutor;
import de.setsoftware.reviewtool.model.api.IChangeSourceUi;
import de.setsoftware.reviewtool.model.api.IMutableFileHistoryGraph;
import de.setsoftware.reviewtool.model.changestructure.FileHistoryGraph;

/**
 * Manages all known local working copies.
 */
final class SvnWorkingCopyManager {

    private static final SvnWorkingCopyManager INSTANCE = new SvnWorkingCopyManager();

    private final Map<String, SvnWorkingCopy> wcPerRootDirectory;
    private SVNClientManager mgr;

    /**
     * Constructor.
     */
    private SvnWorkingCopyManager() {
        this.wcPerRootDirectory = new LinkedHashMap<>();
    }

    /**
     * Returns the singleton instance of this class.
     */
    static SvnWorkingCopyManager getInstance() {
        return INSTANCE;
    }

    /**
     * Initializes the cache.
     * @param mgr The {@link SVNClientManager} for retrieving information about working copies.
     */
    void init(final SVNClientManager mgr) {
        this.mgr = mgr;
    }

    /**
     * Returns a read-only view of all known Subversion working copies.
     */
    synchronized Collection<SvnWorkingCopy> getWorkingCopies() {
        return Collections.unmodifiableCollection(new ArrayList<>(this.wcPerRootDirectory.values()));
    }

    /**
     * Returns a repository by its working copy root.
     * @param workingCopyRoot The root directory of the working copy.
     * @return A {@link SvnRepo} or {@code null} if not found.
     */
    synchronized SvnWorkingCopy getWorkingCopy(final File workingCopyRoot) {
        SvnWorkingCopy wc = this.wcPerRootDirectory.get(workingCopyRoot.toString());
        if (wc == null) {
            final SVNInfo wcInfo;
            try {
                wcInfo = this.mgr.getWCClient().doInfo(workingCopyRoot, SVNRevision.WORKING);
            } catch (final SVNException e) {
                // not a working copy
                // don't log this one as it is probably an unversioned project and hence not an error
                return null;
            }

            final SVNURL wcUrl = wcInfo.getURL();
            final SvnRepo repo = SvnRepositoryManager.getInstance().getRepo(wcUrl);
            if (repo == null) {
                return null;
            }

            wc = new SvnWorkingCopy(repo, workingCopyRoot);
            this.wcPerRootDirectory.put(workingCopyRoot.toString(), wc);
        }
        return wc;
    }

    /**
     * Calls the given handler for all recent log entries of all known working copies.
     */
    synchronized List<Pair<SvnWorkingCopy, SvnRepoRevision>> traverseRecentEntries(
            final CachedLogLookupHandler handler,
            final IChangeSourceUi ui) throws SVNException {

        final List<Pair<SvnWorkingCopy, SvnRepoRevision>> revisions = new ArrayList<>();
        for (final SvnWorkingCopy wc : this.wcPerRootDirectory.values()) {
            if (ui.isCanceled()) {
                throw BackgroundJobExecutor.createOperationCanceledException();
            }

            final Pair<Boolean, List<SvnRepoRevision>> getEntriesResult =
                    SvnRepositoryManager.getInstance().traverseRecentEntries(wc.getRepository(), handler, ui);
            for (final SvnRepoRevision revision : getEntriesResult.getSecond()) {
                revisions.add(Pair.create(wc, revision));
            }

            if (getEntriesResult.getFirst()) {
                // remote history has changed, we have to rebuild the local file history graph
                this.collectWorkingCopyChanges(wc, Collections.<File>emptyList());
            }
        }
        return revisions;
    }

    /**
     * Removes a working copy.
     * @param workingCopyRoot The root directory of the working copy.
     */
    synchronized void removeWorkingCopy(final File workingCopyRoot) {
        this.wcPerRootDirectory.remove(workingCopyRoot.toString());
    }


    /**
     * Collects all local changes and integrates them into the {@link SvnFileHistoryGraph}.
     * @param relevantPaths The list of additional paths to check. If {@code null}, the whole working copy is analyzed.
     */
    void collectWorkingCopyChanges(final List<File> relevantPaths) throws SVNException {
        for (final SvnWorkingCopy wc : SvnWorkingCopyManager.getInstance().getWorkingCopies()) {
            this.collectWorkingCopyChanges(wc, relevantPaths);
        }
    }

    /**
     * Collects all local changes of a given working copy and integrates them into the {@link SvnFileHistoryGraph}.
     * @param relevantPaths The list of additional paths to check. If {@code null}, the whole working copy is analyzed.
     */
    private void collectWorkingCopyChanges(final SvnWorkingCopy wc, final List<? extends File> relevantPaths)
            throws SVNException {

        final SortedMap<String, CachedLogEntryPath> changeMap = new TreeMap<>();
        final ISVNStatusHandler handler = new ISVNStatusHandler() {
            @Override
            public void handleStatus(final SVNStatus status) throws SVNException {
                if (status.isVersioned()) {
                    final CachedLogEntryPath entry = new CachedLogEntryPath(wc.getRepository(), status);
                    changeMap.put(entry.getPath(), entry);
                }
            }
        };

        if (relevantPaths != null) {
            final Set<File> filteredPaths = this.filterPaths(relevantPaths, wc);
            this.collectWorkingCopyChanges(filteredPaths, handler);
        } else {
            this.collectWorkingCopyChanges(wc, handler);
        }

        final SvnWorkingCopyRevision wcRevision = new SvnWorkingCopyRevision(wc, changeMap);
        final IMutableFileHistoryGraph localFileHistoryGraph =
                new FileHistoryGraph(DiffAlgorithmFactory.createDefault());
        wcRevision.integrateInto(localFileHistoryGraph);
        wc.setLocalFileHistoryGraph(localFileHistoryGraph);
    }

    /**
     * Collects local changes given a set of paths.
     * @param paths The paths to consider.
     * @param handler Receives information about changes files.
     */
    private void collectWorkingCopyChanges(final Set<File> paths, final ISVNStatusHandler handler)
            throws SVNException {

        for (final File path : paths) {
            this.mgr.getStatusClient().doStatus(
                    path,
                    SVNRevision.WORKING,
                    SVNDepth.EMPTY,
                    false, // no remote
                    false, // report only modified files
                    false, // don't include ignored files
                    false, // ignored
                    handler,
                    null); // no change lists
        }
    }

    /**
     * Collects local changes within a whole working copy.
     * @param wc The working copy to consider.
     * @param handler Receives information about changes files.
     */
    private void collectWorkingCopyChanges(final SvnWorkingCopy wc, final ISVNStatusHandler handler)
            throws SVNException {

        this.mgr.getStatusClient().doStatus(
                wc.getLocalRoot(), // analyse whole working copy
                SVNRevision.WORKING,
                SVNDepth.INFINITY,
                false, // no remote
                false, // report only modified files
                false, // don't include ignored files
                false, // ignored
                handler,
                null); // no change lists
    }

    /**
     * Filters out paths that do not belong to passed working copy.
     * @param relevantPaths The paths to filter.
     * @param wc The relevant working copy.
     * @return A set of filtered paths.
     */
    private Set<File> filterPaths(final List<? extends File> relevantPaths, final SvnWorkingCopy wc) {
        final Set<File> paths = new LinkedHashSet<>();
        for (final File path : relevantPaths) {
            final String repoPath = wc.toAbsolutePathInRepo(path);
            if (repoPath != null) {
                paths.add(path);
            }
        }

        for (final String repoPath : wc.getLocalFileHistoryGraph().getPaths()) {
            final File path = wc.toAbsolutePathInWc(repoPath);
            if (path != null && path.isFile()) {
                paths.add(path);
            }
        }

        return paths;
    }
}
