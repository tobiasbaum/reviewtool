package de.setsoftware.reviewtool.model.api;

import java.io.Reader;

import de.setsoftware.reviewtool.model.ICortWorkspace;

public interface ICortResource {

    public abstract String getProjectName();

    public abstract ICortPath getProjectRelativePath();

    public abstract ICortWorkspace getWorkspace();

    public abstract ICortPath getFullPath();

    public abstract boolean isWorkspaceRoot();

    public abstract Reader open();

}
