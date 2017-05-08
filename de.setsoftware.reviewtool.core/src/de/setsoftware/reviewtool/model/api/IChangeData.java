package de.setsoftware.reviewtool.model.api;

import java.io.File;
import java.util.Collection;
import java.util.List;

/**
 * Interface for the results from getting the relevant changes for a ticket from the ticket system.
 */
public interface IChangeData {

    /**
     * Returns the {@link IChangeSource} that created this object.
     */
    public abstract IChangeSource getSource();

    /**
     * Returns all repositories that were matching for the given ticket.
     */
    public abstract Collection<? extends IRepository> getRepositories();

    /**
     * Returns all {@link ICommit}s that were matching for the given ticket.
     */
    public abstract List<? extends ICommit> getMatchedCommits();

    /**
     * Returns the paths for all locally modified files. Only meaningful in results of
     * {@link IChangeSource#getLocalChanges(IChangeData, List, org.eclipse.core.runtime.IProgressMonitor)}.
     */
    public abstract List<File> getLocalPaths();

    /**
     * Returns a {@link IFileHistoryGraph} for the change data.
     */
    public abstract IFileHistoryGraph getHistoryGraph();

}
