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
import java.util.regex.Pattern;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc.DefaultSVNAuthenticationManager;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;

import de.setsoftware.reviewtool.base.Logger;
import de.setsoftware.reviewtool.base.Pair;
import de.setsoftware.reviewtool.model.api.BackgroundJobExecutor;
import de.setsoftware.reviewtool.model.api.ChangeSourceException;
import de.setsoftware.reviewtool.model.api.IChange;
import de.setsoftware.reviewtool.model.api.IChangeData;
import de.setsoftware.reviewtool.model.api.IChangeSourceUi;
import de.setsoftware.reviewtool.model.api.ICommit;
import de.setsoftware.reviewtool.model.api.ICortProgressMonitor;
import de.setsoftware.reviewtool.model.api.IFileHistoryNode;
import de.setsoftware.reviewtool.model.api.IRevisionedFile;
import de.setsoftware.reviewtool.model.changestructure.AbstractChangeSource;
import de.setsoftware.reviewtool.model.changestructure.ChangestructureFactory;

/**
 * A simple change source that loads the changes from subversion.
 */
final class SvnChangeSource extends AbstractChangeSource {

    private final SVNClientManager mgr = SVNClientManager.newInstance();

    SvnChangeSource(
            final String logMessagePattern,
            final String user,
            final String pwd,
            final long maxTextDiffThreshold,
            final int logCacheMinSize) {
        super(logMessagePattern, maxTextDiffThreshold);

        this.mgr.setAuthenticationManager(new DefaultSVNAuthenticationManager(
                null, false, user, pwd.toCharArray(), null, null));

        SvnRepositoryManager.getInstance().init(this.mgr, logCacheMinSize);
        SvnWorkingCopyManager.getInstance().init(this.mgr);
    }

    @Override
    public File determineWorkingCopyRoot(final File projectRoot) {
        File curPotentialRoot = projectRoot;
        while (!this.isPotentialRoot(curPotentialRoot)) {
            curPotentialRoot = curPotentialRoot.getParentFile();
            if (curPotentialRoot == null) {
                return null;
            }
        }
        while (true) {
            final File next = curPotentialRoot.getParentFile();
            if (next == null || !this.isPotentialRoot(next)) {
                return curPotentialRoot;
            }
            curPotentialRoot = next;
        }
    }

    private boolean isPotentialRoot(final File next) {
        final File dotsvn = new File(next, ".svn");
        return dotsvn.isDirectory();
    }

    @Override
    public IChangeData getRepositoryChanges(final String key, final IChangeSourceUi ui) throws ChangeSourceException {
        try {
            ui.subTask("Determining relevant commits...");
            final List<Pair<SvnWorkingCopy, SvnRepoRevision>> revisions = this.determineRelevantRevisions(key, ui);
            final Map<ISvnRepo, Long> neededRevisionPerRepo = this.determineMaxRevisionPerRepo(revisions);
            ui.subTask("Checking state of working copy...");
            this.checkWorkingCopiesUpToDate(neededRevisionPerRepo, ui);
            ui.subTask("Analyzing commits...");
            final List<ICommit> commits = this.convertRepoRevisionsToChanges(revisions, ui);
            return ChangestructureFactory.createChangeData(commits);
        } catch (final SVNException e) {
            throw new ChangeSourceException(this, e);
        }
    }

    @Override
    public void analyzeLocalChanges(final List<File> relevantPaths) throws ChangeSourceException {
        try {
            SvnWorkingCopyManager.getInstance().collectWorkingCopyChanges(relevantPaths);
        } catch (final SVNException e) {
            throw new ChangeSourceException(this, e);
        }
    }

    @Override
    protected void workingCopyAdded(File wcRoot) {
        SvnWorkingCopyManager.getInstance().getWorkingCopy(wcRoot);
    }

    @Override
    protected void workingCopyRemoved(File wcRoot) {
        SvnWorkingCopyManager.getInstance().removeWorkingCopy(wcRoot);
    }

    /**
     * Checks whether the working copy should be updated in order to incorporate remote changes.
     * @param neededRevisionPerRepo A map storing the last known revisions for each repository.
     */
    private void checkWorkingCopiesUpToDate(
            final Map<ISvnRepo, Long> neededRevisionPerRepo,
            final IChangeSourceUi ui) throws SVNException {

        for (final SvnWorkingCopy wc : SvnWorkingCopyManager.getInstance().getWorkingCopies()) {
            if (ui.isCanceled()) {
                throw BackgroundJobExecutor.createOperationCanceledException();
            }

            final ISvnRepo repo = wc.getRepository();
            if (neededRevisionPerRepo.containsKey(repo)) {
                final long remoteRev = neededRevisionPerRepo.get(repo);
                final File wcRoot = wc.getLocalRoot();
                final long wcRev = this.mgr.getStatusClient().doStatus(wcRoot, false).getRevision().getNumber();
                if (wcRev < remoteRev) {
                    final Boolean doUpdate = ui.handleLocalWorkingIncomplete("The working copy (" + wc.toString()
                            + ") does not contain all relevant changes. Perform an update?");
                    if (doUpdate == null) {
                        throw BackgroundJobExecutor.createOperationCanceledException();
                    }
                    if (doUpdate) {
                        this.mgr.getUpdateClient().doUpdate(wcRoot, SVNRevision.HEAD, SVNDepth.INFINITY, true, false);
                    }
                }
            }
        }
    }

