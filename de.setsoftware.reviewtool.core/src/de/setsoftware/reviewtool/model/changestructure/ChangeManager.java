package de.setsoftware.reviewtool.model.changestructure;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;

import de.setsoftware.reviewtool.base.WeakListeners;
import de.setsoftware.reviewtool.model.api.IChangeData;
import de.setsoftware.reviewtool.model.api.IFileHistoryGraph;
import de.setsoftware.reviewtool.model.api.IFileHistoryNode;
import de.setsoftware.reviewtool.model.api.IRevisionedFile;

/**
 * Helps to manage changes used while reviewing or fixing code.
 */
public final class ChangeManager {

    private final VirtualFileHistoryGraph historyGraph;
    private final IChangeData remoteChanges;
    private Map<File, IRevisionedFile> relevantLocalFiles = new LinkedHashMap<>();
    private final WeakListeners<IChangeManagerListener> changeManagerListeners = new WeakListeners<>();

    /**
     * Constructor.
     *
     * @param remoteChanges The remote repository changes.
     */
    public ChangeManager(final IChangeData remoteChanges) {
        this.historyGraph = new VirtualFileHistoryGraph(remoteChanges.getHistoryGraph());
        this.remoteChanges = remoteChanges;
    }

    /**
     * Default constructor without remote changes (only for tests).
     */
    public ChangeManager() {
        this.historyGraph = new VirtualFileHistoryGraph();
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
     * @return The underlying {@link IFileHistoryGraph}.
     */
    public IFileHistoryGraph getHistoryGraph() {
        return this.historyGraph;
    }

    /**
     * Returns a {@link IFileHistoryNode} for passed {@link IRevisionedFile}.
     *
     * @param file The file whose change history to retrieve.
     * @return The {@link IFileHistoryNode} describing changes for passed {@link IRevisionedFile} or null if not found.
     */
    public IFileHistoryNode getFileHistoryNode(final IRevisionedFile file) {
        return this.historyGraph.getNodeFor(file);
    }

    /**
     * Returns a {@link IFileHistoryNode} for passed {@link File}.
     *
     * @param filePath The file whose change history to retrieve.
     * @param progressMonitor The progress monitor to use.
     * @return The {@link IFileHistoryNode} describing changes for passed {@link File} or null if not found.
     */
    public IFileHistoryNode getFileHistoryNode(final File filePath, final IProgressMonitor progressMonitor) {
        this.analyzeLocalChanges(Collections.<File> singletonList(filePath), progressMonitor);
        final IRevisionedFile file = this.relevantLocalFiles.get(filePath);
        return file == null ? null : this.getFileHistoryNode(file);
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

        if (this.historyGraph.size() > 1) {
            this.historyGraph.remove(this.historyGraph.size() - 1);
        }
        this.historyGraph.add(localChanges.getHistoryGraph());

        for (final IChangeManagerListener listener: this.changeManagerListeners) {
            listener.localChangeInfoUpdated(this);
        }
    }
}
