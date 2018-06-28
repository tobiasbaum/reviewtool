package de.setsoftware.reviewtool.changesources.core;

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

import de.setsoftware.reviewtool.base.Logger;
import de.setsoftware.reviewtool.base.Multimap;
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
import de.setsoftware.reviewtool.model.api.IHunk;
import de.setsoftware.reviewtool.model.api.IRepoRevision;
import de.setsoftware.reviewtool.model.api.IRevisionedFile;
import de.setsoftware.reviewtool.model.changestructure.ChangestructureFactory;

/**
 * Default implementation of the {@link IChangeSource} interface.
 *
 * @param <ItemT> Type of a change item.
 * @param <CommitIdT> Type of a commit ID.
 * @param <CommitT> Type of a commit.
 * @param <RepoT> Type of the repository.
 * @param <LocalChangeT> Type of a local change.
 * @param <WcT> Type of the working copy.
 * @param <RepoBridgeT> Type of the repository bridge.
 * @param <WcBridgeT> Type of the working copy bridge.
 */
public final class DefaultChangeSource<
        ItemT extends IScmChangeItem,
        CommitIdT extends IScmCommitId<CommitIdT>,
        CommitT extends IScmCommit<ItemT, CommitIdT>,
        RepoT extends DefaultScmRepository<ItemT, CommitIdT, CommitT, RepoT>,
        LocalChangeT extends IScmLocalChange<ItemT>,
        WcT extends DefaultScmWorkingCopy<ItemT, CommitIdT, CommitT, RepoT, LocalChangeT, WcT>,
        RepoBridgeT extends IScmRepositoryBridge<ItemT, CommitIdT, CommitT, RepoT>,
        WcBridgeT extends IScmWorkingCopyBridge<ItemT, CommitIdT, CommitT, RepoT, LocalChangeT, WcT>>
            implements IChangeSource {

    /**
     * Placeholder searched for in the log message pattern.
     */
    public static final String KEY_PLACEHOLDER = "${key}";

    private final RepoBridgeT scmRepoBridge;
    private final WcBridgeT scmWcBridge;
    private final String logMessagePattern;
    private final long maxTextDiffThreshold;
    private final Map<File, Set<File>> projectsPerWcMap;
    private final IScmRepositoryManager<ItemT, CommitIdT, CommitT, RepoT> repoManager;
    private final IScmWorkingCopyManager<ItemT, CommitIdT, CommitT, RepoT, LocalChangeT, WcT> wcManager;

    /**
     * Constructor.
     *
     * @param scmRepoBridge The {@link IScmRepositoryBridge} object to use.
     * @param scmWcBridge The {@link IScmWorkingCopyBridge} object to use.
     * @param logMessagePattern The message pattern for filtering commits.
     * @param maxTextDiffThreshold Files with a size beyond of this threshold are handled as binary.
     * @param logCacheMinSize The minimum number of commits to process given a new repository.
     */
    public DefaultChangeSource(
            final RepoBridgeT scmRepoBridge,
            final WcBridgeT scmWcBridge,
            final String logMessagePattern,
            final long maxTextDiffThreshold,
            final int logCacheMinSize) {

        this.scmRepoBridge = scmRepoBridge;
        this.scmWcBridge = scmWcBridge;
        this.logMessagePattern = logMessagePattern;
        this.maxTextDiffThreshold = maxTextDiffThreshold;
        this.projectsPerWcMap = new LinkedHashMap<>();
        this.repoManager = new DefaultScmRepositoryManager<>(this, this.scmRepoBridge, logCacheMinSize);
        this.wcManager = new DefaultScmWorkingCopyManager<>(this, this.repoManager, this.scmWcBridge);

        //check that the pattern can be parsed
        this.createPatternForKey("TEST-123");
    }

    private Pattern createPatternForKey(final String key) {
        return Pattern.compile(
                this.logMessagePattern.replace(KEY_PLACEHOLDER, Pattern.quote(key)),
                Pattern.DOTALL);
    }

    /**
     * Returns the associated {@link IScmRepositoryBridge}.
     */
    public RepoBridgeT getScmRepoBridge() {
        return this.scmRepoBridge;
    }

    /**
     * Returns the associated {@link IScmWorkingCopyBridge}.
     */
    public WcBridgeT getScmWcBridge() {
        return this.scmWcBridge;
    }

    @Override
    public String getId() {
        return this.getClass().getName();
    }

    @Override
    public Collection<RepoT> getRepositories() {
        return this.repoManager.getRepositories();
    }

    @Override
    public RepoT getRepositoryById(final String id) {
        for (final RepoT repo : this.repoManager.getRepositories()) {
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
            final Multimap<WcT, CommitT> revisions = this.determineRelevantRevisions(key, ui);
            ui.subTask("Checking state of working copy...");
            this.checkWorkingCopiesUpToDate(revisions, ui);
            ui.subTask("Analyzing commits...");
            final List<ICommit> commits = this.convertRepoRevisionsToChanges(revisions, ui);
            return ChangestructureFactory.createChangeData(this, commits);
        } catch (final ScmException e) {
            throw new ChangeSourceException(this, e);
        }
    }

    @Override
    public void analyzeLocalChanges(final List<File> relevantPaths) throws ChangeSourceException {
        try {
            this.wcManager.collectLocalChanges(relevantPaths);
        } catch (final ScmException e) {
            throw new ChangeSourceException(this, e);
        }
    }

    @Override
    public void addProject(final File projectRoot) throws ChangeSourceException {
        try {
            final WcT wc = this.wcManager.getWorkingCopy(projectRoot);
            if (wc != null) {
                final File wcRoot = wc.getLocalRoot();
                synchronized (this.projectsPerWcMap) {
                    Set<File> projects = this.projectsPerWcMap.get(wcRoot);
                    if (projects == null) {
                        projects = new LinkedHashSet<>();
                        this.projectsPerWcMap.put(wcRoot, projects);
                    }
                    projects.add(projectRoot);
                }
            }
        } catch (final ScmException e) {
            throw new ChangeSourceException(this, e);
        }
    }

    @Override
    public void removeProject(final File projectRoot) throws ChangeSourceException {
        try {
            final WcT wc = this.wcManager.getWorkingCopy(projectRoot);
            if (wc != null) {
                final File wcRoot = wc.getLocalRoot();
                synchronized (this.projectsPerWcMap) {
                    final Set<File> projects = this.projectsPerWcMap.get(wcRoot);
                    if (projects != null) {
                        projects.remove(projectRoot);
                        if (projects.isEmpty()) {
                            this.projectsPerWcMap.remove(wcRoot);
                            this.wcManager.removeWorkingCopy(wcRoot);
                        }
                    }
                }
            }
        } catch (final ScmException e) {
            throw new ChangeSourceException(this, e);
        }
    }

    /**
     * Checks whether the working copy should be updated in order to incorporate remote changes.
     */
    private void checkWorkingCopiesUpToDate(final Multimap<WcT, CommitT> commitsPerWc, final IChangeSourceUi ui)
            throws ScmException {

        for (final Map.Entry<WcT, List<CommitT>> entry : commitsPerWc.entrySet()) {
            if (ui.isCanceled()) {
                throw new OperationCanceledException();
            }

            final WcT wc = entry.getKey();
            final List<CommitT> commits = entry.getValue();
            if (commits.isEmpty()) {
                continue;
            }

            final CommitT lastCommit = commits.get(commits.size() - 1);
            if (!lastCommit.getId().le(wc.getIdOfLastCommit())) {
                final Boolean doUpdate = ui.handleLocalWorkingCopyOutOfDate(wc.toString());
                if (doUpdate == null) {
                    throw new OperationCanceledException();
                }
                if (doUpdate) {
                    wc.update();
                }
            }
        }
    }

    private Multimap<WcT, CommitT> determineRelevantRevisions(
            final String key,
            final IChangeSourceUi ui) throws ScmException {

        final Pattern pattern = this.createPatternForKey(key);
        final IScmCommitHandler<ItemT, CommitIdT, CommitT, Boolean> handler =
                new DefaultScmCommitFilter<>(pattern);

        return this.wcManager.filterCommits(handler, ui);
    }

    private List<ICommit> convertRepoRevisionsToChanges(
            final Multimap<WcT, CommitT> commitsPerWc,
            final IProgressMonitor ui) {

        final List<ICommit> ret = new ArrayList<>();
        for (final Map.Entry<WcT, List<CommitT>> entry : commitsPerWc.entrySet()) {
            for (final CommitT commit : entry.getValue()) {
                if (ui.isCanceled()) {
                    throw new OperationCanceledException();
                }
                final WcT wc = entry.getKey();
                this.convertToCommitIfPossible(wc, commit, ret, ui);
            }
        }
        return ret;
    }

    private void convertToCommitIfPossible(
            final WcT wc,
            final CommitT commit,
            final Collection<? super ICommit> result,
            final IProgressMonitor ui) {
        final List<? extends IChange> changes = this.determineChangesInCommit(wc, commit, ui);
        if (!changes.isEmpty()) {
            result.add(ChangestructureFactory.createCommit(
                    wc,
                    commit.toPrettyString(),
                    changes,
                    commit.toRevision(),
                    commit.getCommitDate()));
        }
    }

    private List<? extends IChange> determineChangesInCommit(
            final WcT wc,
            final CommitT e,
            final IProgressMonitor ui) {

        final IRepoRevision<CommitIdT> revision = e.toRevision();

        final List<IChange> ret = new ArrayList<>();
        final Map<String, ItemT> changedPaths = e.getChangedItems();
        final List<String> sortedPaths = new ArrayList<>(changedPaths.keySet());
        Collections.sort(sortedPaths);
        for (final String path : sortedPaths) {
            if (ui.isCanceled()) {
                throw new OperationCanceledException();
            }

            final IRevisionedFile fileInfo = ChangestructureFactory.createFileInRevision(path, revision);
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
            final WcT wc,
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

    private FileChangeType mapChangeType(final IFileHistoryNode.Type type) {
        switch (type) {
        case ADDED:
            return FileChangeType.ADDED;
        case DELETED:
            return FileChangeType.DELETED;
        case CHANGED:
        case REPLACED:
        case UNCONFIRMED:
        default:
            return FileChangeType.OTHER;
        }
    }

    private List<? extends IChange> determineChangesInFile(
            final WcT wc,
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
