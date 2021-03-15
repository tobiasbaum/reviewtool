package de.setsoftware.reviewtool.ui.popup.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;

import de.setsoftware.reviewtool.plugin.ReviewPlugin;

/**
 * Action to delete the local repository caches (can be used for example when commit
 * messages were changed).
 */
public class ClearCommitCacheAction extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        if (MessageDialog.openConfirm(null, "Really clear cache?",
                "Do you really want to clear the locally cached version control data?")) {
            ReviewPlugin.getInstance().getChangeManager().clearChangeSourceCaches();
        }
        return null;
    }

}
