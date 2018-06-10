package de.setsoftware.reviewtool.viewtracking;

import de.setsoftware.reviewtool.base.WeakListeners;
import de.setsoftware.reviewtool.model.changestructure.Stop;

/**
 * Singleton that manages the currently active {@link CodeViewTracker} and allows
 * listening for changes in the active tracker.
 */
public class TrackerManager {

    private static final TrackerManager INSTANCE = new TrackerManager();

    private static final int LONG_ENOUGH_COUNT = 50;

    private CodeViewTracker currentTracker;
    private final WeakListeners<ITrackerCreationListener> creationListeners = new WeakListeners<>();

    public static TrackerManager get() {
        return INSTANCE;
    }

    /**
     * Starts a new tracker. If another one is still active, it is stopped.
     */
    public void startTracker() {
        if (this.currentTracker != null) {
            this.stopTracker();
        }
        this.currentTracker = new CodeViewTracker();
        this.creationListeners.notifyListeners(l -> l.trackerStarts(this.currentTracker));
        this.currentTracker.start();
    }

    /**
     * Stops the currently active tracker, so that none is active afterwards.
     */
    public void stopTracker() {
        if (this.currentTracker == null) {
            return;
        }
        this.currentTracker.stop();
        this.currentTracker = null;
    }

    /**
     * Registers the given listener for changes of the active tracker. If there currently is
     * one active, the listener will be notified immediately for it once.
     */
    public void registerListener(ITrackerCreationListener listener) {
        this.creationListeners.add(listener);
        if (this.currentTracker != null) {
            listener.trackerStarts(this.currentTracker);
        }
    }

    /**
     * Returns a number between 0.0 and 1.0 that denotes the time that the given stop
     * has been viewed. Zero means "not viewed at all", one means "every line has been
     * viewed long enough".
     */
    public ViewStatDataForStop determineViewRatio(Stop f) {
        if (this.currentTracker == null) {
            return ViewStatDataForStop.NO_VIEWS;
        }
        //TODO the long enough count should depend on the size of the stop, because
        //  larger stops usually need more time to be understood
        return this.currentTracker.getStatistics().determineViewRatio(f, LONG_ENOUGH_COUNT);
    }

    public ViewStatistics getStatistics() {
        return this.currentTracker == null ? null : this.currentTracker.getStatistics();
    }

}
