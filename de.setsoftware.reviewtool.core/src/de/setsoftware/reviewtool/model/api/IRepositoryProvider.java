package de.setsoftware.reviewtool.model.api;

import java.util.Collection;

/**
 * Provides access to repositories known to CoRT.
 */
public interface IRepositoryProvider {

    /**
     * Returns all repositories known to this {@link IRepositoryProvider}.
     */
    public abstract Collection<? extends IRepository> getRepositories();

    /**
     * Returns a repository given its ID. If no repository with passed ID is known to this provider,
     * {@code null} is returned.
     * @param id The repository ID.
     * @return The repository with passed ID or {@code null} if not found.
     */
    public abstract IRepository getRepositoryById(String id);

}
