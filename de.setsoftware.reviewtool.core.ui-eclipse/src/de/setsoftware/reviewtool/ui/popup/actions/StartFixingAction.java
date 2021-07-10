package de.setsoftware.reviewtool.ui.popup.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import de.setsoftware.reviewtool.model.PositionTransformer;
import de.setsoftware.reviewtool.plugin.ReviewPlugin;

/**
 * Action to enter fixing mode.
 */
public class StartFixingAction extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        EclipsePositionTransformer.initializeCacheInBackground();
        ReviewPlugin.getInstance().startFixing();
        return null;
    }

}
