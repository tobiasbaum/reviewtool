package de.setsoftware.reviewtool.model.changestructure;

import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;

/**
 * Manages the current state regarding the change slices under review.
 */
public class SlicesInReview {

    private final List<Slice> slices;
    private final int currentSliceIndex;

    private SlicesInReview(List<Slice> slices) {
        this.slices = slices;
        this.currentSliceIndex = 0;
    }

    /**
     * Loads the slices for the given ticket and creates a corresponding {@link SlicesInReview}
     * object with initial settings.
     */
    public static SlicesInReview create(
            ISliceSource src,
            IFragmentTracer tracer,
            String ticketKey) {
        final List<Slice> slices = src.getSlices(ticketKey);
        //TODO tracer einbauen
        return new SlicesInReview(slices);
    }

    //TODO this is just a method for testing that should disappear some time in the future
    public void showInfo() {
        MessageDialog.openInformation(null, this.slices.size() + " slices", this.slices.toString());
    }

}
