package de.setsoftware.reviewtool.changesources.svn;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import de.setsoftware.reviewtool.base.ValueWrapper;
import de.setsoftware.reviewtool.model.api.IBinaryChange;
import de.setsoftware.reviewtool.model.api.IChange;
import de.setsoftware.reviewtool.model.api.IChangeData;
import de.setsoftware.reviewtool.model.api.IChangeSource;
import de.setsoftware.reviewtool.model.api.IChangeSourceUi;
import de.setsoftware.reviewtool.model.api.ICommit;
import de.setsoftware.reviewtool.model.api.IFileHistoryEdge;
import de.setsoftware.reviewtool.model.api.IFileHistoryNode;
import de.setsoftware.reviewtool.model.api.IHunk;
import de.setsoftware.reviewtool.model.api.IMutableFileHistoryGraph;
import de.setsoftware.reviewtool.model.api.IMutableFileHistoryNode;
import de.setsoftware.reviewtool.model.api.IRepository;
import de.setsoftware.reviewtool.model.api.IRevision;
import de.setsoftware.reviewtool.model.api.IRevisionedFile;
import de.setsoftware.reviewtool.model.changestructure.ChangestructureFactory;

/**
 * A simple change source that loads the changes from subversion.
 */
public class SvnChangeSource implements IChangeSource {

    private static final String KEY_PLACEHOLDER = "${key}";

    private final Set<File> workingCopyRoots;
    private final String logMessagePattern;
    private final SVNClientManager mgr = SVNClientManager.newInstance();
    private final long maxTextDiffThreshold;

    public SvnChangeSource(
            List<File> projectRoots,
            String logMessagePattern,
            String user,
            String pwd,
            long maxTextDiffThreshold,
            int logCacheMinSize,
            int logCacheMaxSize) {
        this.mgr.setAuthenticationManager(new DefaultSVNAuthenticationManager(
                null, false, user, pwd.toCharArray(), null, null));
        this.workingCopyRoots = this.determineWorkingCopyRoots(projectRoots);

        this.logMessagePattern = logMessagePattern;
        //check that the pattern can be parsed
        this.createPatternForKey("TEST-123");
        this.maxTextDiffThreshold = maxTextDiffThreshold;
        CachedLog.getInstance().setSizeLimits(logCacheMinSize, logCacheMaxSize);
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
        return Collections.unmodifiableCollection(CachedLog.getInstance().getRepositories());
    }

    @Override
    public SvnRepo getRepositoryById(final String id) {
        for (final SvnRepo repo : CachedLog.getInstance().getRepositories()) {
            if (repo.getId().equals(id)) {
                return repo;
            }
        }
        return null;
    }

    @Override
    public IChangeData getRepositoryChanges(String key, IChangeSourceUi ui) {
        try {
            final IMutableFileHistoryGraph historyGraph = new SvnFileHistoryGraph();
            ui.subTask("Determining relevant commits...");
            final List<ISvnRevision> revisions = this.determineRelevantRevisions(key, historyGraph, ui);
            final Map<SvnRepo, Long> neededRevisionPerRepo = this.determineMaxRevisionPerRepo(revisions);
            ui.subTask("Checking state of working copy...");
            this.checkWorkingCopiesUpToDate(neededRevisionPerRepo, ui);
            ui.subTask("Analyzing commits...");
            final List<ICommit> commits = this.convertToChanges(historyGraph, revisions, ui);
            return new SvnChangeData(
                    this,
                    commits,
                    Collections.<File, IRevisionedFile> emptyMap(),
                    historyGraph);
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
            final IMutableFileHistoryGraph historyGraph = new SvnFileHistoryGraph();
            ui.subTask("Collecting local changes...");
            final List<WorkingCopyRevision> revisions =
                    this.collectWorkingCopyChanges(relevantPaths, historyGraph, ui);
            ui.subTask("Analyzing local changes...");
            final List<ICommit> commits = this.convertToChanges(historyGraph, revisions, ui);
            final Map<File, IRevisionedFile> localPathMap = this.extractLocalPaths(revisions);
            return new SvnChangeData(this, commits, localPathMap, historyGraph);
        } catch (final SVNException e) {
            throw new ReviewtoolException(e);
        }
    }

