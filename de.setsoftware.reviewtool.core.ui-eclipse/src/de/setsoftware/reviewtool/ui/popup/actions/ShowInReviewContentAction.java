package de.setsoftware.reviewtool.ui.popup.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.part.IShowInTarget;
import org.eclipse.ui.part.ShowInContext;

/**
 * Action to mimic "Show in -> Review content" for keyboard access.
 */
public class ShowInReviewContentAction extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final String targetId = "de.setsoftware.reviewtool.ui.views.reviewcontentview";

        final ShowInContext context = this.getContext(
                HandlerUtil.getCurrentSelection(event),
                HandlerUtil.getActiveEditorInput(event));
        if (context == null) {
            return null;
        }

        final IWorkbenchWindow activeWorkbenchWindow = HandlerUtil.getActiveWorkbenchWindowChecked(event);
        final IWorkbenchPage page = activeWorkbenchWindow.getActivePage();

        try {
            final IViewPart view = page.showView(targetId);
            final IShowInTarget target = (IShowInTarget) view;
            if (target != null) {
                target.show(context);
            }
        } catch (final PartInitException e) {
            throw new ExecutionException("Failed to show in", e); //$NON-NLS-1$
        }

        return null;
    }

    private ShowInContext getContext(ISelection showInSelection, Object input) {
        if (input == null && showInSelection == null) {
            return null;
        }
        return new ShowInContext(input, showInSelection);
    }
}
