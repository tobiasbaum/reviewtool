package de.setsoftware.reviewtool.model.api;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Class to execute background jobs, without knowing which UI we're in.
 */
public abstract class BackgroundJobExecutor {
    
    private static BackgroundJobExecutor instance;
    
    public static void execute(String name, Function<ICortProgressMonitor, Throwable> job) {
        instance.startJob(name, job);
    }

    public static void execute(String name, Consumer<ICortProgressMonitor> job) {
        instance.startJob(name, (ICortProgressMonitor m) -> {
            job.accept(m);
            return null;
        });
    }

    protected abstract void startJob(String name, Function<ICortProgressMonitor, Throwable> job);

    public static void setInstance(BackgroundJobExecutor inst) {
        instance = inst;
    }
}
