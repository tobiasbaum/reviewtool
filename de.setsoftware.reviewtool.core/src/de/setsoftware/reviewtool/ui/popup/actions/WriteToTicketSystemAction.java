package de.setsoftware.reviewtool.ui.popup.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import de.setsoftware.reviewtool.plugin.ReviewPlugin;

/**
 * Action to flush the local review remarks to the ticket system.
 */
public class WriteToTicketSystemAction extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        ReviewPlugin.getInstance().flushLocalReviewData();
        return null;
    }

}
