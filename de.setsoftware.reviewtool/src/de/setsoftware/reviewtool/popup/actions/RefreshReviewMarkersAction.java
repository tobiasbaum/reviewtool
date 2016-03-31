package de.setsoftware.reviewtool.popup.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;

import de.setsoftware.reviewtool.plugin.ReviewPlugin;

public class RefreshReviewMarkersAction extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        try {
            ReviewPlugin.getInstance().refreshMarkers();
        } catch (final CoreException e) {
            throw new ExecutionException("problem while ending fixing", e);
        }
        return null;
    }

}
