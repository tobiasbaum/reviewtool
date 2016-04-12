package de.setsoftware.reviewtool.model.changestructure;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;

import de.setsoftware.reviewtool.base.ReviewtoolException;
import de.setsoftware.reviewtool.model.Constants;
import de.setsoftware.reviewtool.model.IMarkerFactory;
import de.setsoftware.reviewtool.model.PositionTransformer;

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

    /**
     * Creates markers for the fragments. Takes the currently active slice into account.
     */
    public void createMarkers(IMarkerFactory markerFactory) {
        if (this.slices.size() <= this.currentSliceIndex) {
            return;
        }
        final Slice s = this.slices.get(this.currentSliceIndex);
        final Map<IResource, PositionLookupTable> lookupTables = new HashMap<>();
        for (final Fragment f : s.getFragments()) {
            createMarkerFor(markerFactory, lookupTables, f);
        }
    }

    private static IMarker createMarkerFor(
            IMarkerFactory markerFactory,
            final Map<IResource, PositionLookupTable> lookupTables,
            final Fragment f) {

        try {
            final IResource resource = determineResource(f.getFile());
            if (resource == null) {
                return null;
            }
            if (!lookupTables.containsKey(resource)) {
                lookupTables.put(resource, PositionLookupTable.create((IFile) resource));
            }
            final IMarker marker = markerFactory.createMarker(resource, Constants.FRAGMENTMARKER_ID);
            marker.setAttribute(IMarker.LINE_NUMBER, f.getFrom().getLine());
            marker.setAttribute(IMarker.CHAR_START,
                    lookupTables.get(resource).getCharsSinceFileStart(f.getFrom()) - 1);
            marker.setAttribute(IMarker.CHAR_END,
                    lookupTables.get(resource).getCharsSinceFileStart(f.getTo()));
            return marker;
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
            final Fragment f) {
        return createMarkerFor(markerFactory, new HashMap<IResource, PositionLookupTable>(), f);
    }

    /**
     * Finds a resource corresponding to a path that is relative to the SCM repository root.
     * Heuristically drops path prefixes (like "trunk", ...) until a resource can be found.
     * If none can be found, null is returned.
     */
    private static IResource determineResource(FileInRevision file) {
        String path = file.getPath();
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        while (true) {
            final IResource resource = PositionTransformer.toResource(path);
            if (!(resource instanceof IWorkspaceRoot)) {
                return resource;
            }
            final int slashIndex = path.indexOf('/');
            if (slashIndex < 0) {
                return null;
            }
            path = path.substring(slashIndex + 1);
        }
    }

    public List<Slice> getSlices() {
        return this.slices;
    }

}
