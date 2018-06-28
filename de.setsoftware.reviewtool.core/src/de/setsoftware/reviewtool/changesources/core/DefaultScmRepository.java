package de.setsoftware.reviewtool.changesources.core;

import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.setsoftware.reviewtool.diffalgorithms.DiffAlgorithmFactory;
import de.setsoftware.reviewtool.model.api.IChangeSource;
import de.setsoftware.reviewtool.model.api.IMutableFileHistoryGraph;
import de.setsoftware.reviewtool.model.api.IRepoRevision;
import de.setsoftware.reviewtool.model.changestructure.AbstractRepository;
import de.setsoftware.reviewtool.model.changestructure.FileHistoryGraph;

/**
 * Default implementation of the {@link IScmRepository} interface.
 *
 * @param <ItemT> Type of a change item.
 * @param <CommitIdT> Type of a commit ID.
 * @param <CommitT> Type of a commit.
 * @param <RepoT> Type of the repository.
 */
public abstract class DefaultScmRepository<
        ItemT extends IScmChangeItem,
        CommitIdT extends IScmCommitId<CommitIdT>,
        CommitT extends IScmCommit<ItemT, CommitIdT>,
        RepoT extends DefaultScmRepository<ItemT, CommitIdT, CommitT, RepoT>>
            extends AbstractRepository
            implements IScmRepository<ItemT, CommitIdT, CommitT, RepoT> {

    /**
     * References a repository by its ID.
     *
     * @param <CommitIdT> Type of a commit ID.
     * @param <ItemT> Type of a change item.
     * @param <CommitT> Type of a commit.
     * @param <RepoT> Type of the repository.
     */
    private static final class RepositoryRef<
            CommitIdT extends IScmCommitId<CommitIdT>,
            ItemT extends IScmChangeItem,
            CommitT extends IScmCommit<ItemT, CommitIdT>,
            RepoT extends DefaultScmRepository<ItemT, CommitIdT, CommitT, RepoT>> implements Serializable {

        private static final long serialVersionUID = 6104011101986343069L;
        private final String changeSourceId;
        private final String id;

        /**
         * Constructor.
         * @param id The ID of the repository.
         * @param changeSourceId The ID of the change source.
         */
        RepositoryRef(final String changeSourceId, final String id) {
            this.changeSourceId = changeSourceId;
            this.id = id;
        }

        private Object readResolve() throws ObjectStreamException {
            final IScmRepositoryManager<ItemT, CommitIdT, CommitT, RepoT> manager =
                        ManagerOfScmManagers.getInstance().getRepositoryManager(this.changeSourceId);
            final RepoT repo = manager == null ? null : manager.getRepo(this.id);
            if (repo == null) {
                throw new InvalidObjectException(
                        "No repository found at " + this.id + " for change source " + this.changeSourceId);
            }
            return repo;
        }
    }

    private static final long serialVersionUID = 5953641588342069586L;
    private final IChangeSource changeSource;
    private final String id;
    private final IScmRepositoryBridge<ItemT, CommitIdT, CommitT, RepoT> scmBridge;
    private final FileCache<ItemT, CommitIdT, CommitT, RepoT> fileCache;
    private final List<CommitT> commits;
    private IMutableFileHistoryGraph fileHistoryGraph;

    /**
     * Constructor.
     *
     * @param changeSource The change source.
     * @param id The repository ID.
     * @param scmBridge The SCM bridge.
     */
    protected DefaultScmRepository(
            final IChangeSource changeSource,
            final String id,
            final IScmRepositoryBridge<ItemT, CommitIdT, CommitT, RepoT> scmBridge) {

        this.changeSource = changeSource;
        this.id = id;
        this.scmBridge = scmBridge;
        this.fileCache = new FileCache<>(this.getThis());
        this.commits = new ArrayList<>();
        this.fileHistoryGraph = new FileHistoryGraph(DiffAlgorithmFactory.createDefault());
    }

    @Override
    public final String getId() {
        return this.id;
    }

    @Override
    public final IRepoRevision<CommitIdT> toRevision(final String commitId) {
        try {
            final CommitIdT cId = this.scmBridge.createCommitIdFromString(commitId);
            return cId == null ? null : cId.toRevision(this);
        } catch (final ScmException e) {
            return null;
        }
    }

    @Override
    public final byte[] getFileContents(final String path, final IRepoRevision<?> revision) throws ScmException {
        @SuppressWarnings("unchecked")
        final CommitIdT commitId = (CommitIdT) revision.getId();
        return this.fileCache.getFileContents(path, commitId);
    }

    @Override
    public final IMutableFileHistoryGraph getFileHistoryGraph() {
        return this.fileHistoryGraph;
    }

    @Override
    public final void setFileHistoryGraph(final IMutableFileHistoryGraph fileHistoryGraph) {
        this.fileHistoryGraph = fileHistoryGraph;
    }

    @Override
    public final List<CommitT> getCommits() {
        return Collections.unmodifiableList(this.commits);
    }

    @Override
    public final void appendNewCommits(final List<CommitT> newCommits) {
        this.commits.addAll(newCommits);
    }

    @Override
    public final byte[] loadContents(final String path, final CommitIdT commitId) throws ScmException {
        return this.scmBridge.loadContents(this.getThis(), path, commitId);
    }

    @Override
    public final String toString() {
        return this.id;
    }

    protected Object writeReplace() {
        return new RepositoryRef<>(this.changeSource.getId(), this.id);
    }
}
