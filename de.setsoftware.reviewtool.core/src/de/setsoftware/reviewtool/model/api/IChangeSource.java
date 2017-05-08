package de.setsoftware.reviewtool.model.api;

import java.io.File;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Interface for strategies to determine the changes for a ticket, separated into commits.
 */
public interface IChangeSource {

    /**
     * Returns all repositories known to this change source.
     */
    public abstract Collection<? extends IRepository> getRepositories();

    /**
     * Returns a repository given its ID. If no repository with passed ID is known to this change source,
     * {@code null} ist returned.
     * @param id The repository ID.
     * @return The repository with passed ID or {@code null} if not found.
     */
    public abstract IRepository getRepositoryById(String id);

    /**
     * Returns all repository changes (that are relevant for the review tool) for the ticket with the given key.
     */
    public abstract IChangeData getRepositoryChanges(String key, IChangeSourceUi ui);

    /**
     * Returns all local changes (that are relevant for the review tool) in a new {@link IChangeData} object,
     * based on a {@link IChangeData} object returned earlier by {@link #getRepositoryChanges(String, IChangeSourceUi)}.
     *
     * @param relevantPaths The files to consider while searching for modifications. If {@code null},
     *      the whole working copy is considered.
     */
    public abstract IChangeData getLocalChanges(
            IChangeData remoteChanges,
            List<File> relevantPaths,
            IProgressMonitor ui);

}
