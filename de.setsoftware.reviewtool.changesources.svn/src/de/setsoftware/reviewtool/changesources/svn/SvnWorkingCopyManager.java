package de.setsoftware.reviewtool.changesources.svn;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.OperationCanceledException;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNInfo;
import org.tmatesoft.svn.core.wc.SVNRevision;

import de.setsoftware.reviewtool.base.Pair;
import de.setsoftware.reviewtool.model.api.IChangeSourceUi;

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
        return Collections.unmodifiableCollection(this.wcPerRootDirectory.values());
    }

    /**
     * Returns a repository by its working copy root.
     * @param workingCopyRoot The root directory of the working copy.
     * @return A {@link SvnRepo} or {@code null} if not found.
     */
    synchronized SvnWorkingCopy getWorkingCopy(final File workingCopyRoot) {
        try {
            SvnWorkingCopy wc = this.wcPerRootDirectory.get(workingCopyRoot.toString());
            if (wc == null) {
                final SVNInfo wcInfo = this.mgr.getWCClient().doInfo(workingCopyRoot, SVNRevision.WORKING);
                final SVNURL wcUrl = wcInfo.getURL();
                final SvnRepo repo = SvnRepositoryManager.getInstance().getRepo(wcUrl);
                if (repo == null) {
                    return null;
                }

                final SVNURL repoUrl = repo.getRemoteUrl();
                final String relPath = wcUrl.toString().substring(repoUrl.toString().length());
                wc = new SvnWorkingCopy(repo, workingCopyRoot, relPath);
                this.wcPerRootDirectory.put(workingCopyRoot.toString(), wc);
            }
            return wc;
        } catch (final SVNException e) {
            return null;
        }
    }

    /**
     * Calls the given handler for all recent log entries of all known working copies.
     */
    synchronized List<Pair<SvnWorkingCopy, SvnRepoRevision>> traverseRecentEntries(
            final CachedLogLookupHandler handler,
            final IChangeSourceUi ui) throws SVNException {

        List<Pair<SvnWorkingCopy, SvnRepoRevision>> revisions = new ArrayList<>();
        for (final SvnWorkingCopy wc : this.wcPerRootDirectory.values()) {
            if (ui.isCanceled()) {
                throw new OperationCanceledException();
            }

            for (final SvnRepoRevision revision :
                    SvnRepositoryManager.getInstance().traverseRecentEntries(wc.getRepository(), handler, ui)) {
                revisions.add(Pair.create(wc, revision));
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
}
