package de.setsoftware.reviewtool.popup.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import de.setsoftware.reviewtool.plugin.ReviewPlugin;

public class ReviewInfoAction extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        ReviewPlugin.getInstance().showReviewInfo();
        return null;
    }

}
