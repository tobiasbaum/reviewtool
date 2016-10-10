package de.setsoftware.reviewtool.ui.views;

import java.lang.ref.WeakReference;

import de.setsoftware.reviewtool.base.WeakListeners;
import de.setsoftware.reviewtool.model.changestructure.Stop;

/**
 * Manages the currently selected stop and listeners for its change.
 */
public class CurrentStop {

    private static WeakListeners<StopSelectionListener> listeners = new WeakListeners<>();
    private static WeakReference<Stop> currentStop;

    static void setCurrentStop(Stop fragment) {
        currentStop = new WeakReference<Stop>(fragment);
        for (final StopSelectionListener l : listeners.getListeners()) {
            l.notifyStopChange(fragment);
        }
    }

    /**
     * Resets the current stop to "no selection" and notifies all listeners of this.
     */
    public static void unsetCurrentStop() {
        currentStop = null;
        for (final StopSelectionListener l : listeners.getListeners()) {
            l.notifyStopChange(null);
        }
    }

    /**
     * Returns the stop last used/jumped to by the user.
     */
    public static Stop getCurrentStop() {
        if (currentStop == null) {
            return null;
        }
        return currentStop.get();
    }

    static void registerListener(StopSelectionListener listener) {
        listeners.add(listener);
        listener.notifyStopChange(getCurrentStop());
    }

}
