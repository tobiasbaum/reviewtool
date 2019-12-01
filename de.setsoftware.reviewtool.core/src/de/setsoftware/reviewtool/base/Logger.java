package de.setsoftware.reviewtool.base;

import java.util.function.Supplier;

import org.eclipse.core.runtime.IStatus;

/**
 * Wrapper for the Eclipse logging mechanism.
 */
public abstract class Logger {

    private static Logger instance;
    private static volatile boolean verbose;

    public static void error(String message, Throwable exception) {
        instance.log(IStatus.ERROR, message, exception);
    }

    public static void warn(String message, Throwable exception) {
        instance.log(IStatus.WARNING, message, exception);
    }

    public static void info(String message) {
        instance.log(IStatus.INFO, message);
    }

    public static void debug(String message) {
        instance.log(IStatus.OK, message);
    }

    public static void verboseDebug(Supplier<String> message) {
        if (verbose) {
            debug(message.get());
        }
    }

    protected abstract void log(int status, String message);

    protected abstract void log(int status, String message, Throwable exception);

    public static void setLogger(Logger logger) {
        instance = logger;
    }

    public static void setVerbose() {
        verbose = true;
    }

}
