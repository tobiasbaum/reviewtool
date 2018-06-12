package de.setsoftware.reviewtool.changesources.svn;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobFunction;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
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

    /**
     * Scheduling rule preventing two jobs to run concurrently for the same working copy.
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
     * @param workingCopyRoots The root directories of all known working copies.
     */
    void init(
            final SVNClientManager mgr,
            final Set<File> workingCopyRoots) {

        this.mgr = mgr;

        for (final File workingCopyRoot : workingCopyRoots) {
            final Job job = Job.create("Loading SVN review cache for " + workingCopyRoot,
                    new IJobFunction() {
                        @Override
                        public IStatus run(IProgressMonitor monitor) {
                            SvnWorkingCopyManager.this.getWorkingCopy(workingCopyRoot);
                            return Status.OK_STATUS;
                        }
                    });
            job.setRule(new CacheJobMutexRule(workingCopyRoot));
            job.schedule();
        }
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
     * Calls the given handler for all recent log entries of the given working copy root.
     * All revisions returned have an associated working copy.
     */
    List<Pair<SvnWorkingCopy, SvnRevision>> traverseRecentEntries(
            final File workingCopyRoot,
            final CachedLogLookupHandler handler,
            final IChangeSourceUi ui) throws SVNException {

        final SvnWorkingCopy wc = this.getWorkingCopy(workingCopyRoot);
        if (wc == null) {
            return Collections.emptyList();
        }

        List<Pair<SvnWorkingCopy, SvnRevision>> revisions = new ArrayList<>();
        for (final SvnRevision revision :
                SvnRepositoryManager.getInstance().traverseRecentEntries(wc.getRepository(), handler, ui)) {
            revisions.add(Pair.create(wc, revision));
        }
        return revisions;
    }
}
