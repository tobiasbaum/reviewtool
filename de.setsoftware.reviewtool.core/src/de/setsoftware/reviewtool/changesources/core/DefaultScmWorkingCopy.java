package de.setsoftware.reviewtool.changesources.core;

import java.io.File;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import de.setsoftware.reviewtool.diffalgorithms.DiffAlgorithmFactory;
import de.setsoftware.reviewtool.model.api.IMutableFileHistoryGraph;
import de.setsoftware.reviewtool.model.changestructure.AbstractWorkingCopy;
import de.setsoftware.reviewtool.model.changestructure.FileHistoryGraph;
import de.setsoftware.reviewtool.model.changestructure.VirtualFileHistoryGraph;

/**
 * Default implementation of the {@link IScmWorkingCopy} interface.
 *
 * @param <ItemT> Type of a change item.
 * @param <CommitIdT> Type of a commit ID.
 * @param <CommitT> Type of a commit.
 * @param <RepoT> Type of the repository.
 * @param <LocalChangeT> Type of a local change.
 * @param <WcT> Type of the working copy.
 */
public abstract class DefaultScmWorkingCopy<
        ItemT extends IScmChangeItem,
        CommitIdT extends IScmCommitId<CommitIdT>,
        CommitT extends IScmCommit<ItemT, CommitIdT>,
        RepoT extends DefaultScmRepository<ItemT, CommitIdT, CommitT, RepoT>,
        LocalChangeT extends IScmLocalChange<ItemT>,
        WcT extends DefaultScmWorkingCopy<ItemT, CommitIdT, CommitT, RepoT, LocalChangeT, WcT>>
            extends AbstractWorkingCopy
            implements IScmWorkingCopy<ItemT, CommitIdT, CommitT, RepoT, LocalChangeT, WcT> {

    private final RepoT repo;
    private final IScmWorkingCopyBridge<ItemT, CommitIdT, CommitT, RepoT, LocalChangeT, WcT> scmBridge;
    private final File workingCopyRoot;
    private final String relPath;
    private final VirtualFileHistoryGraph combinedFileHistoryGraph;

    protected DefaultScmWorkingCopy(
            final RepoT repo,
            final IScmWorkingCopyBridge<ItemT, CommitIdT, CommitT, RepoT, LocalChangeT, WcT> scmBridge,
            final File workingCopyRoot,
            final String relPath) {
        this.repo = repo;
        this.scmBridge = scmBridge;
        this.workingCopyRoot = workingCopyRoot;
        this.relPath = relPath;
        this.combinedFileHistoryGraph = new VirtualFileHistoryGraph(repo.getFileHistoryGraph());
        this.combinedFileHistoryGraph.setLocalFileHistoryGraph(
                new FileHistoryGraph(DiffAlgorithmFactory.createDefault()));
    }

    @Override
    public final RepoT getRepository() {
        return this.repo;
    }

    @Override
    public final File getLocalRoot() {
        return this.workingCopyRoot;
    }

    @Override
    public final String getRelativePath() {
        return this.relPath;
    }

    @Override
    public final File toAbsolutePathInWc(final String absolutePathInRepo) {
        if (absolutePathInRepo.equals(this.relPath)) {
            return this.workingCopyRoot;
        } else if (absolutePathInRepo.startsWith(this.relPath + "/")) {
            assert !absolutePathInRepo.contains("\\");
            return new File(
                    this.workingCopyRoot,
                    absolutePathInRepo.substring(this.relPath.length() + 1));
        } else {
            return null;
        }
    }

    @Override
    public final String toAbsolutePathInRepo(final File absolutePathInWc) {
        final Path wcRootPath = this.workingCopyRoot.toPath();
        final Path wcPath = absolutePathInWc.toPath();
        if (wcPath.startsWith(wcRootPath)) {
            try {
                final String relativePath = wcRootPath.relativize(wcPath).toString().replaceAll("\\\\", "/");
                return this.relPath + '/' + relativePath;
            } catch (final IllegalArgumentException e) {
                return null;
            }
        } else {
            return null;
        }
    }

    @Override
    public final VirtualFileHistoryGraph getFileHistoryGraph() {
        return this.combinedFileHistoryGraph;
    }

    @Override
    public final String toString() {
        return this.workingCopyRoot.toString();
    }

    @Override
    public final void collectLocalChanges(final List<File> pathsToCheck) throws ScmException {
        final SortedMap<String, ItemT> changeMap = new TreeMap<>();
        final IScmChangeItemHandler<ItemT, Void> handler = new IScmChangeItemHandler<ItemT, Void>() {
            @Override
            public Void processChangeItem(final ItemT item) {
                changeMap.put(item.getPath(), item);
                return null;
            }
        };

        final Set<File> filteredPaths = pathsToCheck == null ? null : this.filterPaths(pathsToCheck);
        this.scmBridge.collectLocalChanges(this.getThis(), filteredPaths, handler);

        final IScmChange<ItemT> change = this.scmBridge.createLocalChange(this.getThis(), changeMap);
        final IMutableFileHistoryGraph localFileHistoryGraph =
                new FileHistoryGraph(DiffAlgorithmFactory.createDefault());
        change.integrateInto(localFileHistoryGraph);
        this.combinedFileHistoryGraph.setLocalFileHistoryGraph(localFileHistoryGraph);
    }

    @Override
    public final CommitIdT getIdOfLastCommit() throws ScmException {
        return this.scmBridge.getIdOfLastCommit(this.getThis());
    }

    @Override
    public final void update() throws ScmException {
        this.scmBridge.updateWorkingCopy(this.getThis());
    }

    /**
     * Filters out paths that do not belong to this working copy.
     *
     * @param relevantPaths The paths to filter.
     * @return A set of filtered paths.
     */
    private Set<File> filterPaths(final List<? extends File> relevantPaths) {
        final Set<File> paths = new LinkedHashSet<>();
        for (final File path : relevantPaths) {
            final String repoPath = this.toAbsolutePathInRepo(path);
            if (repoPath != null) {
                paths.add(path);
            }
        }

        for (final String repoPath : this.combinedFileHistoryGraph.getLocalFileHistoryGraph().getPaths()) {
            final File path = this.toAbsolutePathInWc(repoPath);
            if (path != null && path.isFile()) {
                paths.add(path);
            }
        }

        return paths;
    }
}
