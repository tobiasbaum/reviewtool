package de.setsoftware.reviewtool.model;

import java.io.File;
import java.util.List;

import de.setsoftware.reviewtool.model.api.ICortPath;
import de.setsoftware.reviewtool.model.api.ICortResource;

public interface ICortWorkspace {

    public abstract List<? extends ICortPath> getProjectPaths();

    public abstract ICortResource getRoot();

    public abstract ICortResource getResourceForPath(ICortPath path);

    public abstract ICortPath createPath(File absolutePathInWc);

}