    private Map<ISvnRepo, Long> determineMaxRevisionPerRepo(
            final List<Pair<SvnWorkingCopy, SvnRepoRevision>> revisions) {

        final Map<ISvnRepo, Long> ret = new LinkedHashMap<>();
        for (final Pair<SvnWorkingCopy, SvnRepoRevision> p : revisions) {
            final SvnRepoRevision revision = p.getSecond();
            final ISvnRepo repo = revision.getRepository();
            final long curRev = revision.getRevisionNumber();
            if (ret.containsKey(repo)) {
                if (curRev > ret.get(repo)) {
                    ret.put(repo, curRev);
                }
            } else {
                ret.put(repo, curRev);
            }

        }
        return ret;
    }

    private List<Pair<SvnWorkingCopy, SvnRepoRevision>> determineRelevantRevisions(
            final String key,
            final IChangeSourceUi ui) throws SVNException {

        final Pattern pattern = this.createPatternForKey(key);
        final CachedLogLookupHandler handler = new CachedLogLookupHandler() {

            @Override
            public boolean handleLogEntry(final CachedLogEntry logEntry) throws SVNException {
                final String message = logEntry.getMessage();
                return message != null && pattern.matcher(message).matches();
            }
        };

        return SvnWorkingCopyManager.getInstance().traverseRecentEntries(handler, ui);
    }

    private List<ICommit> convertRepoRevisionsToChanges(
            final List<Pair<SvnWorkingCopy, SvnRepoRevision>> revisions,
            final ICortProgressMonitor ui) {
        final List<ICommit> ret = new ArrayList<>();
        for (final Pair<SvnWorkingCopy, SvnRepoRevision> e : revisions) {
            if (ui.isCanceled()) {
                throw BackgroundJobExecutor.createOperationCanceledException();
            }
            this.convertToCommitIfPossible(e.getFirst(), e.getSecond(), ret, ui);
        }
        return ret;
    }

    private void convertToCommitIfPossible(
            final SvnWorkingCopy wc,
            final SvnRevision e,
            final Collection<? super ICommit> result,
            final ICortProgressMonitor ui) {
        final List<? extends IChange> changes = this.determineChangesInCommit(wc, e, ui);
        if (!changes.isEmpty()) {
            result.add(ChangestructureFactory.createCommit(
                    wc,
                    e.toPrettyString(),
                    changes,
                    e.toRevision(),
                    e.getDate()));
        }
    }

    /**
     * Helpers class to account for the fact that SVN does not fill the copy path
     * for single files when the whole containing directory has been copied.
     */
    private static final class DirectoryCopyInfo {
        private final List<Pair<String, String>> directoryCopies = new ArrayList<>();

        public DirectoryCopyInfo(final Collection<CachedLogEntryPath> values) {
            for (final CachedLogEntryPath p : values) {
                if (p.isDir() && p.getCopyPath() != null) {
                    this.directoryCopies.add(Pair.create(p.getCopyPath(), p.getPath()));
                }
            }
        }

        private String determineOldPath(final CachedLogEntryPath entryInfo) {
            if (entryInfo.getCopyPath() != null) {
                return entryInfo.getCopyPath();
            }
            final String path = entryInfo.getPath();
            for (final Pair<String, String> dirCopy : this.directoryCopies) {
                if (path.startsWith(dirCopy.getSecond())) {
                    return dirCopy.getFirst() + path.substring(dirCopy.getSecond().length());
                }
            }
            return path;
        }

    }

    private List<? extends IChange> determineChangesInCommit(
            final SvnWorkingCopy wc,
            final SvnRevision e,
            final ICortProgressMonitor ui) {

        final List<IChange> ret = new ArrayList<>();
        final Map<String, CachedLogEntryPath> changedPaths = e.getChangedPaths();
        final DirectoryCopyInfo dirCopies = new DirectoryCopyInfo(changedPaths.values());
        final Set<String> copySources = this.determineCopySources(changedPaths.values(), dirCopies);
        final List<String> sortedPaths = new ArrayList<>(changedPaths.keySet());
        Collections.sort(sortedPaths);
        for (final String path : sortedPaths) {
            if (ui.isCanceled()) {
                throw BackgroundJobExecutor.createOperationCanceledException();
            }

            final CachedLogEntryPath value = changedPaths.get(path);
            if (!value.isFile()) {
                continue;
            }
            if (value.isDeleted() && copySources.contains(value.getPath())) {
                //Moves are contained twice, as a copy and a deletion. The deletion shall not result in a fragment.
                continue;
            }

            final IRevisionedFile fileInfo = ChangestructureFactory.createFileInRevision(path, e.toRevision());
            final IFileHistoryNode node = wc.getFileHistoryGraph().getNodeFor(fileInfo);
            if (node != null) {
                try {
                    ret.addAll(this.determineChangesInFile(wc, node));
                } catch (final Exception ex) {
                    Logger.error("An error occurred while computing changes for " + fileInfo.toString(), ex);
                }
            }
        }
        return ret;
    }

    private Set<String> determineCopySources(
            final Collection<CachedLogEntryPath> entries,
            final DirectoryCopyInfo dirMoves) {

        final Set<String> ret = new LinkedHashSet<>();

        for (final CachedLogEntryPath p : entries) {
            final String copyPath = dirMoves.determineOldPath(p);
            if (!copyPath.equals(p.getPath())) {
                ret.add(copyPath);
            }
        }

        return ret;
    }

    @Override
    public void clearCaches() {
        for (final SvnWorkingCopy wc : SvnWorkingCopyManager.getInstance().getWorkingCopies()) {
            wc.getRepository().clearCache();
        }
    }
}
