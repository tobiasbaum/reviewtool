package de.setsoftware.reviewtool.ui.popup.actions;

import java.util.EnumSet;
import java.util.Set;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.part.FileEditorInput;

import de.setsoftware.reviewtool.base.ReviewtoolException;
import de.setsoftware.reviewtool.model.PositionTransformer;
import de.setsoftware.reviewtool.model.RemarkType;
import de.setsoftware.reviewtool.model.ReviewRemark;
import de.setsoftware.reviewtool.model.ReviewStateManager;
import de.setsoftware.reviewtool.plugin.ReviewPlugin;
import de.setsoftware.reviewtool.plugin.ReviewPlugin.Mode;
import de.setsoftware.reviewtool.telemetry.Telemetry;
import de.setsoftware.reviewtool.ui.dialogs.CreateRemarkDialog;
import de.setsoftware.reviewtool.ui.dialogs.CreateRemarkDialog.CreateDialogCallback;
import de.setsoftware.reviewtool.ui.dialogs.CreateRemarkDialog.PositionReference;

/**
 * Action that adds a review remark (after prompting the user for details with a dialog).
 */
public class AddRemarkAction extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        PositionTransformer.initializeCacheInBackground();
        if (ReviewPlugin.getInstance().getMode() == Mode.IDLE) {
            try {
                ReviewPlugin.getInstance().startReview();
            } catch (final CoreException e) {
                throw new ExecutionException("exception while starting review", e);
            }
        }

        final Shell shell = HandlerUtil.getActiveShell(event);
        ISelection sel = HandlerUtil.getActiveMenuSelection(event);
        if (sel == null) {
            sel = HandlerUtil.getActiveEditor(event).getEditorSite().getSelectionProvider().getSelection();
        }
        if (sel == null) {
            return null;
        }

        if (sel instanceof TextSelection) {
            this.handleTextSelection(shell, (TextSelection) sel, event);
        } else if (sel instanceof IStructuredSelection) {
            this.handleStructuredSelection(shell, (IStructuredSelection) sel);
        } else {
            MessageDialog.openInformation(shell, "Unsupported selection",
                    "type=" + sel.getClass());
        }
        return null;
    }

    private void handleTextSelection(Shell shell, TextSelection sel, ExecutionEvent event)
            throws ExecutionException {

        //      createMarker(EditorUtility.getActiveEditorJavaInput(), "in file", sel.getStartLine());
        //      final IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        //      final IEditorPart activeEditor = page.getActiveEditor();
        final IEditorPart activeEditor = HandlerUtil.getActiveEditor(event);
        final IEditorInput input = activeEditor.getEditorInput();
        if (input instanceof FileEditorInput) {
            final IFile f = ((FileEditorInput) input).getFile();
            this.createMarker(f, sel.getStartLine() + 1);
        }
    }

    private void handleStructuredSelection(Shell shell, IStructuredSelection selection)
            throws ExecutionException {

        final Object firstElement = selection.getFirstElement();
        if (firstElement instanceof IJavaProject) {
            this.createMarker((IJavaProject) firstElement, 0);
        } else if (firstElement instanceof ICompilationUnit) {
            this.createMarker((ICompilationUnit) firstElement, 0);
        } else if (firstElement instanceof IPackageFragment) {
            this.createMarker((IPackageFragment) firstElement, 0);
        } else if (firstElement instanceof IResource) {
            this.createMarker((IResource) firstElement, 0);
        } else if (firstElement instanceof IAdaptable) {
            final Object res = ((IAdaptable) firstElement).getAdapter(IResource.class);
            if (res != null) {
                this.createMarker((IResource) res, 0);
            } else {
                MessageDialog.openInformation(shell, "Unsupported selection 3",
                        "type=" + firstElement.getClass());
            }
        } else {
            MessageDialog.openInformation(shell, "Unsupported selection 2",
                    "type=" + firstElement.getClass());
        }
    }

    private void createMarker(IJavaElement type, int line) throws ExecutionException {
        try {
            this.createMarker(type.getUnderlyingResource(), line);
        } catch (final JavaModelException e) {
            throw new ExecutionException("Error while adding marker", e);
        }
    }

    private void createMarker(final IResource resource, final int line) throws ExecutionException {
        try {
            Set<PositionReference> allowedRefs;
            if (resource.getType() != IResource.FILE) {
                allowedRefs = EnumSet.of(PositionReference.GLOBAL);
            } else if (line <= 0) {
                allowedRefs = EnumSet.of(PositionReference.GLOBAL, PositionReference.FILE);
            } else {
                allowedRefs = EnumSet.allOf(PositionReference.class);
            }
            CreateRemarkDialog.get(allowedRefs, new CreateDialogCallback() {
                @Override
                public void execute(String text, RemarkType type, PositionReference chosenRef) {
                    try {
                        final ReviewStateManager p = ReviewPlugin.getPersistence();
                        final IResource resourceFiltered = chosenRef != PositionReference.GLOBAL
                                ? resource : ResourcesPlugin.getWorkspace().getRoot();
                        final int lineFiltered = chosenRef == PositionReference.LINE ? line : 0;
                        final String reviewer = p.getReviewerForRound(p.getCurrentRound());
                        ReviewRemark.create(
                                p,
                                resourceFiltered,
                                reviewer,
                                text,
                                lineFiltered,
                                type).save();
                        Telemetry.get().remarkCreated(
                                p.getTicketKey(),
                                reviewer,
                                type.name(),
                                resource.getFullPath().toString(),
                                lineFiltered);
                    } catch (final CoreException e) {
                        throw new ReviewtoolException(e);
                    }
                }
            });
        } catch (final ReviewtoolException e) {
            throw new ExecutionException("error creating marker", e);
        }
    }

}
