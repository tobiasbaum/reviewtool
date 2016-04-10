package de.setsoftware.reviewtool.base;

import org.eclipse.core.runtime.IStatus;

/**
 * Wrapper for the Eclipse logging mechanism.
 */
public abstract class Logger {

    private static Logger instance;

    public static void info(String message) {
        instance.log(IStatus.INFO, message);
    }

    protected abstract void log(int status, String message);

    public static void setLogger(Logger logger) {
        instance = logger;
    }

}
