package de.setsoftware.reviewtool.model.api;

import java.io.File;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Interface for strategies to determine the changes for a ticket, separated into commits.
 */
public interface IChangeSource extends IRepositoryProvider {

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

    /**
     * Notifies the change source that a project has been added.
     *
     * @param projectRoot The root directory of the project.
     */
    public abstract void addProject(final File projectRoot);

    /**
     * Notifies the change source that a project has been removed.
     *
     * @param projectRoot The root directory of the project.
     */
    public abstract void removeProject(final File projectRoot);
}
