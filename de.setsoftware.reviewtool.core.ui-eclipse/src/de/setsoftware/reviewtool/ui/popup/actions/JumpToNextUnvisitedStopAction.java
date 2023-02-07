package de.setsoftware.reviewtool.ui.popup.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import de.setsoftware.reviewtool.model.changestructure.CurrentStop;
import de.setsoftware.reviewtool.model.changestructure.Stop;
import de.setsoftware.reviewtool.model.changestructure.Tour;
import de.setsoftware.reviewtool.model.changestructure.ToursInReview;
import de.setsoftware.reviewtool.model.viewtracking.ViewStatistics;
import de.setsoftware.reviewtool.model.viewtracking.ViewStatistics.INextStopCallback;
import de.setsoftware.reviewtool.plugin.ReviewPlugin;
import de.setsoftware.reviewtool.ui.dialogs.RealMarkerFactory;
import de.setsoftware.reviewtool.ui.views.ReviewContentView;
import de.setsoftware.reviewtool.ui.views.ViewDataSource;
import de.setsoftware.reviewtool.viewtracking.TrackerManager;

/**
 * Action that jumps to the next review stop that has not been viewed yet.
 */
public class JumpToNextUnvisitedStopAction extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final Shell shell = HandlerUtil.getActiveShell(event);
        final ToursInReview tours = this.getToursAndStartReviewIfNecessary();
        final ViewStatistics stats = TrackerManager.get().getStatistics();
        if (tours == null || stats == null) {
            MessageDialog.openInformation(shell, "No active review",
                    "No active review");
            return null;
        }


        final Stop nextStop = stats.getNextUnvisitedStop(
                tours,
                CurrentStop.getCurrentStop(),
                new INextStopCallback() {

                    @Override
                    public void newTourStarted(Tour tour) {
                        tours.ensureTourActive(tour, new RealMarkerFactory());
                        MessageDialog.openInformation(shell, "Change of review tour",
                                "Start of a new tour:\n" + tour.getDescription());
                    }

                    @Override
                    public void wrappedAround() {
                        MessageDialog.openInformation(shell, "End of last tour",
                                "The end of the last tour has been reached and CoRT wrapped around to the beginning.");
                    }

                });
        if (nextStop == null) {
            MessageDialog.openInformation(shell, "No further stops",
                    "There are no relevant stops left that have not been visited.");
            return null;
        }

        ReviewContentView.jumpTo(tours, tours.getActiveTour(), nextStop, "next");
        return null;
    }

    private ToursInReview getToursAndStartReviewIfNecessary() {
        final ToursInReview tours = ViewDataSource.get().getToursInReview();
        if (tours != null) {
            return tours;
        }
        ReviewPlugin.getInstance().startReview();
        return ViewDataSource.get().getToursInReview();
    }

}
