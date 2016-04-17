package de.setsoftware.reviewtool.model.changestructure;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import de.setsoftware.reviewtool.base.ReviewtoolException;
import de.setsoftware.reviewtool.model.Constants;
import de.setsoftware.reviewtool.model.IMarkerFactory;

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
            IChangeSource src,
            ISlicingStrategy slicer,
            String ticketKey) {
        final List<Slice> slices = slicer.toSlices(src.getChanges(ticketKey));
        return new SlicesInReview(slices);
    }

    /**
     * Creates markers for the fragments. Takes the currently active slice into account.
     */
    public void createMarkers(IMarkerFactory markerFactory) {
        if (this.slices.size() <= this.currentSliceIndex) {
            return;
        }
        final Slice s = this.slices.get(this.currentSliceIndex);
        final Map<IResource, PositionLookupTable> lookupTables = new HashMap<>();
        for (final SliceFragment f : s.getFragments()) {
            createMarkerFor(markerFactory, lookupTables, f);
        }
    }

    private static IMarker createMarkerFor(
            IMarkerFactory markerFactory,
            final Map<IResource, PositionLookupTable> lookupTables,
            final SliceFragment f) {

        try {
            final IResource resource = f.getMostRecentFile().determineResource();
            if (resource == null) {
                return null;
            }
            if (f.isDetailedFragmentKnown()) {
                if (!lookupTables.containsKey(resource)) {
                    lookupTables.put(resource, PositionLookupTable.create((IFile) resource));
                }
                final FileFragment pos = f.getMostRecentFragment();
                final IMarker marker = markerFactory.createMarker(resource, Constants.FRAGMENTMARKER_ID);
                marker.setAttribute(IMarker.LINE_NUMBER, pos.getFrom().getLine());
                marker.setAttribute(IMarker.CHAR_START,
                        lookupTables.get(resource).getCharsSinceFileStart(pos.getFrom()) - 1);
                marker.setAttribute(IMarker.CHAR_END,
                        lookupTables.get(resource).getCharsSinceFileStart(pos.getTo()));
                return marker;
            } else {
                return markerFactory.createMarker(resource, Constants.FRAGMENTMARKER_ID);
            }
        } catch (final CoreException | IOException e) {
            throw new ReviewtoolException(e);
        }
    }

    /**
     * Creates a marker for the given fragment.
     * If multiple markers have to be created, use the method that caches lookup tables instead.
     * If a marker could not be created (for example because the resource is not availabel in Eclipse), null
     * is returned.
     */
    public static IMarker createMarkerFor(
            IMarkerFactory markerFactory,
            final SliceFragment f) {
        return createMarkerFor(markerFactory, new HashMap<IResource, PositionLookupTable>(), f);
    }

    public List<Slice> getSlices() {
        return this.slices;
    }

}
