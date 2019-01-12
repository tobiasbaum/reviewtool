package de.setsoftware.reviewtool.ordering;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;

import de.setsoftware.reviewtool.base.Multimap;
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
            final String sourceFolder = this.determineSourceFolder(c.getStops().get(0).getMostRecentFile());
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
        final IResource resource = file.determineResource();
        if (resource == null) {
            return null;
        }
        final IProject project = resource.getProject();
        if (project == null) {
            return null;
        }
        final IPath projectRelativePath = resource.getProjectRelativePath();
        if (projectRelativePath.isEmpty()) {
            return project.getName();
        }
        return project.getName() + "/" + projectRelativePath.segment(0);
    }

}
