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

    private final Map<File, Set<File>> projectsPerWcMap;
    private final String logMessagePattern;
    private final SVNClientManager mgr = SVNClientManager.newInstance();
    private final long maxTextDiffThreshold;

    SvnChangeSource(
            final String logMessagePattern,
            final String user,
            final String pwd,
            final long maxTextDiffThreshold,
            final int logCacheMinSize) {
        this.mgr.setAuthenticationManager(new DefaultSVNAuthenticationManager(
                null, false, user, pwd.toCharArray(), null, null));

        this.projectsPerWcMap = new LinkedHashMap<>();
        this.logMessagePattern = logMessagePattern;
        //check that the pattern can be parsed
        this.createPatternForKey("TEST-123");
        this.maxTextDiffThreshold = maxTextDiffThreshold;
        SvnRepositoryManager.getInstance().init(this.mgr, logCacheMinSize);
        SvnWorkingCopyManager.getInstance().init(this.mgr);
    }

    private File determineWorkingCopyRoot(final File projectRoot) {
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

    private Pattern createPatternForKey(final String key) {
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
    public IChangeData getRepositoryChanges(final String key, final IChangeSourceUi ui) {
        try {
            ui.subTask("Determining relevant commits...");
            final List<Pair<SvnWorkingCopy, SvnRepoRevision>> revisions = this.determineRelevantRevisions(key, ui);
            final Map<ISvnRepo, Long> neededRevisionPerRepo = this.determineMaxRevisionPerRepo(revisions);
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
    public void analyzeLocalChanges(final List<File> relevantPaths) {
        try {
            this.collectWorkingCopyChanges(relevantPaths);
        } catch (final SVNException e) {
            throw new ReviewtoolException(e);
        }
    }

    @Override
    public void addProject(final File projectRoot) {
        final File wcRoot = this.determineWorkingCopyRoot(projectRoot);
        if (wcRoot != null) {
            boolean wcCreated = false;
            synchronized (this.projectsPerWcMap) {
                Set<File> projects = this.projectsPerWcMap.get(wcRoot);
                if (projects == null) {
                    projects = new LinkedHashSet<>();
                    this.projectsPerWcMap.put(wcRoot, projects);
                    wcCreated = true;
                }
                projects.add(projectRoot);
            }

            if (wcCreated) {
                SvnWorkingCopyManager.getInstance().getWorkingCopy(wcRoot);
            }
        }
    }

    @Override
    public void removeProject(final File projectRoot) {
        final File wcRoot = this.determineWorkingCopyRoot(projectRoot);
        if (wcRoot != null) {
            boolean wcHasProjects = true;
            synchronized (this.projectsPerWcMap) {
                final Set<File> projects = this.projectsPerWcMap.get(wcRoot);
                if (projects != null) {
                    projects.remove(projectRoot);
                    if (projects.isEmpty()) {
                        this.projectsPerWcMap.remove(wcRoot);
                        wcHasProjects = false;
                    }
                }
            }

            if (!wcHasProjects) {
                SvnWorkingCopyManager.getInstance().removeWorkingCopy(wcRoot);
            }
        }
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
                throw new OperationCanceledException();
            }

            final ISvnRepo repo = wc.getRepository();
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
     * @param relevantPaths The list of additional paths to check. If {@code null}, the whole working copy is analyzed.
     */
    private void collectWorkingCopyChanges(final List<File> relevantPaths) throws SVNException {
        for (final SvnWorkingCopy wc : SvnWorkingCopyManager.getInstance().getWorkingCopies()) {
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
            final SvnFileHistoryGraph localFileHistoryGraph = new SvnFileHistoryGraph();
            localFileHistoryGraph.processRevision(wcRevision);
            wc.setLocalFileHistoryGraph(localFileHistoryGraph);
        }
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
    private Set<File> filterPaths(final List<File> relevantPaths, final SvnWorkingCopy wc) {
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
                    final IStatus status = new Status(
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

    private static boolean contentLooksBinary(final byte[] fileContent) {
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

    private static boolean isStrangeChar(final byte b) {
        return b != '\n' && b != '\r' && b != '\t' && b < 0x20 && b >= 0;
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

}
