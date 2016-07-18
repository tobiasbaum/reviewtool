package de.setsoftware.reviewtool.viewtracking;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.FileStoreEditorInput;

import de.setsoftware.reviewtool.telemetry.Telemetry;

/**
 * Tracks which parts of the code have been viewed by the user.
 * This information can then be used to determine which stops have been visited
 * (at all and for how long).
 */
public class CodeViewTracker {

    private static final int CHECK_INTERVAL = 500;

    /**
     * Number of milliseconds that have to elapse before no change in activity state is
     * considered "possible inactivity". Is chosen rather, further postprocessing of the
     * telemetry data can use a higher bound.
     */
    private static final long INACTIVITY_THRESHOLD = 60 * 1000L;

    private final IWorkbench workbench;
    private final ViewStatistics statistics;

    private volatile boolean running;

    private long timeOfLastNoticedActivity;
    private boolean sentInactivity;
    private ActivityState lastActivityState;

    /**
     * Encapsulates the state that is used to check for inactivity.
     */
    private static final class ActivityState {
        private final List<File> activeFiles = new ArrayList<>();
        private final List<ISelection> selectionsInFiles = new ArrayList<>();
        private final Point mousePosition;

        private ActivityState(List<IEditorPart> activeEditors) {
            for (final IEditorPart editor : activeEditors) {
                this.activeFiles.add(determineFilePath(editor));
                this.selectionsInFiles.add(this.getSelection(editor));
            }
            this.mousePosition = Display.getCurrent().getCursorLocation();
        }

        private ISelection getSelection(final IEditorPart editor) {
            final IEditorSite site = editor.getEditorSite();
            if (site == null) {
                return null;
            }
            final ISelectionProvider provider = site.getSelectionProvider();
            if (provider == null) {
                return null;
            }
            return provider.getSelection();
        }

        public static ActivityState create(List<IEditorPart> activeEditors) {
            return new ActivityState(activeEditors);
        }

        public List<File> getActiveFiles() {
            return this.activeFiles;
        }

        @Override
        public int hashCode() {
            return this.activeFiles.hashCode()
                    + 31 * this.mousePosition.hashCode()
                    + 234 * this.selectionsInFiles.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ActivityState)) {
                return false;
            }
            final ActivityState other = (ActivityState) obj;
            return this.mousePosition.equals(other.mousePosition)
                && this.activeFiles.equals(other.activeFiles)
                && this.selectionsInFiles.equals(other.selectionsInFiles);
        }

    }

    public CodeViewTracker() {
        this.workbench = PlatformUI.getWorkbench();
        this.statistics = new ViewStatistics();
        this.sentInactivity = true;
    }

    private void performSnapshot() {
        for (final IEditorPart activeEditor : this.getActiveEditorParts()) {
            final Object target = activeEditor.getAdapter(ITextOperationTarget.class);
            final File activeFilePath = determineFilePath(activeEditor);
            if (activeFilePath == null) {
                continue;
            }
            if (target instanceof ITextViewer) {
                final ITextViewer textViewer = (ITextViewer) target;
                this.statistics.mark(activeFilePath,
                        textViewer.getTopIndex() + 1, textViewer.getBottomIndex() + 1);
            } else {
                this.statistics.markUnknownPosition(activeFilePath);
            }
        }
    }

    private List<IEditorPart> getActiveEditorParts() {
        final List<IEditorPart> ret = new ArrayList<>();
        for (final IWorkbenchWindow window : this.workbench.getWorkbenchWindows()) {
            final IWorkbenchPage activePage = window.getActivePage();
            if (activePage == null) {
                continue;
            }
            final IEditorPart activeEditor = activePage.getActiveEditor();
            if (activeEditor == null) {
                continue;
            }
            ret.add(activeEditor);
        }
        return ret;
    }

    private void trackInactivityAndFileChanges() {
        final ActivityState newActivityState = ActivityState.create(this.getActiveEditorParts());
        final long curTime = System.currentTimeMillis();
        if (newActivityState.equals(this.lastActivityState)) {
            final long timeWithoutChange = curTime - this.timeOfLastNoticedActivity;
            if (timeWithoutChange > INACTIVITY_THRESHOLD && !this.sentInactivity) {
                //no change long enough => send an event and move to inactivity
                Telemetry.get().possibleInactivity(timeWithoutChange);
                this.sentInactivity = true;
            }
        } else {
            if (this.sentInactivity) {
                //coming back from inactivity => send an event
                Telemetry.get().activeFilesChanged(newActivityState.getActiveFiles());
                this.sentInactivity = false;
            } else if (!newActivityState.getActiveFiles().equals(this.lastActivityState.getActiveFiles())) {
                //change in active files => send an event
                Telemetry.get().activeFilesChanged(newActivityState.getActiveFiles());
            }
            this.lastActivityState = newActivityState;
            this.timeOfLastNoticedActivity = curTime;
        }
    }

    private static File determineFilePath(IEditorPart activeEditor) {
        final Object file = activeEditor.getEditorInput().getAdapter(IFile.class);
        if (file != null) {
            final IPath path = ((IFile) file).getLocation();
            if (path == null) {
                return null;
            }
            return path.toFile();
        }
        final Object externalFile = activeEditor.getEditorInput().getAdapter(FileStoreEditorInput.class);
        if (externalFile != null) {
            return new File(((FileStoreEditorInput) externalFile).getURI());
        }
        return null;
    }

    public void start() {
        this.running = true;
        this.workbench.getDisplay().timerExec(CHECK_INTERVAL, this.createSnapshotRunnable());
    }

    private Runnable createSnapshotRunnable() {
        return new Runnable() {
            @Override
            public void run() {
                if (!CodeViewTracker.this.running) {
                    return;
                }
                try {
                    CodeViewTracker.this.performSnapshot();
                    CodeViewTracker.this.trackInactivityAndFileChanges();
                } finally {
                    CodeViewTracker.this.workbench.getDisplay().timerExec(CHECK_INTERVAL, this);
                }
            }
        };
    }

    public void stop() {
        this.running = false;
    }

    public ViewStatistics getStatistics() {
        return this.statistics;
    }

}
