package de.setsoftware.reviewtool.ui.dialogs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

import de.setsoftware.reviewtool.base.ReviewtoolException;
import de.setsoftware.reviewtool.model.Constants;
import de.setsoftware.reviewtool.model.PositionTransformer;
import de.setsoftware.reviewtool.model.api.IFragment;
import de.setsoftware.reviewtool.model.api.IRevisionedFile;
import de.setsoftware.reviewtool.model.changestructure.IStopMarker;
import de.setsoftware.reviewtool.model.changestructure.IStopMarkerFactory;
import de.setsoftware.reviewtool.model.changestructure.PositionLookupTable;
import de.setsoftware.reviewtool.model.changestructure.ToursInReview;
import de.setsoftware.reviewtool.model.remarks.IMarkerFactory;
import de.setsoftware.reviewtool.model.remarks.IReviewMarker;
import de.setsoftware.reviewtool.model.remarks.Position;
import de.setsoftware.reviewtool.model.remarks.ReviewRemarkException;

/**
 * A factory to create normal Eclipse markers.
 */
public class RealMarkerFactory implements IStopMarkerFactory, IMarkerFactory {

    private final Map<IResource, PositionLookupTable> lookupTables = new HashMap<>();
    
    @Override
    public IReviewMarker createMarker(Position pos) throws ReviewRemarkException {
        try {
            IResource res = getResourceForPath(PositionTransformer.toPath(pos.getShortFileName()));
            return EclipseMarker.create(res.createMarker(Constants.REVIEWMARKER_ID));
        } catch (final CoreException e) {
            throw new ReviewRemarkException(e);
        }
    }

    @Override
    public IStopMarker createStopMarker(IRevisionedFile file, boolean tourActive, String message) {
        final IResource resource = determineResource(file);
        if (resource == null) {
            return null;
        }
        return createStopMarker(resource, tourActive, message);
    }
    
    @Override
    public IStopMarker createStopMarker(IRevisionedFile file, boolean tourActive, String message, IFragment pos) {
        final IResource resource = determineResource(file);
        if (resource == null) {
            return null;
        }
        EclipseMarker marker = createStopMarker(resource, tourActive, message);
        if (marker == null) {
            return null;
        }
        if (!lookupTables.containsKey(resource)) {
            lookupTables.put(resource, createLookupTable((IFile) resource));
        }
        marker.setAttribute(IMarker.LINE_NUMBER, pos.getFrom().getLine());
        marker.setAttribute(IMarker.CHAR_START,
                lookupTables.get(resource).getCharsSinceFileStart(pos.getFrom()));
        marker.setAttribute(IMarker.CHAR_END,
                lookupTables.get(resource).getCharsSinceFileStart(pos.getTo()));
        return marker;
    }
    
    private EclipseMarker createStopMarker(IResource resource, boolean tourActive, String message) {
        try {
            IMarker marker = resource.createMarker(
                    tourActive ? Constants.STOPMARKER_ID : Constants.INACTIVESTOPMARKER_ID);
            marker.setAttribute(IMarker.MESSAGE, message);
            return EclipseMarker.wrap(marker);
        } catch (final CoreException e) {
            throw new ReviewRemarkException(e);
        }        
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Heuristically drops path prefixes (like "trunk", ...) until a resource can be found.
     */
    private static IResource determineResource(IRevisionedFile f) {
        String partOfPath = f.getPath();
        if (partOfPath.startsWith("/")) {
            partOfPath = partOfPath.substring(1);
        }
        while (true) {
            final IResource resource = getResourceForPath(PositionTransformer.toPath(partOfPath));
            if (!(resource instanceof IWorkspaceRoot)) {
                //perhaps too much was dropped and a different file then the intended returned
                //  therefore double check by using the inverse lookup
                final String shortName = 
                        PositionTransformer.toPosition(resource.getFullPath().toFile(), 1).getShortFileName();
                if (partOfPath.contains(shortName)) {
                    return resource;
                }
            }
            final int slashIndex = partOfPath.indexOf('/');
            if (slashIndex < 0) {
                return null;
            }
            partOfPath = partOfPath.substring(slashIndex + 1);
        }
    }

    private static IResource getResourceForPath(File path) {
        final IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        return path == null ? workspaceRoot : getResourceForPath(workspaceRoot, path);
    }

    private static IResource getResourceForPath(IWorkspaceRoot workspaceRoot, File fittingPath) {
        final IFile file = workspaceRoot.getFileForLocation(new Path(fittingPath.getPath()));
        if (file == null || !file.exists()) {
            return workspaceRoot;
        }
        return file;
    }
    
    /**
     * Clears all current tour stop markers.
     */
    public void clearStopMarkers() {
        try {
            ResourcesPlugin.getWorkspace().getRoot().deleteMarkers(
                    Constants.STOPMARKER_ID, true, IResource.DEPTH_INFINITE);
            ResourcesPlugin.getWorkspace().getRoot().deleteMarkers(
                    Constants.INACTIVESTOPMARKER_ID, true, IResource.DEPTH_INFINITE);
        } catch (CoreException e) {
            throw new ReviewtoolException(e);
        }
    }

    /**
     * Creates a lookup table for the contents from the given file.
     */
    private static PositionLookupTable createLookupTable(IFile file) {
        try {
            if (!file.isSynchronized(IResource.DEPTH_ZERO)) {
                file.refreshLocal(IResource.DEPTH_ZERO, null);
            }
            final InputStream stream = file.getContents();
            try {
                final Reader r = new InputStreamReader(stream, file.getCharset());
                return PositionLookupTable.create(r);
            } finally {
                stream.close();
            }
        } catch (CoreException | IOException e) {
            throw new ReviewtoolException(e);
        }
    }

}
