package de.setsoftware.reviewtool.ui.popup.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import de.setsoftware.reviewtool.model.changestructure.Stop;
import de.setsoftware.reviewtool.model.changestructure.Tour;
import de.setsoftware.reviewtool.model.changestructure.ToursInReview;
import de.setsoftware.reviewtool.plugin.ReviewPlugin;
import de.setsoftware.reviewtool.ui.views.CurrentStop;
import de.setsoftware.reviewtool.ui.views.RealMarkerFactory;
import de.setsoftware.reviewtool.ui.views.ReviewContentView;
import de.setsoftware.reviewtool.ui.views.ViewDataSource;
import de.setsoftware.reviewtool.viewtracking.TrackerManager;
import de.setsoftware.reviewtool.viewtracking.ViewStatistics;
import de.setsoftware.reviewtool.viewtracking.ViewStatistics.INextStopCallback;

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
            MessageDialog.openInformation(shell, "Kein aktives Review",
                    "Kein aktives Review");
            return null;
        }


        final Stop nextStop = stats.getNextUnvisitedStop(
                tours,
                CurrentStop.getCurrentStop(),
                new INextStopCallback() {

                    @Override
                    public void newTourStarted(Tour tour) {
                        try {
                            tours.ensureTourActive(tour, new RealMarkerFactory());
                        } catch (final CoreException e) {
                            throw new RuntimeException(e);
                        }
                        MessageDialog.openInformation(shell, "Wechsel der Tour",
                                "Beginn einer neuen Tour:\n" + tour.getDescription());
                    }

                    @Override
                    public void wrappedAround() {
                        MessageDialog.openInformation(shell, "Ende der letzten Tour",
                                "Es wurde das Ende der letzten Tour erreicht und neu von vorne begonnen.");
                    }

                });
        if (nextStop == null) {
            MessageDialog.openInformation(shell, "Keine weiteren Stops",
                    "Es gibt keine weiteren Stops, die noch nicht betrachtet wurden.");
            return null;
        }

        ReviewContentView.jumpTo(tours, tours.getActiveTour(), nextStop);
        return null;
    }

    private ToursInReview getToursAndStartReviewIfNecessary() throws ExecutionException {
        final ToursInReview tours = ViewDataSource.get().getToursInReview();
        if (tours != null) {
            return tours;
        }
        try {
            ReviewPlugin.getInstance().startReview();
        } catch (final CoreException e) {
            throw new ExecutionException("error while selecting ticket", e);
        }
        return ViewDataSource.get().getToursInReview();
    }

}
