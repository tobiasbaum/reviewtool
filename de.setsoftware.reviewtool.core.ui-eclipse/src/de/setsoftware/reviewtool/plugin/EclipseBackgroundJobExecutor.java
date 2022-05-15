package de.setsoftware.reviewtool.plugin;

import java.util.function.Function;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobFunction;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
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
    
    private static final class MutexRule implements ISchedulingRule {
        
        private Object mutexResource;
        
        public MutexRule(Object mutexResource) {
            this.mutexResource = mutexResource;
        }

        @Override
        public boolean contains(ISchedulingRule arg0) {
            return this.equals(arg0);
        }

        @Override
        public boolean isConflicting(ISchedulingRule arg0) {
            return this.equals(arg0);
        }
        
        public int hashCode() {
            return this.mutexResource.hashCode();
        }
        
        public boolean equals(Object o) {
            if (!(o instanceof MutexRule)) {
                return false;
            }
            MutexRule other = (MutexRule) o;
            return this.mutexResource.equals(other.mutexResource);
        }
        
    }

    @Override
    protected void startJob(
            String name,
            Object mutexResource, 
            Function<ICortProgressMonitor, Throwable> job, 
            long processingDelay) {
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
        if (mutexResource != null) {
            eclipseJob.setRule(new MutexRule(mutexResource));
        }
        if (processingDelay > 0) {
            eclipseJob.schedule(processingDelay);
        } else {
            eclipseJob.schedule();
        }
    }

    @Override
    protected RuntimeException doCreateOperationCanceledException() {
        return new OperationCanceledException();
    }

}
