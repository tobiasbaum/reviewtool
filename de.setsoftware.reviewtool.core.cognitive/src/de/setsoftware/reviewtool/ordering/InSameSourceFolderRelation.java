package de.setsoftware.reviewtool.ordering;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

import de.setsoftware.reviewtool.base.Logger;
import de.setsoftware.reviewtool.base.Multimap;
import de.setsoftware.reviewtool.model.api.IRevisionedFile;
import de.setsoftware.reviewtool.ordering.efficientalgorithm.TourCalculatorControl;

/**
 * Groups stops that belong to the same source folder (and project).
 * This matcher relies on naming conventions and is therefore probably not well portable to other contexts.
 */
public class InSameSourceFolderRelation implements RelationMatcher {
    
    private static final String[] SOURCE_FOLDER_NAMES = new String[] {"src", "test", "resources", "testresources"};

    private final HierarchyExplicitness explicitness;

    public InSameSourceFolderRelation(HierarchyExplicitness explicitness) {
        this.explicitness = explicitness;
    }

    @Override
    public Collection<? extends OrderingInfo> determineMatches(
            List<ChangePart> changeParts, TourCalculatorControl control) {
        final Multimap<String, ChangePart> grouping = new Multimap<>();
        for (final ChangePart c : changeParts) {
            Logger.verboseDebug(
                    () -> "change part with " + c.getStops().size() + " stops, e.g. " + c.getStops().get(0));
            final IRevisionedFile mostRecentFile = c.getStops().get(0).getMostRecentFile();
            final String sourceFolder = this.determineSourceFolder(mostRecentFile);
            Logger.verboseDebug(() -> "source folder for " + mostRecentFile + " is " + sourceFolder);
            if (sourceFolder != null) {
                grouping.put(sourceFolder, c);
            }
        }

        final List<OrderingInfo> ret = new ArrayList<>();
        for (final Entry<String, List<ChangePart>> e : grouping.entrySet()) {
            ret.add(OrderingInfoImpl.unordered(this.explicitness, e.getKey(), e.getValue()));
        }
        return ret;
    }

    private String determineSourceFolder(IRevisionedFile file) {
        String path = file.getPath().replace('\\', '/');
        for (String sf : SOURCE_FOLDER_NAMES) {
            int index = sf.indexOf("/" + sf + "/");
            if (index >= 0) {
                return path.substring(0, index + sf.length() + 2);
            }
        }
        return null;
    }

}
