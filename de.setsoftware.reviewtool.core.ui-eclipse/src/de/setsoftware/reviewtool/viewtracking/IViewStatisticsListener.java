package de.setsoftware.reviewtool.viewtracking;

import java.io.File;

/**
 * Interface for observers that want to be notified when the view statistics change.
 */
public interface IViewStatisticsListener {

    /**
     * Is called whenever the statistics for the given file change.
     */
    public abstract void statisticsChanged(File absolutePath);

}
