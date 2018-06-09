package de.setsoftware.reviewtool.model.changestructure;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;

import de.setsoftware.reviewtool.base.WeakListeners;
import de.setsoftware.reviewtool.model.api.IChangeData;
import de.setsoftware.reviewtool.model.api.IRevisionedFile;

/**
 * Helps to manage changes used while reviewing or fixing code.
 */
public final class ChangeManager {

    private final IChangeData remoteChanges;
    private Map<File, IRevisionedFile> relevantLocalFiles = new LinkedHashMap<>();
    private final WeakListeners<IChangeManagerListener> changeManagerListeners = new WeakListeners<>();

    /**
     * Constructor.
     *
     * @param remoteChanges The remote repository changes.
     */
    public ChangeManager(final IChangeData remoteChanges) {
        this.remoteChanges = remoteChanges;
    }

    /**
     * Default constructor without remote changes (only for tests).
     */
    public ChangeManager() {
        this.remoteChanges = null;
    }

    /**
     * Adds a listener to be notified about updates.
     *
     * @param changeManagerListener The listener to add.
     */
    public void addListener(final IChangeManagerListener changeManagerListener) {
        this.changeManagerListeners.add(changeManagerListener);
    }

    /**
     * Analyzes local changes and combines them with the remote changes managed by this object.
     * Notifies listeners about the update.
     *
     * @param filesToAnalyze Files to analyze. If {@code null}, all local files are checked for local modifications.
     * @param progressMonitor The progress monitor to use.
     */
    public void analyzeLocalChanges(final List<File> filesToAnalyze, final IProgressMonitor progressMonitor) {
        assert this.remoteChanges != null;
        final IChangeData localChanges;
        if (filesToAnalyze == null) {
            localChanges = this.remoteChanges.getSource().getLocalChanges(this.remoteChanges, null,
                    progressMonitor);
        } else {
            final List<File> allFilesToAnalyze = new ArrayList<>(this.relevantLocalFiles.keySet());
            allFilesToAnalyze.addAll(filesToAnalyze);
            localChanges = this.remoteChanges.getSource().getLocalChanges(this.remoteChanges, allFilesToAnalyze,
                    progressMonitor);
        }
        this.relevantLocalFiles = new LinkedHashMap<>(localChanges.getLocalPathMap());

        for (final IChangeManagerListener listener: this.changeManagerListeners) {
            listener.localChangeInfoUpdated(this);
        }
    }
}
