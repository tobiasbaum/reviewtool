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
import de.setsoftware.reviewtool.model.changestructure.Tour;
import de.setsoftware.reviewtool.model.changestructure.ToursInReview;
import de.setsoftware.reviewtool.ui.views.ReviewContentView;
import de.setsoftware.reviewtool.ui.views.ViewDataSource;

/**
 * Action that opens/jumps to a stop and forces the text editor to be used.
 */
public class OpenInTextEditorAction extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final ToursInReview tours = ViewDataSource.get().getToursInReview();
        if (tours == null) {
            return null;
        }

        final ISelection selection = HandlerUtil.getActiveMenuSelectionChecked(event);
        if (!(selection instanceof TreeSelection)) {
            return null;
        }
        final TreeSelection ts = (TreeSelection) selection;

        final List<Stop> stops = new ArrayList<>(ts.toList());
        for (final Stop stop : stops) {
            final Tour tour = this.getTour(tours, stop);
            if (tour != null) {
                ReviewContentView.openInTextEditor(tours, tour, stop);
            }
        }
        return null;
    }

    private Tour getTour(ToursInReview tours, Stop stop) {
        for (final Tour tour : tours.getTopmostTours()) {
            if (tour.getStops().contains(stop)) {
                return tour;
            }
        }
        return null;
    }

}
