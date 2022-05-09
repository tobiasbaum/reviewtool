package de.setsoftware.reviewtool.ui.api;

import org.eclipse.swt.widgets.Composite;

/**
 * Allows extensions to the "end review" dialog.
 */
public interface EndReviewExtension {

    /**
     * Allows the extension to create controls and other objects for a dialog.
     * The dialog's content composite is given as a parameter.
     * This operation must return a {@link EndReviewExtensionData} object which
     * encapsulates the additional state needed for the dialog.
     */
    public abstract EndReviewExtensionData createControls(Composite comp);

}
