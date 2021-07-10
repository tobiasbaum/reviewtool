package de.setsoftware.reviewtool.ordering;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

import de.setsoftware.reviewtool.base.Logger;
import de.setsoftware.reviewtool.base.Multimap;
import de.setsoftware.reviewtool.model.api.ICortPath;
import de.setsoftware.reviewtool.model.api.ICortResource;
import de.setsoftware.reviewtool.model.api.IRevisionedFile;
import de.setsoftware.reviewtool.ordering.efficientalgorithm.TourCalculatorControl;

/**
 * Groups stops that belong to the same source folder (and project).
 */
public class InSameSourceFolderRelation implements RelationMatcher {

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
        final ICortResource resource = file.determineResource();
        if (resource == null) {
            return null;
        }
        final String projectName = resource.getProjectName();
        if (projectName == null) {
            return null;
        }
        final ICortPath projectRelativePath = resource.getProjectRelativePath();
        if (projectRelativePath.isEmpty()) {
            return projectName;
        }
        return projectName + "/" + projectRelativePath.segment(0);
    }

}
