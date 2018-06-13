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
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc.DefaultSVNAuthenticationManager;
import org.tmatesoft.svn.core.wc.ISVNStatusHandler;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNInfo;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNStatus;

import de.setsoftware.reviewtool.base.Pair;
import de.setsoftware.reviewtool.base.ReviewtoolException;
import de.setsoftware.reviewtool.model.api.IBinaryChange;
import de.setsoftware.reviewtool.model.api.IChange;
import de.setsoftware.reviewtool.model.api.IChangeData;
import de.setsoftware.reviewtool.model.api.IChangeSource;
import de.setsoftware.reviewtool.model.api.IChangeSourceUi;
import de.setsoftware.reviewtool.model.api.ICommit;
import de.setsoftware.reviewtool.model.api.IFileHistoryEdge;
import de.setsoftware.reviewtool.model.api.IFileHistoryNode;
import de.setsoftware.reviewtool.model.api.IHunk;
import de.setsoftware.reviewtool.model.api.IRevisionedFile;
import de.setsoftware.reviewtool.model.changestructure.ChangestructureFactory;

/**
 * A simple change source that loads the changes from subversion.
 */
final class SvnChangeSource implements IChangeSource {

    private static final String KEY_PLACEHOLDER = "${key}";

    private final Set<File> workingCopyRoots;
    private final String logMessagePattern;
    private final SVNClientManager mgr = SVNClientManager.newInstance();
    private final long maxTextDiffThreshold;

    SvnChangeSource(
            List<File> projectRoots,
            String logMessagePattern,
            String user,
            String pwd,
            long maxTextDiffThreshold,
            int logCacheMinSize) {
        this.mgr.setAuthenticationManager(new DefaultSVNAuthenticationManager(
                null, false, user, pwd.toCharArray(), null, null));
        this.workingCopyRoots = this.determineWorkingCopyRoots(projectRoots);

        this.logMessagePattern = logMessagePattern;
        //check that the pattern can be parsed
        this.createPatternForKey("TEST-123");
        this.maxTextDiffThreshold = maxTextDiffThreshold;
        SvnRepositoryManager.getInstance().init(this.mgr, logCacheMinSize);
        SvnWorkingCopyManager.getInstance().init(this.mgr, this.workingCopyRoots);
    }

    private Set<File> determineWorkingCopyRoots(List<File> projectRoots) {
        final LinkedHashSet<File> workingCopyRoots = new LinkedHashSet<>();
        for (final File projectRoot : projectRoots) {
            final File wcRoot = this.determineWorkingCopyRoot(projectRoot);
            if (wcRoot != null) {
                workingCopyRoots.add(wcRoot);
            }
        }
        return workingCopyRoots;
    }

