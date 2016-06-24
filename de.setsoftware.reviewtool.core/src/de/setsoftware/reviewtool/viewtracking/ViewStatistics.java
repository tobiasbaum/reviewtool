package de.setsoftware.reviewtool.viewtracking;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import de.setsoftware.reviewtool.base.WeakListeners;
import de.setsoftware.reviewtool.model.changestructure.Fragment;
import de.setsoftware.reviewtool.model.changestructure.Stop;

/**
 * Statistics on if and how long portions of files have been viewed.
 */
public class ViewStatistics {

    private final Map<File, ViewStatisticsForFile> files = new HashMap<>();
    private final WeakListeners<IViewStatisticsListener> listeners = new WeakListeners<>();

    /**
     * Marks that the given portion of the file has been viewed for one time slot.
     */
    public void mark(File filePath, int fromLine, int toLine) {
        final File absFile = filePath.getAbsoluteFile();
        this.getOrCreate(absFile).mark(fromLine, toLine);
        this.notifyListeners(absFile);
    }

    /**
     * Marks that the file has been viewed for one time slot when no specific information
     * on the viewed part of the file is available.
     */
    public void markUnknownPosition(File filePath) {
        final File absFile = filePath.getAbsoluteFile();
        this.getOrCreate(absFile).markUnknownPosition();
        this.notifyListeners(absFile);
    }

    private ViewStatisticsForFile getOrCreate(File absFile) {
        ViewStatisticsForFile ret = this.files.get(absFile);
        if (ret == null) {
            ret = new ViewStatisticsForFile();
            this.files.put(absFile, ret);
        }
        return ret;
    }

    /**
     * Returns a number between 0.0 and 1.0 that denotes the time that the given stop
     * has been viewed. Zero means "not viewed at all", one means "every line has been
     * viewed long enough".
     */
    public double determineViewRatio(Stop f, int longEnoughCount) {
        final File absFile = f.getAbsoluteFile();
        final ViewStatisticsForFile stats = this.files.get(absFile);
        if (stats == null) {
            return 0.0;
        }
        final Fragment fragment = f.getMostRecentFragment();
        if (fragment == null) {
            return stats.determineViewRatioWithoutPosition(longEnoughCount);
        } else {
            final int toCorrection = fragment.getTo().getColumn() == 0 ? -1 : 0;
            return stats.determineViewRatio(fragment.getFrom().getLine(),
                    fragment.getTo().getLine() + toCorrection, longEnoughCount);
        }
    }

    private void notifyListeners(File absFile) {
        for (final IViewStatisticsListener l : this.listeners.getListeners()) {
            l.statisticsChanged(absFile);
        }
    }

    public void addListener(IViewStatisticsListener listener) {
        this.listeners.add(listener);
    }

}
