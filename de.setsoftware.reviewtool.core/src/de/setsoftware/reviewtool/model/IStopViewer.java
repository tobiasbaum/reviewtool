package de.setsoftware.reviewtool.model;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewPart;

import de.setsoftware.reviewtool.model.api.IStop;

/**
 * Describes a viewer for a {@link IStop}.
 */
public interface IStopViewer {

    /**
     * Creates a view for a {@link IStop}.
     *
     * @param view The parent {@link IViewPart}.
     * @param scrollContent The parent {@link Composite}.
     * @param stop The {@link IStop} to display.
     */
    public abstract void createStopView(IViewPart view, Composite scrollContent, IStop stop);

}
