package de.setsoftware.reviewtool.model.api;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Class to execute background jobs, without knowing which UI we're in.
 */
public abstract class BackgroundJobExecutor {
    
    private static BackgroundJobExecutor instance;
    
    public static void setInstance(BackgroundJobExecutor inst) {
        instance = inst;
    }

    public static void execute(String name, Function<ICortProgressMonitor, Throwable> job) {
        executeWithMutex(name, null, job);
    }

    public static void execute(String name, Consumer<ICortProgressMonitor> job) {
        executeWithMutex(name, null, job);
    }
    
    public static void executeWithMutex(
            String name, Object mutexResource, Function<ICortProgressMonitor, Throwable> job) {
        executeWithMutex(name, mutexResource, job, -1);
    }
    
    public static void executeWithMutex(
            String name, Object mutexResource, Consumer<ICortProgressMonitor> job, long processingDelay) {
        executeWithMutex(name, mutexResource, wrap(job), processingDelay);
    }

    public static void executeWithMutex(
            String name, Object mutexResource, Function<ICortProgressMonitor, Throwable> job, long processingDelay) {
        instance.startJob(name, mutexResource, job, processingDelay);
    }

    public static void executeWithMutex(String name, Object mutexResource, Consumer<ICortProgressMonitor> job) {
        executeWithMutex(name, mutexResource, wrap(job));
    }

    private static Function<ICortProgressMonitor, Throwable> wrap(Consumer<ICortProgressMonitor> job) {
        return (ICortProgressMonitor m) -> {
            job.accept(m);
            return null;
        };
    }

    protected abstract void startJob(
            String name, Object mutexResource, Function<ICortProgressMonitor, Throwable> job, long processingDelay);

    public static RuntimeException createOperationCanceledException() {
        if (instance == null) {
            return new RuntimeException("operation cancelled on uninitialized BackgroundJobExecutor");
        }
        return instance.doCreateOperationCanceledException();
    }
    
    protected abstract RuntimeException doCreateOperationCanceledException();

}
