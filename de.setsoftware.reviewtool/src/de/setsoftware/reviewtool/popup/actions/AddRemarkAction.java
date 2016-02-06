package de.setsoftware.reviewtool.popup.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
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

import de.setsoftware.reviewtool.dialogs.CreateRemarkDialog;
import de.setsoftware.reviewtool.dialogs.CreateRemarkDialog.CreateDialogCallback;
import de.setsoftware.reviewtool.model.PositionTransformer;
import de.setsoftware.reviewtool.model.RemarkType;
import de.setsoftware.reviewtool.model.ReviewRemark;
import de.setsoftware.reviewtool.model.ReviewStateManager;
import de.setsoftware.reviewtool.plugin.ReviewPlugin;
import de.setsoftware.reviewtool.plugin.ReviewPlugin.Mode;

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

		//		createMarker(EditorUtility.getActiveEditorJavaInput(), "in file", sel.getStartLine());
		//		final IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		//		final IEditorPart activeEditor = page.getActiveEditor();
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
		CreateRemarkDialog.get(new CreateDialogCallback() {
			@Override
			public void execute(String text, RemarkType type) {
				try {
					final ReviewStateManager p = ReviewPlugin.getPersistence();
					ReviewRemark.create(
							p,
							resource,
							p.getReviewerForRound(p.getCurrentRound()),
							text,
							line,
							type).save();
				} catch (final CoreException e) {
					throw new RuntimeException(e);
				}
			}
		});
	}

}
