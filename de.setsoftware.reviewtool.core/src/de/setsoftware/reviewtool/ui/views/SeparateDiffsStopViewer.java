package de.setsoftware.reviewtool.ui.views;

import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

import de.setsoftware.reviewtool.model.changestructure.FileInRevision;
import de.setsoftware.reviewtool.model.changestructure.Fragment;
import de.setsoftware.reviewtool.model.changestructure.Stop;

/**
 * Displays each difference of a {@link Stop} in a separate window.
 */
public class SeparateDiffsStopViewer extends AbstractStopViewer {

    @Override
    public void createStopView(final ViewPart view, final Composite scrollContent, final Stop stop) {
        final Iterator<FileInRevision> it = stop.getHistory().iterator();
        FileInRevision oldRevision = null;
        while (it.hasNext()) {
            final FileInRevision newRevision = it.next();
            if (oldRevision != null) {
                this.createContentLabel(view, scrollContent, stop, oldRevision, newRevision);
                oldRevision = null;
            } else {
                oldRevision = newRevision;
            }
        }
    }

   private void createContentLabel(final ViewPart view, final Composite scrollContent, final Stop stop,
           final FileInRevision oldRevision, final FileInRevision newRevision) {
       final List<Fragment> oldContent = stop.getContentFor(oldRevision);
       final List<Fragment> newContent = stop.getContentFor(newRevision);
       this.createDiffViewer(view, scrollContent, oldRevision, newRevision, oldContent, newContent);
   }

}
