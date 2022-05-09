package de.setsoftware.reviewtool.ui.dialogs;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import de.setsoftware.reviewtool.base.Logger;

/**
 * Class with helper functions for dialogs.
 */
public class DialogHelper {

    private static final String PREFIX = "dialogSizes_";

    private static IPersistentPreferenceStore preferenceStore;

    private static final Map<Object, Color> colorCache = new HashMap<>();

    public static void setPreferenceStore(IPersistentPreferenceStore ps) {
        preferenceStore = ps;
    }

    /**
     * Saves the given dialog size for the dialog with the given ID in the plugin's preferences.
     */
    public static void saveDialogSize(String id, int width, int height) {
        preferenceStore.setValue(determineId(id, "x"), width);
        preferenceStore.setValue(determineId(id, "y"), height);
        saveIfNeeded();
    }

    /**
     * Saves the dialog size for the given window (with the ID given by the window's class name).
     */
    public static void saveDialogSize(Window window) {
        final Point size = window.getShell().getSize();
        saveDialogSize(window.getClass().getName(), size.x, size.y);
    }

    private static void saveIfNeeded() {
        if (preferenceStore.needsSaving()) {
            try {
                preferenceStore.save();
            } catch (final IOException e) {
                Logger.error("error while saving preferences", e);
            }
        }
    }

    /**
     * Returns the saved size for the dialog with the given id.
     */
    public static Point getDialogSize(String id) {
        final int x = getSize(id, "x");
        final int y = getSize(id, "y");
        if (x > 0 && y > 0) {
            return new Point(x, y);
        } else {
            return null;
        }

    }

    private static int getSize(String id, String coord) {
        return preferenceStore.getInt(determineId(id, coord));
    }

    private static String determineId(String id, String coord) {
        return PREFIX + id + "_" + coord;
    }

    /**
     * Sets the dialog's size to the saved values. If no saved values are available, the given default values
     * are used.
     */
    public static void restoreSavedSize(
            Shell newShell, Window dialog, int defaultWidth, int defaultHeight) {
        final Point savedSize = getDialogSize(dialog.getClass().getName());
        if (savedSize == null) {
            newShell.setSize(defaultWidth, defaultHeight);
        } else {
            newShell.setSize(savedSize);
        }
    }

    /**
     * Saves some generic setting with the given ID.
     */
    public static void saveSetting(String id, String value) {
        preferenceStore.setValue("dialogSetting_" + id, value);
        saveIfNeeded();
    }

    /**
     * Returns the value of a setting saved with {@link #saveSetting(String, String)}.
     * If there is no such setting, the empty string is returned.
     */
    public static String getSetting(String id) {
        return preferenceStore.getString("dialogSetting_" + id);
    }

    /**
     * Opens the given link (normally in a browser, depending on the platform settings).
     */
    public static void openLink(String link) {
        try {
            Desktop.getDesktop().browse(new URI(link));
        } catch (IOException | URISyntaxException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Returns the color for the given preference key. If the preference is not set, returns the
     * given default.
     */
    public static Color getColor(Device device, String preferenceKey, RGB defaultValue) {
        if (colorCache.containsKey(preferenceKey)) {
            return colorCache.get(preferenceKey);
        }

        final IPreferenceStore editorPreferences =
                new ScopedPreferenceStore(InstanceScope.INSTANCE, "org.eclipse.ui.editors");

        RGB rgb;
        if (editorPreferences.contains(preferenceKey)) {
            rgb = PreferenceConverter.getColor(editorPreferences, preferenceKey);
        } else {
            rgb = defaultValue;
        }
        Color cached = colorCache.get(rgb);
        if (cached == null) {
            cached = new Color(device, rgb);
            colorCache.put(rgb, cached);
            colorCache.put(preferenceKey, cached);
        }
        return cached;
    }

}
