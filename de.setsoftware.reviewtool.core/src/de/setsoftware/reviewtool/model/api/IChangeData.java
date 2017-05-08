package de.setsoftware.reviewtool.model.api;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Interface for the results from getting the relevant changes for a ticket from the ticket system.
 */
public interface IChangeData {

    /**
     * Returns the {@link IChangeSource} that created this object.
     */
    public abstract IChangeSource getSource();

    /**
     * Returns all {@link ICommit}s that were matching for the given ticket.
     */
    public abstract List<? extends ICommit> getMatchedCommits();

    /**
     * Returns the paths for all locally modified files together with their repository path.
     * Only meaningful in results of
     * {@link IChangeSource#getLocalChanges(IChangeData, List, org.eclipse.core.runtime.IProgressMonitor)}.
     */
    public abstract Map<File, IRevisionedFile> getLocalPathMap();

    /**
     * Returns a {@link IFileHistoryGraph} for the change data.
     */
    public abstract IFileHistoryGraph getHistoryGraph();

}
