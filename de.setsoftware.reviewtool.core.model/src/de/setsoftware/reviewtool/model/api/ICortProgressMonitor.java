package de.setsoftware.reviewtool.model.api;

/**
 * Progress monitor to receive infos for the UI for long running tasks.
 */
public interface ICortProgressMonitor {

    boolean isCanceled();

    public abstract void beginTask(String name, int totalWork);

    public abstract void subTask(String name);
    
    public abstract void done();
    
}
