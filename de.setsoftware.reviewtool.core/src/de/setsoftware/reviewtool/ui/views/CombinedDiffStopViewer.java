package de.setsoftware.reviewtool.ui.views;

import java.util.List;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.part.ViewPart;

import de.setsoftware.reviewtool.model.changestructure.FileChangeHistory;
import de.setsoftware.reviewtool.model.changestructure.FileDiff;
import de.setsoftware.reviewtool.model.changestructure.FileInRevision;
import de.setsoftware.reviewtool.model.changestructure.Fragment;
import de.setsoftware.reviewtool.model.changestructure.Hunk;
import de.setsoftware.reviewtool.model.changestructure.IncompatibleFragmentException;
import de.setsoftware.reviewtool.model.changestructure.Stop;
import de.setsoftware.reviewtool.model.changestructure.ToursInReview;

/**
 * Displays all differences of a {@link Stop} combined in a single window.
 */
public class CombinedDiffStopViewer extends AbstractStopViewer {

    @Override
    public void createStopView(final ViewPart view, final Composite scrollContent, final Stop stop) {
        final ToursInReview tours = ViewDataSource.get().getToursInReview();
        final FileChangeHistory changeHistory = tours.getChangeHistory(stop.getHistory().get(0));
        if (changeHistory != null) {
            final List<FileInRevision> history = stop.getHistory();
            final FileInRevision firstRevision = history.get(0);
            final FileInRevision lastRevision = history.get(history.size() - 1);

            final List<Fragment> fragmentsFirst = stop.getContentFor(firstRevision);
            final List<Fragment> fragmentsLast = stop.getContentFor(lastRevision);
            if (fragmentsFirst == null || fragmentsLast == null) { // binary change
                this.createDiffViewer(view, scrollContent, firstRevision, lastRevision, fragmentsFirst, fragmentsLast);
            } else { // textual change
                try {
                    final FileDiff diff = changeHistory.build(firstRevision, lastRevision);
                    final List<Hunk> hunks = diff.getHunksForTargets(fragmentsLast);
                    this.createDiffViewer(view, scrollContent, firstRevision, lastRevision,
                            Hunk.getSources(hunks).getFragments(), Hunk.getTargets(hunks).getFragments());
                } catch (final IncompatibleFragmentException e) {
                    final Label label = new Label(scrollContent, SWT.NULL);
                    label.setText("Error while merging fragments for " + stop.toString());
                    label.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT));
                    ViewHelper.createContextMenuWithoutSelectionProvider(view, label);
                }
            }
        }
    }

}
