package de.setsoftware.reviewtool.model.changestructure;

import java.lang.ref.WeakReference;

import de.setsoftware.reviewtool.base.WeakListeners;

/**
 * Manages the currently selected stop and listeners for its change.
 */
public class CurrentStop {

    private static WeakListeners<StopSelectionListener> listeners = new WeakListeners<>();
    private static WeakReference<Stop> currentStop;

    public static void setCurrentStop(Stop fragment) {
        currentStop = new WeakReference<Stop>(fragment);
        listeners.notifyListeners(l -> l.notifyStopChange(fragment));
    }

    /**
     * Resets the current stop to "no selection" and notifies all listeners of this.
     */
    public static void unsetCurrentStop() {
        currentStop = null;
        listeners.notifyListeners(l -> l.notifyStopChange(null));
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

    public static void registerListener(StopSelectionListener listener) {
        listeners.add(listener);
        listener.notifyStopChange(getCurrentStop());
    }

    public static void unregisterListener(StopSelectionListener listener) {
        listeners.remove(listener);
    }

}
