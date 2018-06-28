package de.setsoftware.reviewtool.changesources.core;

import java.util.List;

import de.setsoftware.reviewtool.model.api.IMutableFileHistoryGraph;
import de.setsoftware.reviewtool.model.api.IRepository;

/**
 * Interface to a {@link IRepository} which allows to access and modify various data associated with the repository.
 *
 * @param <ItemT> Type of a change item.
 * @param <CommitIdT> Type of a commit ID.
 * @param <CommitT> Type of a commit.
 * @param <RepoT> Type of the repository.
 */
public interface IScmRepository<
        ItemT extends IScmChangeItem,
        CommitIdT extends IScmCommitId<CommitIdT>,
        CommitT extends IScmCommit<ItemT, CommitIdT>,
        RepoT extends IScmRepository<ItemT, CommitIdT, CommitT, RepoT>> extends IRepository {

    /**
     * Returns a properly typed reference to this repository.
     */
    public abstract RepoT getThis();

    @Override
    public abstract IMutableFileHistoryGraph getFileHistoryGraph();

    /**
     * Sets the underlying file history graph. Used only while loading history data.
     *
     * @param fileHistoryGraph The file history graph to use.
     */
    public abstract void setFileHistoryGraph(IMutableFileHistoryGraph fileHistoryGraph);

    /**
     * Returns the commits stored in the history cache belonging to this repository.
     */
    public abstract List<CommitT> getCommits();

    /**
     * Appends new commits to the history cache belonging to this repository.
     * This is a purely local operation. The underlying SCM repository is not changed in any way.
     *
     * <p>Note that the caller is responsible to keep the history cache and the file history graph consistent.
     * Typically, a commit is added via this operation after it has been integrated into the file history graph.
     *
     * @param newCommits The commits to append.
     */
    public abstract void appendNewCommits(List<CommitT> newCommits);

    /**
     * Loads the contents of some file in some commit of this repository.
     *
     * @param path The file path.
     * @param commitId The commit ID.
     * @return The file contents as a byte array.
     */
    public abstract byte[] loadContents(String path, CommitIdT commitId) throws ScmException;
}
