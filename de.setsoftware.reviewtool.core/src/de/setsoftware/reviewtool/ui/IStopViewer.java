package de.setsoftware.reviewtool.ui;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

import de.setsoftware.reviewtool.model.changestructure.Stop;

/**
 * Describes a viewer for a {@link Stop}.
 */
public interface IStopViewer {

    /**
     * Creates a {@link Stop} view.
     * @param view The parent {@link ViewPart}.
     * @param scrollContent The parent {@link Composite}.
     * @param stop The {@link Stop} to display.
     */
    void createStopView(ViewPart view, Composite scrollContent, Stop stop);

}
