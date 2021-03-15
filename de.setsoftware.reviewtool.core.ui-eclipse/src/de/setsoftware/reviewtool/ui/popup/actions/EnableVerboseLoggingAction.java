package de.setsoftware.reviewtool.ui.popup.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import de.setsoftware.reviewtool.base.Logger;

/**
 * Action to enable verbose debug logging.
 */
public class EnableVerboseLoggingAction extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        Logger.setVerbose();
        return null;
    }

}
