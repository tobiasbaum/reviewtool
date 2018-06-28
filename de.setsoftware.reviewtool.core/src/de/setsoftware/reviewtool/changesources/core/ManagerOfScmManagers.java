package de.setsoftware.reviewtool.changesources.core;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Provides access to {@link IScmRepositoryManager}s and {@link IScmWorkingCopyManager}s, ordered by change source ID.
 */
public final class ManagerOfScmManagers {

    private static final ManagerOfScmManagers INSTANCE = new ManagerOfScmManagers();
    private final Map<String, IScmRepositoryManager<?, ?, ?, ?>> repositoryManagers;
    private final Map<String, IScmWorkingCopyManager<?, ?, ?, ?, ?, ?>> wcManagers;

    /**
     * Default constructor.
     */
    private ManagerOfScmManagers() {
        this.repositoryManagers = new LinkedHashMap<>();
        this.wcManagers = new LinkedHashMap<>();
    }

    /**
     * Returns the singleton instance of this class.
     */
    public static ManagerOfScmManagers getInstance() {
        return INSTANCE;
    }

    /**
     * Returns the repository manager for a given change source ID.
     *
     * @param changeSourceId The change source ID.
     * @return The associated repository manager or {@code null} if the change source ID is unknown.
     * @throws ClassCastException if the change source ID points to a repository manager of different type.
     */
    public synchronized <ItemT extends IScmChangeItem,
                         CommitIdT extends IScmCommitId<CommitIdT>,
                         CommitT extends IScmCommit<ItemT, CommitIdT>,
                         RepoT extends IScmRepository<ItemT, CommitIdT, CommitT, RepoT>>
            IScmRepositoryManager<ItemT, CommitIdT, CommitT, RepoT> getRepositoryManager(
                    final String changeSourceId) {

        @SuppressWarnings("unchecked")
        final IScmRepositoryManager<ItemT, CommitIdT, CommitT, RepoT> manager =
                (IScmRepositoryManager<ItemT, CommitIdT, CommitT, RepoT>)
                this.repositoryManagers.get(changeSourceId);
        return manager;
    }

    /**
     * Returns the working copy manager for a given change source ID.
     *
     * @param changeSourceId The change source ID.
     * @return The associated working copy manager or {@code null} if the change source ID is unknown.
     * @throws ClassCastException if the change source ID points to a working copy manager of different type.
     */
    public synchronized <ItemT extends IScmChangeItem,
                         CommitIdT extends IScmCommitId<CommitIdT>,
                         CommitT extends IScmCommit<ItemT, CommitIdT>,
                         RepoT extends IScmRepository<ItemT, CommitIdT, CommitT, RepoT>,
                         LocalChangeT extends IScmLocalChange<ItemT>,
                         WcT extends IScmWorkingCopy<ItemT, CommitIdT, CommitT, RepoT, LocalChangeT, WcT>>
            IScmWorkingCopyManager<ItemT, CommitIdT, CommitT, RepoT, LocalChangeT, WcT> getWorkingCopyManager(
                    final String changeSourceId) {

        @SuppressWarnings("unchecked")
        final IScmWorkingCopyManager<ItemT, CommitIdT, CommitT, RepoT, LocalChangeT, WcT> manager =
                (IScmWorkingCopyManager<ItemT, CommitIdT, CommitT, RepoT, LocalChangeT, WcT>)
                this.wcManagers.get(changeSourceId);
        return manager;
    }

    public synchronized void addRepositoryManager(
            final String changeSourceId,
            final IScmRepositoryManager<?, ?, ?, ?> manager) {
        this.repositoryManagers.put(changeSourceId, manager);
    }

    public synchronized void addWorkingCopyManager(
            final String changeSourceId,
            final IScmWorkingCopyManager<?, ?, ?, ?, ?, ?> manager) {
        this.wcManagers.put(changeSourceId, manager);
    }
}
