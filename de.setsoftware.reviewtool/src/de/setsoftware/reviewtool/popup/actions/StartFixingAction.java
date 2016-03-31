package de.setsoftware.reviewtool.popup.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;

import de.setsoftware.reviewtool.model.PositionTransformer;
import de.setsoftware.reviewtool.plugin.ReviewPlugin;

public class StartFixingAction extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        PositionTransformer.initializeCacheInBackground();

        try {
            ReviewPlugin.getInstance().startFixing();
        } catch (final CoreException e) {
            throw new ExecutionException("problem while starting fixing", e);
        }
        return null;
    }

}
