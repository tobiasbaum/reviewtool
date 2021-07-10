package de.setsoftware.reviewtool.ui.popup.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;

import de.setsoftware.reviewtool.model.changestructure.IChangeManager;
import de.setsoftware.reviewtool.plugin.ChangeManager;
import de.setsoftware.reviewtool.plugin.ReviewPlugin;

/**
 * Action to stop the tracking of local changes (until restart). Can be used for example when locking
 * in SVN leads to problems.
 */
public class StopChangeTrackingAction extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final IChangeManager changeManager = ReviewPlugin.getInstance().getChangeManager();
        if (!changeManager.isTrackingEnabled()) {
            MessageDialog.openInformation(null, "Already disabled",
                    "Change tracking was already disabled. To enable again, restart Eclipse.");
        }
        changeManager.disableChangeTracking();
        return null;
    }

}
