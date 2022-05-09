package de.setsoftware.reviewtool.ui.popup.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;

import de.setsoftware.reviewtool.plugin.ReviewPlugin;

/**
 * Action to reload the markers for the review.
 */
public class RefreshReviewMarkersAction extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        try {
            ReviewPlugin.getInstance().refreshMarkers();
        } catch (final CoreException e) {
            ReviewPlugin.getInstance().logException(e);
            throw new ExecutionException("problem while refreshing markers", e);
        }
        return null;
    }

}