    /**
     * Checks whether the working copy should be updated in order to incorporate remote changes.
     * @param revisions The list of revisions.
     */
    private void checkWorkingCopiesUpToDate(
            final Map<SvnRepo, Long> neededRevisionPerRepo,
            final IChangeSourceUi ui) throws SVNException {

        for (final Entry<SvnRepo, Long> e : neededRevisionPerRepo.entrySet()) {
            if (ui.isCanceled()) {
                throw new OperationCanceledException();
            }
            final SvnRepo repo = e.getKey();
            final File wc = repo.getLocalRoot();
            final long wcRev = this.mgr.getStatusClient().doStatus(wc, false).getRevision().getNumber();
            if (wcRev < e.getValue()) {
                final Boolean doUpdate = ui.handleLocalWorkingCopyOutOfDate(wc.toString());
                if (doUpdate == null) {
                    throw new OperationCanceledException();
                }
                if (doUpdate) {
                    this.mgr.getUpdateClient().doUpdate(wc, SVNRevision.HEAD, SVNDepth.INFINITY, true, false);
                }
            }
        }
    }

    /**
     * Collects all local changes and integrates them into the {@link IMutableFileHistoryGraph}.
     * @param repositories The list of relevant {@link IRepository Repositories}.
     * @param historyGraph The {@link IMutableFileHistoryGraph}. Local changes will be integrated using a
     *      {@link WorkingCopyRevision}.
     * @return A list of {@link WorkingCopyRevision}s. May be empty if no relevant local changes have been found.
     */
    private List<WorkingCopyRevision> collectWorkingCopyChanges(
            final List<File> relevantPaths,
            final IMutableFileHistoryGraph historyGraph,
            final IProgressMonitor ui) throws SVNException {

        if (relevantPaths != null) {
            return this.collectWorkingCopyChangesByPath(relevantPaths, historyGraph, ui);
        } else {
            return this.collectWorkingCopyChangesByRepository(historyGraph, ui);
        }
    }

