package de.setsoftware.reviewtool.plugin;

import java.util.function.Function;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobFunction;
import org.eclipse.core.runtime.jobs.Job;

import de.setsoftware.reviewtool.model.api.BackgroundJobExecutor;
import de.setsoftware.reviewtool.model.api.ICortProgressMonitor;

public class EclipseBackgroundJobExecutor extends BackgroundJobExecutor {
    
    private static final class ProgressAdapter implements ICortProgressMonitor {
        
        private IProgressMonitor target;

        public ProgressAdapter(IProgressMonitor monitor) {
            this.target = monitor;
        }

        @Override
        public boolean isCanceled() {
            return this.target.isCanceled();
        }

        @Override
        public void beginTask(String name, int totalWork) {
            this.target.beginTask(name, totalWork);
        }

        @Override
        public void subTask(String name) {
            this.target.subTask(name);
        }

        @Override
        public void done() {
            this.target.done();
        }
        
    }

    @Override
    protected void startJob(String name, Function<ICortProgressMonitor, Throwable> job) {
        final Job eclipseJob = Job.create(name, new IJobFunction() {
            @Override
            public IStatus run(IProgressMonitor monitor) {
                Throwable ex = null;
                try {
                    ex = job.apply(new ProgressAdapter(monitor));
                } catch (final Throwable e) {
                    ex = e;
                }
                if (ex == null) {
                    return Status.OK_STATUS;
                } else if (ex instanceof InterruptedException) {
                    return Status.CANCEL_STATUS;
                } else {
                    return new Status(
                            Status.ERROR, "de.set.reviewtool.core.ui-eclipse", 99, ex.getLocalizedMessage(), ex);
                }
            }

        });
        eclipseJob.schedule();
    }

}
