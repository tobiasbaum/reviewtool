package de.setsoftware.reviewtool.ui.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

/**
 * Widget that shows the progress of the review, with colored bars as well as with a textual note
 * about the unvisited stops.
 */
public class ProgressChart extends Canvas {

    private static final int TEXT_HEIGHT = 15;
    private final Color irrelevantColor;
    private final Color visitedColor;
    private final Color partlyVisitedColor;
    private final Color unvisitedColor;

    private int irrelevant;
    private int visited;
    private int partlyVisited;
    private int unvisited;

    public ProgressChart(Composite parent, int style) {
        super(parent, style);
        this.irrelevantColor = new Color(null, 128, 128, 128);
        this.visitedColor = new Color(null, 0, 255, 0);
        this.partlyVisitedColor = new Color(null, 255, 255, 0);
        this.unvisitedColor = new Color(null, 255, 0, 0);
        this.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                ProgressChart.this.irrelevantColor.dispose();
                ProgressChart.this.visitedColor.dispose();
                ProgressChart.this.partlyVisitedColor.dispose();
                ProgressChart.this.unvisitedColor.dispose();
            }
        });
        this.addPaintListener(new PaintListener() {
            @Override
            public void paintControl(PaintEvent e) {
                ProgressChart.this.paintControl(e);
            }
        });
    }

    private void paintControl(PaintEvent e) {
        final Point size = this.getSize();
        final int barHeight = size.y - 2 - TEXT_HEIGHT;
        e.gc.drawRectangle(0, 0, size.x - 1, barHeight + 1);

        int startX = 1;
        startX = this.paintPart(
                e.gc, startX, this.irrelevantColor, this.irrelevant, size.x - 1, barHeight);
        startX = this.paintPart(
                e.gc, startX, this.visitedColor, this.visited, size.x - 1, barHeight);
        startX = this.paintPart(
                e.gc, startX, this.partlyVisitedColor, this.partlyVisited, size.x - 1, barHeight);
        this.paintPart(e.gc, startX, this.unvisitedColor, this.unvisited, size.x - 1, barHeight);

        e.gc.drawString(this.unvisited + " unvisited relevant stops left", 1, barHeight + 1, true);
    }

    private int paintPart(GC gc, int startX, Color color, int value, int maxWidth, int height) {
        final int total = this.irrelevant + this.visited + this.partlyVisited + this.unvisited;
        if (total == 0) {
            return startX;
        }
        final int width = value * maxWidth / total;
        if (value > 0) {
            gc.setBackground(color);
            gc.fillRectangle(startX, 1, maxWidth - startX, height);
        }
        return startX + width;
    }

    /**
     * Updates the counter values to the given arguments.
     */
    public void setCounts(int irrelevant, int visited, int partlyVisited, int unvisited) {
        this.irrelevant = irrelevant;
        this.visited = visited;
        this.partlyVisited = partlyVisited;
        this.unvisited = unvisited;
        this.redraw();
    }

    @Override
    public Point computeSize(int wHint, int hHint, boolean changed) {
        final int width;
        if (wHint != SWT.DEFAULT) {
            width = wHint;
        } else {
            width = 100;
        }

        int height = 10 + TEXT_HEIGHT;
        if (hHint != SWT.DEFAULT) {
            height = Math.min(hHint, height);
        }

        return new Point(width, height);
    }

}
