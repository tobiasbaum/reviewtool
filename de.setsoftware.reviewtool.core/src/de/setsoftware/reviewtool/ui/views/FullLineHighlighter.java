package de.setsoftware.reviewtool.ui.views;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.custom.LineBackgroundEvent;
import org.eclipse.swt.custom.LineBackgroundListener;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.part.FileEditorInput;

import de.setsoftware.reviewtool.model.Constants;
import de.setsoftware.reviewtool.model.api.IFragment;
import de.setsoftware.reviewtool.model.changestructure.Stop;
import de.setsoftware.reviewtool.model.changestructure.Tour;
import de.setsoftware.reviewtool.model.changestructure.ToursInReview;
import de.setsoftware.reviewtool.ui.dialogs.DialogHelper;

/**
 * There is no simple way to highlight annotation markers to the full width of the editor in Eclipse.
 * Therefore this class uses LineBackgroundListeners to achieve the same effect. I registers the respective
 * listeners for all open editors.
 */
public class FullLineHighlighter {

    private static final IPartListener partListener = new IPartListener() {
        @Override
        public void partActivated(IWorkbenchPart part) {
            registerHighlighters();
        }

        @Override
        public void partBroughtToTop(IWorkbenchPart part) {
        }

        @Override
        public void partClosed(IWorkbenchPart part) {
        }

        @Override
        public void partDeactivated(IWorkbenchPart part) {
        }

        @Override
        public void partOpened(IWorkbenchPart part) {
        }
    };

    /**
     * Superclass for the LineBackgroundListener to highlight the stops.
     */
    private abstract static class StopLineHighlighter implements LineBackgroundListener {

        protected final Color getActiveStopColor(LineBackgroundEvent event) {
            return DialogHelper.getColor(event.display, "tourStop_color", new RGB(202, 202, 255));
        }

        protected final Color getInactiveStopColor(LineBackgroundEvent event) {
            return DialogHelper.getColor(event.display, "inactiveTourStop_color", new RGB(212, 212, 245));
        }

    }

    /**
     * Highlights review stops based on marker annotations. Is preferable if possible, because
     * marker annotations automatically change position when editing.
     */
    private static final class MarkerBasedLineHighlighter extends StopLineHighlighter {
        private final IFile file;

        public MarkerBasedLineHighlighter(IFile file) {
            this.file = file;
        }

        @Override
        public void lineGetBackground(LineBackgroundEvent event) {
            if (this.lineBelongsToMarker(event, Constants.INACTIVESTOPMARKER_ID)) {
                event.lineBackground = this.getInactiveStopColor(event);
            }
            if (this.lineBelongsToMarker(event, Constants.STOPMARKER_ID)) {
                event.lineBackground = this.getActiveStopColor(event);
            }
        }

        private boolean lineBelongsToMarker(LineBackgroundEvent event, String markerId) {
            try {
                final IMarker[] markers = this.file.findMarkers(markerId, true, IResource.DEPTH_INFINITE);
                final int lineEndOffset = this.getLineEndOffset(event);
                for (final IMarker marker : markers) {
                    if (this.isInsideMarker(marker, lineEndOffset)) {
                        return true;
                    }
                }
                return false;
            } catch (final CoreException e) {
                return false;
            }
        }

        private int getLineEndOffset(LineBackgroundEvent event) {
            final StyledText t = (StyledText) event.widget;
            final int textLength = t.getCharCount();
            if (textLength == 0) {
                return event.lineOffset;
            }
            final String s = t.getText(Math.min(event.lineOffset, textLength - 1), textLength - 1);
            final int lineEnd = s.indexOf('\n');
            if (lineEnd >= 0) {
                return event.lineOffset + lineEnd;
            } else {
                return event.lineOffset + s.length();
            }
        }

        private boolean isInsideMarker(IMarker marker, int offset) {
            return offset >= marker.getAttribute(IMarker.CHAR_START, 0)
                && offset < marker.getAttribute(IMarker.CHAR_END, 0);
        }

    }

    /**
     * Highlights review stops based on stop objects. Is the only possibility for files that are no
     * resources in the Eclipse workspace.
     */
    private static final class StopBasedLineHighlighter extends StopLineHighlighter {
        private final File file;

        public StopBasedLineHighlighter(File file) {
            this.file = file.getAbsoluteFile();
        }

        @Override
        public void lineGetBackground(LineBackgroundEvent event) {
            final ToursInReview tours = ViewDataSource.get().getToursInReview();
            if (tours == null) {
                return;
            }
            for (final Tour tour : tours.getTopmostTours()) {
                if (this.belongsToStopInTour(event, tour)) {
                    if (tour == tours.getActiveTour()) {
                        event.lineBackground = this.getActiveStopColor(event);
                    } else {
                        event.lineBackground = this.getInactiveStopColor(event);
                    }
                    break;
                }
            }
        }

        private boolean belongsToStopInTour(LineBackgroundEvent event, Tour tour) {
            for (final Stop s : tour.getStopsFor(this.file)) {
                if (this.lineBelongsToStop(event, s)) {
                    return true;
                }
            }
            return false;
        }

        private boolean lineBelongsToStop(LineBackgroundEvent event, Stop s) {
            final int lineNumber = ((StyledText) event.widget).getLineAtOffset(event.lineOffset) + 1;
            final IFragment fragment = s.getMostRecentFragment();
            return lineNumber >= fragment.getFrom().getLine()
                && (lineNumber < fragment.getTo().getLine()
                    || (lineNumber == fragment.getTo().getLine() && fragment.getTo().getColumn() > 1));
        }
    }

    private static final List<WeakReference<StyledText>> alreadyRegistered = new ArrayList<>();

    /**
     * Register highlighters for all open editors. Does nothing when every editor already had
     * a listener.
     * Once this method has been called, the class tries to automatically register for new editors.
     */
    public static void registerHighlighters() {
        final IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window == null) {
            return;
        }
        //register a part listener so that we will get notified of editor changes in the future
        //  the addPartListener takes care of not adding the listener twice
        window.getPartService().addPartListener(partListener);

        final IWorkbenchPage activePage = window.getActivePage();
        if (activePage == null) {
            return;
        }
        final IEditorReference[] editorReferences = activePage.getEditorReferences();
        for (final IEditorReference ref : editorReferences) {
            final IEditorPart editor = ref.getEditor(false);
            if (editor == null) {
                continue;
            }
            final Control control = editor.getAdapter(Control.class);
            if (!(control instanceof StyledText)) {
                continue;
            }
            final StyledText text = (StyledText) control;
            if (isAlreadyRegistered(text)) {
                continue;
            }
            final IEditorInput input = editor.getEditorInput();
            if (input instanceof FileEditorInput) {
                text.addLineBackgroundListener(new MarkerBasedLineHighlighter(
                        ((FileEditorInput) input).getFile()));
            } else if (input instanceof FileStoreEditorInput) {
                text.addLineBackgroundListener(new StopBasedLineHighlighter(
                        new File(((FileStoreEditorInput) input).getURI())));
            }
            alreadyRegistered.add(new WeakReference<>(text));
        }
    }

    private static boolean isAlreadyRegistered(StyledText text) {
        final Iterator<WeakReference<StyledText>> iter = alreadyRegistered.iterator();
        while (iter.hasNext()) {
            final StyledText t = iter.next().get();
            if (t == null) {
                iter.remove();
            }
            if (t == text) {
                return true;
            }
        }
        return false;
    }

}
