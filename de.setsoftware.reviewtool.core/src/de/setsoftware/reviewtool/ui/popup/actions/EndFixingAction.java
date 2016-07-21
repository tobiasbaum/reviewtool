package de.setsoftware.reviewtool.ui.popup.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import de.setsoftware.reviewtool.plugin.ReviewPlugin;

public class EndFixingAction extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        try {
            if (ReviewPlugin.getInstance().hasUnresolvedRemarks()) {
                final Shell shell = HandlerUtil.getActiveShell(event);
                final boolean yes = MessageDialog.openQuestion(shell, "Open remarks",
                        "Some remarks have not been marked as processed. You can mark"
                        + " them with a quick fix on the marker. Finish anyway?");
                if (!yes) {
                    return null;
                }
            }
            ReviewPlugin.getInstance().endFixing();
        } catch (final CoreException e) {
            throw new ExecutionException("problem while ending fixing", e);
        }
        return null;
    }

}
