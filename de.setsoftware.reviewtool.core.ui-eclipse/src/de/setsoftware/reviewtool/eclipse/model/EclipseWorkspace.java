package de.setsoftware.reviewtool.eclipse.model;

import java.util.List;

import de.setsoftware.reviewtool.model.ICortWorkspace;
import de.setsoftware.reviewtool.model.api.ICortPath;
import de.setsoftware.reviewtool.model.api.ICortResource;

public class EclipseWorkspace implements ICortWorkspace {

    @Override
    public List<? extends ICortPath> getProjectPaths() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ICortResource getRoot() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ICortResource getResourceForPath(ICortPath path) {
        // TODO Auto-generated method stub
        return null;
    }

//  private static ICortResource getResourceForPath(IWorkspaceRoot workspaceRoot, ICortPath fittingPath) {
//  final IFile file = workspaceRoot.getFileForLocation(fittingPath);
//  if (file == null || !file.exists()) {
//      return workspaceRoot;
//  }
//  return file;
//}

}
