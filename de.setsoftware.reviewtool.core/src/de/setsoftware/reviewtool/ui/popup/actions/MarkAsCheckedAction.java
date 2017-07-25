package de.setsoftware.reviewtool.ui.popup.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import de.setsoftware.reviewtool.model.changestructure.Stop;
import de.setsoftware.reviewtool.viewtracking.TrackerManager;
import de.setsoftware.reviewtool.viewtracking.ViewStatistics;

/**
 * Sets the stop on which the action was called as explicitly checked or clears the flag
 * if it is set.
 */
public class MarkAsCheckedAction extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final ISelection selection = HandlerUtil.getActiveMenuSelectionChecked(event);

        if (!(selection instanceof TreeSelection)) {
            return null;
        }
        final TreeSelection ts = (TreeSelection) selection;

        final List<Stop> stops = new ArrayList<>(ts.toList());
        final ViewStatistics statistics = TrackerManager.get().getStatistics();
        statistics.toggleExplicitlyCheckedMark(stops);
        return null;
    }

}
