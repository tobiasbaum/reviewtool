package de.setsoftware.reviewtool.viewtracking;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.FileStoreEditorInput;

/**
 * Tracks which parts of the code have been viewed by the user.
 * This information can then be used to determine which stops have been visited
 * (at all and for how long).
 */
public class CodeViewTracker {

    private static final int CHECK_INTERVAL = 500;

    private final IWorkbench workbench;
    private final ViewStatistics statistics;

    private boolean running;

    public CodeViewTracker() {
        this.workbench = PlatformUI.getWorkbench();
        this.statistics = new ViewStatistics();
    }

    private void performSnapshot() {

        for (final IWorkbenchWindow window : this.workbench.getWorkbenchWindows()) {
            final IWorkbenchPage activePage = window.getActivePage();
            if (activePage == null) {
                continue;
            }
            final IEditorPart activeEditor = activePage.getActiveEditor();
            if (activeEditor == null) {
                continue;
            }
            final ITextOperationTarget target = (ITextOperationTarget)
                    activeEditor.getAdapter(ITextOperationTarget.class);

            if (target instanceof ITextViewer) {
                final ITextViewer textViewer = (ITextViewer) target;
                this.statistics.mark(this.determineFilePath(activeEditor),
                        textViewer.getTopIndex(), textViewer.getBottomIndex());
            } else {
                this.determineFilePath(activeEditor);
                this.statistics.markUnknownPosition(this.determineFilePath(activeEditor));
            }
        }
    }

    private File determineFilePath(IEditorPart activeEditor) {
        final IFile file = (IFile) activeEditor.getEditorInput().getAdapter(IFile.class);
        if (file != null) {
            final IPath path = file.getLocation();
            if (path == null) {
                return null;
            }
            return path.toFile();
        }
        final FileStoreEditorInput externalFile = (FileStoreEditorInput) 
                activeEditor.getEditorInput().getAdapter(FileStoreEditorInput.class);
        if (externalFile != null) {
            return new File(externalFile.getURI());
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
                CodeViewTracker.this.performSnapshot();
                CodeViewTracker.this.workbench.getDisplay().timerExec(CHECK_INTERVAL, this);
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
