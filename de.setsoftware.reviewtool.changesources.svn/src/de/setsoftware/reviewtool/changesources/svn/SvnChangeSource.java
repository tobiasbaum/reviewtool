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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc.DefaultSVNAuthenticationManager;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;

import de.setsoftware.reviewtool.base.Logger;
import de.setsoftware.reviewtool.base.Pair;
import de.setsoftware.reviewtool.model.api.ChangeSourceException;
import de.setsoftware.reviewtool.model.api.FileChangeType;
import de.setsoftware.reviewtool.model.api.IBinaryChange;
import de.setsoftware.reviewtool.model.api.IChange;
import de.setsoftware.reviewtool.model.api.IChangeData;
import de.setsoftware.reviewtool.model.api.IChangeSource;
import de.setsoftware.reviewtool.model.api.IChangeSourceUi;
import de.setsoftware.reviewtool.model.api.ICommit;
import de.setsoftware.reviewtool.model.api.IFileHistoryEdge;
import de.setsoftware.reviewtool.model.api.IFileHistoryNode;
import de.setsoftware.reviewtool.model.api.IFileHistoryNode.Type;
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
    public String getId() {
        return this.getClass().getName();
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
    public IChangeData getRepositoryChanges(final String key, final IChangeSourceUi ui) throws ChangeSourceException {
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

    private List<? extends IChange> determineChangesInCommit(
            final SvnWorkingCopy wc,
            final SvnRevision e,
            final IProgressMonitor ui) {

        final List<IChange> ret = new ArrayList<>();
        final Map<String, CachedLogEntryPath> changedPaths = e.getChangedPaths();
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

            final IRevisionedFile fileInfo = ChangestructureFactory.createFileInRevision(path, e.toRevision());
            final IFileHistoryNode node = wc.getFileHistoryGraph().getNodeFor(fileInfo);
            if (node != null) {
                if (node.getType().equals(IFileHistoryNode.Type.DELETED) && !node.getMoveTargets().isEmpty()) {
                    // Moves are contained twice, as a copy and a deletion. The deletion shall not result in a fragment.
                    continue;
                }
                try {
                    ret.addAll(this.determineChangesInFile(wc, node));
                } catch (final Exception ex) {
                    Logger.error("An error occurred while computing changes for " + fileInfo.toString(), ex);
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
                this.mapChangeType(node.getType()),
                oldFileInfo,
                node.getFile());
    }

    private FileChangeType mapChangeType(Type type) {
        switch (type) {
        case ADDED:
            return FileChangeType.ADDED;
        case DELETED:
            return FileChangeType.DELETED;
        default:
            return FileChangeType.OTHER;
        }
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
                            this.mapChangeType(node.getType()),
                            hunk.getSource(),
                            hunk.getTarget()));
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
}