    private File determineWorkingCopyRoot(File projectRoot) {
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

    private boolean isPotentialRoot(File next) {
        final File dotsvn = new File(next, ".svn");
        return dotsvn.isDirectory();
    }

    private Pattern createPatternForKey(String key) {
        return Pattern.compile(
                this.logMessagePattern.replace(KEY_PLACEHOLDER, Pattern.quote(key)),
                Pattern.DOTALL);
    }

    @Override
    public Collection<SvnRepo> getRepositories() {
        return Collections.unmodifiableCollection(SvnRepositoryManager.getInstance().getRepositories());
    }

    @Override
    public SvnRepo getRepositoryById(final String id) {
        for (final SvnRepo repo : SvnRepositoryManager.getInstance().getRepositories()) {
            if (repo.getId().equals(id)) {
                return repo;
            }
        }
        return null;
    }

    @Override
    public IChangeData getRepositoryChanges(String key, IChangeSourceUi ui) {
        try {
            ui.subTask("Determining relevant commits...");
            final List<Pair<SvnWorkingCopy, SvnRepoRevision>> revisions = this.determineRelevantRevisions(key, ui);
            final Map<SvnRepo, Long> neededRevisionPerRepo = this.determineMaxRevisionPerRepo(revisions);
            ui.subTask("Checking state of working copy...");
            this.checkWorkingCopiesUpToDate(neededRevisionPerRepo, ui);
            ui.subTask("Analyzing commits...");
            final List<ICommit> commits = this.convertRepoRevisionsToChanges(revisions, ui);
            return ChangestructureFactory.createChangeData(this, commits);
        } catch (final SVNException e) {
            throw new ReviewtoolException(e);
        }
    }

    @Override
    public IChangeData getLocalChanges(
            final IChangeData remoteChanges,
            final List<File> relevantPaths,
            final IProgressMonitor ui) {
        try {
            ui.subTask("Collecting local changes...");
            final List<SvnWorkingCopyRevision> revisions = this.collectWorkingCopyChanges(relevantPaths, ui);
            ui.subTask("Analyzing local changes...");
            final List<ICommit> commits = this.convertLocalRevisionsToChanges(revisions, ui);
            final Map<File, IRevisionedFile> localPathMap = this.extractLocalPaths(revisions);
            return ChangestructureFactory.createChangeData(this, commits, localPathMap);
        } catch (final SVNException e) {
            throw new ReviewtoolException(e);
        }
    }

    /**
     * Checks whether the working copy should be updated in order to incorporate remote changes.
     * @param neededRevisionPerRepo A map storing the last known revisions for each repository.
     */
    private void checkWorkingCopiesUpToDate(
            final Map<SvnRepo, Long> neededRevisionPerRepo,
            final IChangeSourceUi ui) throws SVNException {

        for (final SvnWorkingCopy wc : SvnWorkingCopyManager.getInstance().getWorkingCopies()) {
            if (ui.isCanceled()) {
                throw new OperationCanceledException();
            }

            final SvnRepo repo = wc.getRepository();
            if (neededRevisionPerRepo.containsKey(repo)) {
                final long remoteRev = neededRevisionPerRepo.get(repo);
                final File wcRoot = wc.getLocalRoot();
                final long wcRev = this.mgr.getStatusClient().doStatus(wcRoot, false).getRevision().getNumber();
                if (wcRev < remoteRev) {
                    final Boolean doUpdate = ui.handleLocalWorkingCopyOutOfDate(wc.toString());
                    if (doUpdate == null) {
                        throw new OperationCanceledException();
                    }
                    if (doUpdate) {
                        this.mgr.getUpdateClient().doUpdate(wcRoot, SVNRevision.HEAD, SVNDepth.INFINITY, true, false);
                    }
                }
            }
        }
    }

    /**
     * Collects all local changes and integrates them into the {@link SvnFileHistoryGraph}.
     * @param relevantPaths The list of paths to check. If {@code null}, the whole working copy is analyzed.
     * @return A list of {@link SvnWorkingCopyRevision}s. May be empty if no relevant local changes have been found.
     */
    private List<SvnWorkingCopyRevision> collectWorkingCopyChanges(
            final List<File> relevantPaths,
            final IProgressMonitor ui) throws SVNException {

        if (relevantPaths != null) {
            return this.collectWorkingCopyChangesByPath(relevantPaths, ui);
        } else {
            return this.collectWorkingCopyChangesByRepository(ui);
        }
    }

    private List<SvnWorkingCopyRevision> collectWorkingCopyChangesByRepository(
            final IProgressMonitor ui) throws SVNException {

        final List<SvnWorkingCopyRevision> revisions = new ArrayList<>();
        for (final SvnWorkingCopy wc : SvnWorkingCopyManager.getInstance().getWorkingCopies()) {
            if (ui.isCanceled()) {
                throw new OperationCanceledException();
            }
            final File wcRoot = wc.getLocalRoot();
            final SortedMap<String, CachedLogEntryPath> paths = new TreeMap<>();
            this.mgr.getStatusClient().doStatus(
                    wcRoot,
                    SVNRevision.WORKING,
                    SVNDepth.INFINITY,
                    false, /* no remote */
                    false, /* report only modified paths */
                    false, /* don't include ignored files */
                    false, /* ignored */
                    new ISVNStatusHandler() {
                        @Override
                        public void handleStatus(final SVNStatus status) throws SVNException {
                            if (status.isVersioned()) {
                                final CachedLogEntryPath entry = new CachedLogEntryPath(wc.getRepository(), status);
                                paths.put(entry.getPath(), entry);
                            }
                        }
                    },
                    null); /* no change lists */

            wc.clearLocalFileHistoryGraph();
            final SvnWorkingCopyRevision wcRevision = new SvnWorkingCopyRevision(wc, paths);
            wc.getLocalFileHistoryGraph().processRevision(wcRevision);
            revisions.add(wcRevision);
        }

        return revisions;
    }

    private List<SvnWorkingCopyRevision> collectWorkingCopyChangesByPath(
            final List<File> relevantPaths,
            final IProgressMonitor ui) throws SVNException {

        final Map<SvnWorkingCopy, SortedMap<String, CachedLogEntryPath>> changeMap = new LinkedHashMap<>();
        final List<SvnWorkingCopyRevision> revisions = new ArrayList<>();

        for (final File wcPath : relevantPaths) {
            if (ui.isCanceled()) {
                throw new OperationCanceledException();
            }
            this.checkLocalFile(changeMap, wcPath);
        }

        for (final Map.Entry<SvnWorkingCopy, SortedMap<String, CachedLogEntryPath>> entry : changeMap.entrySet()) {
            final SvnWorkingCopy wc = entry.getKey();
            wc.clearLocalFileHistoryGraph();
            final SvnWorkingCopyRevision wcRevision = new SvnWorkingCopyRevision(wc, entry.getValue());
            wc.getLocalFileHistoryGraph().processRevision(wcRevision);
            revisions.add(wcRevision);
        }

        return revisions;
    }

    private void checkLocalFile(
            final Map<SvnWorkingCopy, SortedMap<String, CachedLogEntryPath>> changeMap,
            final File wcPath) throws SVNException {

        final SVNInfo info = this.mgr.getWCClient().doInfo(wcPath, SVNRevision.WORKING);
        final File wcRoot = info.getWorkingCopyRoot();
        final SvnWorkingCopy wc = SvnWorkingCopyManager.getInstance().getWorkingCopy(wcRoot);
        if (!changeMap.containsKey(wc)) {
            changeMap.put(wc, new TreeMap<String, CachedLogEntryPath>());
        }
        final SortedMap<String, CachedLogEntryPath> paths = changeMap.get(wc);

        this.mgr.getStatusClient().doStatus(
                wcPath,
                SVNRevision.WORKING,
                SVNDepth.INFINITY,
                false, /* no remote */
                true,  /* report also unmodified files */
                false, /* don't include ignored files */
                false, /* ignored */
                new ISVNStatusHandler() {
                    @Override
                    public void handleStatus(final SVNStatus status) throws SVNException {
                        if (status.isVersioned()) {
                            final CachedLogEntryPath entry = new CachedLogEntryPath(wc.getRepository(), status);
                            paths.put(entry.getPath(), entry);
                        }
                    }
                },
                null); /* no change lists */
    }

    private Map<File, IRevisionedFile> extractLocalPaths(final Collection<SvnWorkingCopyRevision> revisions) {
        final Map<File, IRevisionedFile> result = new LinkedHashMap<>();
        for (final SvnWorkingCopyRevision revision : revisions) {
            for (final CachedLogEntryPath path : revision.getChangedPaths().values()) {
                final File localPath = path.getLocalPath();
                if (localPath != null) {
                    result.put(
                            localPath,
                            ChangestructureFactory.createFileInRevision(path.getPath(), revision.toRevision()));
                }
            }
        }
        return result;
    }

    private Map<SvnRepo, Long> determineMaxRevisionPerRepo(List<Pair<SvnWorkingCopy, SvnRepoRevision>> revisions) {
        final Map<SvnRepo, Long> ret = new LinkedHashMap<>();
        for (final Pair<SvnWorkingCopy, SvnRepoRevision> p : revisions) {
            final SvnRepoRevision revision = p.getSecond();
            final SvnRepo repo = revision.getRepository();
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

        final List<Pair<SvnWorkingCopy, SvnRepoRevision>> result = new ArrayList<>();
        final Pattern pattern = this.createPatternForKey(key);
        final CachedLogLookupHandler handler = new CachedLogLookupHandler() {

            @Override
            public boolean handleLogEntry(final CachedLogEntry logEntry) throws SVNException {
                final String message = logEntry.getMessage();
                return message != null && pattern.matcher(message).matches();
            }
        };

        for (final File workingCopyRoot : this.workingCopyRoots) {
            if (ui.isCanceled()) {
                throw new OperationCanceledException();
            }
            result.addAll(SvnWorkingCopyManager.getInstance().traverseRecentEntries(workingCopyRoot, handler, ui));
        }
        return result;
    }

    private List<ICommit> convertRepoRevisionsToChanges(
            final List<Pair<SvnWorkingCopy, SvnRepoRevision>> revisions,
            final IProgressMonitor ui) {
        final List<ICommit> ret = new ArrayList<>();
        for (final Pair<SvnWorkingCopy, SvnRepoRevision> e : revisions) {
            if (ui.isCanceled()) {
                throw new OperationCanceledException();
            }
            this.convertToCommitIfPossible(e.getFirst(), e.getSecond(), ret, ui);
        }
        return ret;
    }

    private List<ICommit> convertLocalRevisionsToChanges(
            final List<SvnWorkingCopyRevision> revisions,
            final IProgressMonitor ui) {
        final List<ICommit> ret = new ArrayList<>();
        for (final SvnWorkingCopyRevision revision : revisions) {
            if (ui.isCanceled()) {
                throw new OperationCanceledException();
            }
            this.convertToCommitIfPossible(revision.getWorkingCopy(), revision, ret, ui);
        }
        return ret;
    }

    private void convertToCommitIfPossible(
            final SvnWorkingCopy wc,
            final SvnRevision e,
            final Collection<? super ICommit> result,
            final IProgressMonitor ui) {
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

        public DirectoryCopyInfo(Collection<CachedLogEntryPath> values) {
            for (final CachedLogEntryPath p : values) {
                if (p.isDir() && p.getCopyPath() != null) {
                    this.directoryCopies.add(Pair.create(p.getCopyPath(), p.getPath()));
                }
            }
        }

        private String determineOldPath(CachedLogEntryPath entryInfo) {
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
            final IProgressMonitor ui) {

        final List<IChange> ret = new ArrayList<>();
        final Map<String, CachedLogEntryPath> changedPaths = e.getChangedPaths();
        final DirectoryCopyInfo dirCopies = new DirectoryCopyInfo(changedPaths.values());
        final Set<String> copySources = this.determineCopySources(changedPaths.values(), dirCopies);
        final List<String> sortedPaths = new ArrayList<>(changedPaths.keySet());
        Collections.sort(sortedPaths);
        for (final String path : sortedPaths) {
            if (ui.isCanceled()) {
                throw new OperationCanceledException();
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
                    IStatus status = new Status(
                            IStatus.ERROR,
                            "CoRT",
                            "An error occurred while computing changes for " + fileInfo.toString(),
                            ex);
                    final Bundle bundle = FrameworkUtil.getBundle(this.getClass());
                    Platform.getLog(bundle).log(status);
                }
            }
        }
        return ret;
    }

    private IBinaryChange createBinaryChange(
            final SvnWorkingCopy wc,
            final IFileHistoryNode node,
            final IFileHistoryNode ancestor) {

        final IRevisionedFile oldFileInfo = ChangestructureFactory.createFileInRevision(
                ancestor.getFile().getPath(),
                ancestor.getFile().getRevision());

        return ChangestructureFactory.createBinaryChange(
                wc,
                oldFileInfo,
                node.getFile(),
                false);
    }

    private List<? extends IChange> determineChangesInFile(
            final SvnWorkingCopy wc,
            final IFileHistoryNode node) throws Exception {

        final byte[] newFileContents = node.getFile().getContents();
        final boolean newFileContentsUseTextualDiff = this.isUseTextualDiff(newFileContents);

        final List<IChange> changes = new ArrayList<>();
        for (final IFileHistoryEdge ancestorEdge : node.getAncestors()) {
            final IFileHistoryNode ancestor = ancestorEdge.getAncestor();

            final byte[] oldFileContents = ancestor.getFile().getContents();
            final boolean oldFileContentsUseTextualDiff = this.isUseTextualDiff(oldFileContents);

            if (oldFileContentsUseTextualDiff && newFileContentsUseTextualDiff) {
                final List<? extends IHunk> hunks = ancestorEdge.getDiff().getHunks();
                for (final IHunk hunk : hunks) {
                    changes.add(ChangestructureFactory.createTextualChangeHunk(
                            wc,
                            hunk.getSource(),
                            hunk.getTarget(),
                            false));
                }
            } else {
                changes.add(this.createBinaryChange(wc, node, ancestor));
            }
        }
        return changes;
    }

    private boolean isUseTextualDiff(final byte[] newFileContent) {
        return !contentLooksBinary(newFileContent) && newFileContent.length <= this.maxTextDiffThreshold;
    }

    private static boolean contentLooksBinary(byte[] fileContent) {
        if (fileContent.length == 0) {
            return false;
        }
        final int max = Math.min(128, fileContent.length);
        for (int i = 0; i < max; i++) {
            if (isStrangeChar(fileContent[i])) {
                //we only count ASCII control chars as "strange" (to be UTF-8 agnostic), so
                //  a single strange char should suffice to declare a file non-text
                return true;
            }
        }
        return false;
    }

    private static boolean isStrangeChar(byte b) {
        return b != '\n' && b != '\r' && b != '\t' && b < 0x20 && b >= 0;
    }

    private Set<String> determineCopySources(Collection<CachedLogEntryPath> entries, DirectoryCopyInfo dirMoves) {
        final Set<String> ret = new LinkedHashSet<>();

        for (final CachedLogEntryPath p : entries) {
            final String copyPath = dirMoves.determineOldPath(p);
            if (!copyPath.equals(p.getPath())) {
                ret.add(copyPath);
            }
        }

        return ret;
    }

}
