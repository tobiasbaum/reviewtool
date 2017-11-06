package de.setsoftware.reviewtool.ui.views;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

/**
 * Caches images and has nice factory methods for them.
 */
public class ImageCache {

    private static final Rectangle TREE_IMAGE_SIZE = new Rectangle(0, 0, 16, 16);

    private static Map<String, Image> images = new HashMap<>();

    /**
     * Returns a filled circle in tree icon size in the given color.
     */
    public static Image getColoredDot(RGB rgb) {
        final String key = "cd:" + rgb.toString();
        if (images.containsKey(key)) {
            return images.get(key);
        }

        final Device dev = Display.getDefault();
        final Rectangle rect = TREE_IMAGE_SIZE;
        final Image img = new Image(dev, rect);
        final GC gc = new GC(img);
        final Color color = new Color(dev, rgb);
        gc.setForeground(color);
        gc.setBackground(color);
        gc.fillOval(rect.width / 2, rect.height / 4, rect.width / 2, rect.height / 2);
        color.dispose();
        gc.dispose();

        cache(key, img);
        return img;
    }

    private static void cache(final String key, final Image img) {
        images.put(key, img);
    }

    /**
     * Returns a rectangle in tree icon size with a border and fill in the given colors.
     */
    public static Image getColoredRectangle(RGB border, RGB fill) {
        final String key = "cr:" + border.toString() + fill.toString();
        if (images.containsKey(key)) {
            return images.get(key);
        }

        final Device dev = Display.getDefault();
        final Image img = new Image(dev, TREE_IMAGE_SIZE);
        final GC gc = new GC(img);
        final Color colorBorder = new Color(dev, border);
        gc.setBackground(colorBorder);
        gc.fillRectangle(8, 3, 8, 10);
        colorBorder.dispose();
        final Color colorFill = new Color(dev, fill);
        gc.setBackground(colorFill);
        gc.fillRectangle(9, 4, 6, 8);
        colorFill.dispose();
        gc.dispose();

        cache(key, img);
        return img;
    }

    /**
     * Returns a half circle in tree icon size with a border and fill in the given colors.
     */
    public static Image getColoredHalfCircle(RGB border, RGB fill) {
        final String key = "cc:" + border.toString() + fill.toString();
        if (images.containsKey(key)) {
            return images.get(key);
        }

        final Device dev = Display.getDefault();
        final Image img = new Image(dev, TREE_IMAGE_SIZE);
        final GC gc = new GC(img);
        final Color colorBorder = new Color(dev, border);
        gc.setBackground(colorBorder);
        gc.fillArc(8, 3, 12, 8, 90, 180);
        colorBorder.dispose();
        final Color colorFill = new Color(dev, fill);
        gc.setBackground(colorFill);
        gc.fillArc(9, 4, 10, 6, 90, 180);
        colorFill.dispose();
        gc.dispose();

        cache(key, img);
        return img;
    }

    /**
     * Returns a green check mark in tree image size.
     */
    public static Image getGreenCheckMark() {
        final String key = "gcm";
        if (images.containsKey(key)) {
            return images.get(key);
        }

        final Device dev = Display.getDefault();
        final Image img = new Image(dev, TREE_IMAGE_SIZE);
        final GC gc = new GC(img);
        final Color green = new Color(dev, 0, 255, 0);
        gc.setForeground(green);
        gc.setLineWidth(2);
        gc.drawLine(4, 12, 8, 15);
        gc.drawLine(8, 15, 15, 1);
        green.dispose();
        gc.dispose();

        cache(key, img);
        return img;
    }

    /**
     * Disposes all cached images and clears the cache.
     */
    public static void dispose() {
        for (final Image img : images.values()) {
            img.dispose();
        }
        images.clear();
    }

}
