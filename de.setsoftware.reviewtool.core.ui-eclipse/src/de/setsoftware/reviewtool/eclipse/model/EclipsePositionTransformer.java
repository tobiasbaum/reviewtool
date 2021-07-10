package de.setsoftware.reviewtool.eclipse.model;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobFunction;
import org.eclipse.core.runtime.jobs.Job;

import de.setsoftware.reviewtool.model.ICortWorkspace;
import de.setsoftware.reviewtool.model.PositionTransformer;

public class EclipsePositionTransformer {
    
    static {
        init();
    }
    
    private static void init() {
        PositionTransformer.init(
                () -> getWorkspace(),
                (ICortWorkspace root) -> refreshCacheInBackground(root));
    }
    
    private static ICortWorkspace getWorkspace() {
        final IWorkspace root = ResourcesPlugin.getWorkspace();        
    }

    /**
     * Starts a new job that initializes the cache in the background.
     * If the cache has already been initialized, it does nothing.
     */
    public static void initializeCacheInBackground() {
        final Job job = Job.create("Review resource cache init", new IJobFunction() {
            @Override
            public IStatus run(IProgressMonitor monitor) {
                try {
                    PositionTransformer.fillCacheIfEmpty(getWorkspace(), monitor::isCanceled);
                    return Status.OK_STATUS;
                } catch (final InterruptedException e) {
                    return Status.CANCEL_STATUS;
                }
            }

        });
        job.schedule();
    }

    /**
     * Starts a new job that refreshes the cache in the background.
     */
    public static void refreshCacheInBackground(final ICortWorkspace root) {
        final Job job = Job.create("Review resource cache refresh", new IJobFunction() {
            @Override
            public IStatus run(IProgressMonitor monitor) {
                try {
                    PositionTransformer.fillCache(root, monitor::isCanceled);
                    return Status.OK_STATUS;
                } catch (final InterruptedException e) {
                    return Status.CANCEL_STATUS;
                }
            }

        });
        job.schedule();
    }

}