    private List<WorkingCopyRevision> collectWorkingCopyChangesByRepository(
            final IMutableFileHistoryGraph historyGraph,
            final IProgressMonitor ui) throws SVNException {

        final List<WorkingCopyRevision> revisions = new ArrayList<>();
        for (final SvnRepo repo : this.getRepositories()) {
            if (ui.isCanceled()) {
                throw new OperationCanceledException();
            }
            final File wc = repo.getLocalRoot();
            final SortedMap<String, CachedLogEntryPath> paths = new TreeMap<>();
            this.mgr.getStatusClient().doStatus(
                    wc,
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
                                final CachedLogEntryPath entry = new CachedLogEntryPath(repo, status);
                                paths.put(entry.getPath(), entry);
                            }
                        }
                    },
                    null); /* no change lists */

            final WorkingCopyRevision wcRevision = new WorkingCopyRevision(repo, paths);
            if (RelevantRevisionLookupHandler.processRevision(wcRevision, historyGraph)) {
                revisions.add(wcRevision);
            }
        }

        return revisions;
    }

    private List<WorkingCopyRevision> collectWorkingCopyChangesByPath(
            final List<File> relevantPaths,
            final IMutableFileHistoryGraph historyGraph,
            final IProgressMonitor ui) throws SVNException {

        final Map<SvnRepo, SortedMap<String, CachedLogEntryPath>> changeMap = new LinkedHashMap<>();
        final List<WorkingCopyRevision> revisions = new ArrayList<>();

        for (final File wcPath : relevantPaths) {
            if (ui.isCanceled()) {
                throw new OperationCanceledException();
            }
            this.checkLocalFile(changeMap, wcPath);
        }

        for (final Map.Entry<SvnRepo, SortedMap<String, CachedLogEntryPath>> entry : changeMap.entrySet()) {
            final WorkingCopyRevision wcRevision = new WorkingCopyRevision(entry.getKey(), entry.getValue());
            if (RelevantRevisionLookupHandler.processRevision(wcRevision, historyGraph)) {
                revisions.add(wcRevision);
            }
        }

        return revisions;
    }

    private void checkLocalFile(
            final Map<SvnRepo, SortedMap<String, CachedLogEntryPath>> changeMap,
            final File wcPath) throws SVNException {

        final SVNInfo info = this.mgr.getWCClient().doInfo(wcPath, SVNRevision.WORKING);
        final File wcRoot = info.getWorkingCopyRoot();
        final SvnRepo svnRepo = CachedLog.getInstance().mapWorkingCopyRootToRepository(this.mgr, wcRoot);
        if (!changeMap.containsKey(svnRepo)) {
            changeMap.put(svnRepo, new TreeMap<String, CachedLogEntryPath>());
        }
        final SortedMap<String, CachedLogEntryPath> paths = changeMap.get(svnRepo);

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
                            final CachedLogEntryPath entry = new CachedLogEntryPath(svnRepo, status);
                            paths.put(entry.getPath(), entry);
                        }
                    }
                },
                null); /* no change lists */
    }

    private Map<File, IRevisionedFile> extractLocalPaths(final Collection<WorkingCopyRevision> revisions) {
        final Map<File, IRevisionedFile> result = new LinkedHashMap<>();
        for (final WorkingCopyRevision revision : revisions) {
            for (final CachedLogEntryPath path : revision.getChangedPaths().values()) {
                final File localPath = path.getLocalPath();
                if (localPath != null) {
                    result.put(
                            localPath,
                            ChangestructureFactory.createFileInRevision(path.getPath(), this.revision(revision)));
                }
            }
        }
        return result;
    }

    private Map<SvnRepo, Long> determineMaxRevisionPerRepo(
            List<ISvnRevision> revisions) {
        final Map<SvnRepo, Long> ret = new LinkedHashMap<>();
        for (final ISvnRevision p : revisions) {
            final SvnRepo repo = p.getRepository();
            final long curRev = p.getRevisionNumber();
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

    private List<ISvnRevision> determineRelevantRevisions(
            final String key,
            final IMutableFileHistoryGraph historyGraph,
            final IChangeSourceUi ui) throws SVNException {
        final RelevantRevisionLookupHandler handler = new RelevantRevisionLookupHandler(this.createPatternForKey(key));
        for (final File workingCopyRoot : this.workingCopyRoots) {
            if (ui.isCanceled()) {
                throw new OperationCanceledException();
            }
            CachedLog.getInstance().traverseRecentEntries(this.mgr, workingCopyRoot, handler, ui);
        }
        return handler.determineRelevantRevisions(historyGraph, ui);
    }

    private List<ICommit> convertToChanges(
            final IMutableFileHistoryGraph historyGraph,
            final List<? extends ISvnRevision> revisions,
            final IProgressMonitor ui) {
        final List<ICommit> ret = new ArrayList<>();
        for (final ISvnRevision e : revisions) {
            if (ui.isCanceled()) {
                throw new OperationCanceledException();
            }
            this.convertToCommitIfPossible(historyGraph, e, ret, ui);
        }
        return ret;
    }

    private void convertToCommitIfPossible(final IMutableFileHistoryGraph historyGraph, final ISvnRevision e,
            final Collection<? super ICommit> result, final IProgressMonitor ui) {
        final List<? extends IChange> changes = this.determineChangesInCommit(historyGraph, e, ui);
        if (!changes.isEmpty()) {
            result.add(ChangestructureFactory.createCommit(
                    e.toPrettyString(),
                    changes,
                    e.isVisible(),
                    this.revision(e),
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
            final IMutableFileHistoryGraph historyGraph,
            final ISvnRevision e,
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

            final IRevisionedFile fileInfo = ChangestructureFactory.createFileInRevision(path, this.revision(e));
            final IMutableFileHistoryNode node = historyGraph.getNodeFor(fileInfo);
            if (node != null) {
                try {
                    ret.addAll(this.determineChangesInFile(node, e.isVisible()));
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

    private IBinaryChange createBinaryChange(final IFileHistoryNode node, final IFileHistoryNode ancestor,
            final boolean isVisible) {

        final IRevisionedFile oldFileInfo = ChangestructureFactory.createFileInRevision(ancestor.getFile().getPath(),
                        ancestor.getFile().getRevision());

        return ChangestructureFactory.createBinaryChange(
                oldFileInfo,
                node.getFile(),
                false,
                isVisible);
    }

    private IRevision revision(final ISvnRevision revision) {
        final ValueWrapper<IRevision> result = new ValueWrapper<>();
        revision.accept(new ISvnRevisionVisitor() {

            @Override
            public void handle(WorkingCopyRevision revision) {
                result.setValue(ChangestructureFactory.createLocalRevision(revision.getRepository()));
            }

            @Override
            public void handle(SvnRevision revision) {
                result.setValue(ChangestructureFactory.createRepoRevision(
                        revision.getRevisionNumber(), revision.getRepository()));
            }
        });
        return result.get();
    }

    private List<? extends IChange> determineChangesInFile(
            final IMutableFileHistoryNode node,
            final boolean isVisible) throws Exception {

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
                            hunk.getSource(),
                            hunk.getTarget(),
                            false,
                            isVisible));
                }
            } else {
                changes.add(this.createBinaryChange(node, ancestor, isVisible));
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
