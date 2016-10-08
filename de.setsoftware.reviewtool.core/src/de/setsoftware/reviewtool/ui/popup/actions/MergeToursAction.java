package de.setsoftware.reviewtool.ui.popup.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import de.setsoftware.reviewtool.model.changestructure.Tour;
import de.setsoftware.reviewtool.plugin.ReviewPlugin;
import de.setsoftware.reviewtool.ui.dialogs.RealMarkerFactory;
import de.setsoftware.reviewtool.ui.views.ViewDataSource;

/**
 * Action to merge the currently selected tours.
 */
public class MergeToursAction extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final ISelection selection = HandlerUtil.getActiveMenuSelectionChecked(event);

        if (!(selection instanceof TreeSelection)) {
            return null;
        }
        final TreeSelection ts = (TreeSelection) selection;

        final List<Tour> tours = new ArrayList<>(ts.toList());
        try {
            ViewDataSource.get().getToursInReview().mergeTours(tours, new RealMarkerFactory());
        } catch (final CoreException e) {
            ReviewPlugin.getInstance().logException(e);
            throw new ExecutionException("error during merge of tours", e);
        }
        return null;
    }

}
